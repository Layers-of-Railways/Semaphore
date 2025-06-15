/*
 * Semaphore
 * Copyright (c) 2025 Sam Wagenaar
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.slimeistdev.semaphore.content.signal_debug;

import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.signal.SignalBoundary;
import com.simibubi.create.content.trains.signal.SignalEdgeGroup;
import com.simibubi.create.foundation.utility.Pair;
import io.github.slimeistdev.semaphore.mixin.common.AccessorNavigation;
import io.github.slimeistdev.semaphore.network.SemaphorePackets;
import io.github.slimeistdev.semaphore.network.packets.s2c.TrainDebugLabelsPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.BiConsumer;

public class ServerSignalDebugManager {
    private final ServerPlayer player;
    private final UUID trainId;
    private final Map<UUID, Node> knownNodes = new HashMap<>();

    private ServerSignalDebugManager(ServerPlayer player, UUID trainId) {
        this.player = player;
        this.trainId = trainId;
    }

    public static void runDebug(UUID trainId, ServerPlayer player) {
        new ServerSignalDebugManager(player, trainId).run();
    }

    private void msg(MutableComponent message) {
        player.sendSystemMessage(message);
    }

    private void err(MutableComponent message) {
        msg(message.withStyle(ChatFormatting.DARK_RED));
    }

    private static Component copyableString(String str) {
        return Component.literal(str)
            .withStyle(Style.EMPTY
                .withHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    Component.translatable("semaphore.tooltip.copyable_string")
                ))
                .withClickEvent(new ClickEvent(
                    ClickEvent.Action.COPY_TO_CLIPBOARD,
                    str
                ))
            );
    }

    private void run() {
        msg(Component.translatable("semaphore.signal_debug.title", copyableString(trainId.toString())).withStyle(ChatFormatting.GOLD));

        Train currentTrain = Create.RAILWAYS.trains.get(trainId);
        if (currentTrain == null) {
            err(Component.translatable("semaphore.signal_debug.no_train"));
            return;
        }

        if (currentTrain.navigation.waitingForSignal == null) {
            err(Component.translatable("semaphore.signal_debug.not_waiting"));
            return;
        }

        Node root = new Node(currentTrain);
        Stack<Node> stack = new Stack<>();
         stack.push(root);

        while (!stack.isEmpty()) {
            stack.pop().resolve(stack);
        }

        ReportTagGenerator tags = new ReportTagGenerator();

        root.visit((node, pair) -> {
            BlockerType type = pair.getFirst();
            Blocker blocker = pair.getSecond();

            var tag = tags.getTag(node.train);
            var color = type == BlockerType.IMMEDIATE
                ? ChatFormatting.WHITE
                : ChatFormatting.GRAY;

            if (blocker instanceof Blocker.ForcedRedSignal) {
                msg(Component.translatable("semaphore.signal_debug.blocker.forced_red", tag).withStyle(color));
            } else if (blocker instanceof Blocker.SimpleNode simpleNode) {
                var blockerTag = tags.getTag(simpleNode.node.train);
                msg(Component.translatable("semaphore.signal_debug.blocker.simple", tag, blockerTag).withStyle(color));
            } else if (blocker instanceof Blocker.DeadlockedParent deadlockedParent) {
                Node parent = deadlockedParent.parent.get();
                if (parent != null) {
                    var parentTag = tags.getTag(parent.train);
                    msg(Component.translatable("semaphore.signal_debug.blocker.deadlocked_parent", tag, parentTag).withStyle(color));
                }
            }
        });

        SemaphorePackets.PACKETS.sendTo(player, new TrainDebugLabelsPacket(tags.tagCache));
    }

    private sealed interface Blocker permits Blocker.ForcedRedSignal, Blocker.SimpleNode, Blocker.DeadlockedParent {
        enum ForcedRedSignal implements Blocker {
            INSTANCE
        }
        record SimpleNode(Node node) implements Blocker {}
        record DeadlockedParent(WeakReference<Node> parent) implements Blocker {}
    }

    private enum BlockerType {
        IMMEDIATE,
        CHAINED
    }

    private final class Node {
        final @NotNull Train train;
        private boolean resolved = false;

        private @NotNull BlockerType blockerType = BlockerType.IMMEDIATE;
        private @Nullable Blocker blocker;

        private final @NotNull List<WeakReference<Node>> parents = new ArrayList<>();
        private final @NotNull Set<UUID> deadlockSources = new HashSet<>();

        private int dependents = 0;

        public Node(@NotNull Train train) {
            this.train = train;
            knownNodes.put(train.id, this);
        }

        public void visit(BiConsumer<Node, Pair<BlockerType, Blocker>> visitor) {
            if (blocker != null) {
                visitor.accept(this, Pair.of(blockerType, blocker));
            }

            if (blocker instanceof Blocker.SimpleNode simpleNode) {
                simpleNode.node.visit(visitor);
            }
        }

        private void addParent(Node parent) {
            parents.add(new WeakReference<>(parent));
            dependents++;
            deadlockSources.add(parent.train.id);
            deadlockSources.addAll(parent.deadlockSources);
        }

        public void resolve(Stack<Node> stack) {
            if (resolved) return;
            resolved = true;

            if (train.navigation.waitingForSignal == null) return;
            // handle immediate signal
            {
                UUID signalId = train.navigation.waitingForSignal.getFirst();
                boolean primary = train.navigation.waitingForSignal.getSecond();

                SignalBoundary signal = train.graph.getPoint(EdgePointType.SIGNAL, signalId);
                if (resolveSignal(signal, primary, stack)) {
                    return;
                }
            }

            // handle chained signals
            blockerType = BlockerType.CHAINED;
            Map<UUID, Pair<SignalBoundary, Boolean>> chainedGroups = ((AccessorNavigation) train.navigation).semaphore$getWaitingForChainedGroups();
            for (Pair<SignalBoundary, Boolean> chainedGroup : chainedGroups.values()) {
                if (resolveSignal(chainedGroup.getFirst(), chainedGroup.getSecond(), stack)) {
                    return;
                }
            }
        }

        /**
         * @return whether to stop searching for blockers
         */
        private boolean resolveSignal(SignalBoundary signal, boolean primary, Stack<Node> stack) {
            if (signal.isForcedRed(primary)) {
                this.blocker = Blocker.ForcedRedSignal.INSTANCE;
                return true;
            } else {
                UUID groupId = signal.groups.get(primary);
                if (groupId == null) return false;
                SignalEdgeGroup group = Create.RAILWAYS.signalEdgeGroups.get(groupId);
                if (group == null) return false;

                for (Train occupier : group.trains) {
                    if (occupier == train) continue;

                    if (deadlockSources.contains(occupier.id)) {
                        // we found an (indirect) parent
                        Node parent = knownNodes.get(occupier.id);
                        parent.dependents += 1000; // weight deadlocks more heavily
                        blocker = new Blocker.DeadlockedParent(new WeakReference<>(parent));
                        return true;
                    }

                    Node child;
                    if (knownNodes.containsKey(occupier.id)) {
                        child = knownNodes.get(occupier.id);
                    } else {
                        child = new Node(occupier);
                        stack.push(child);
                    }
                    blocker = new Blocker.SimpleNode(child);
                    child.addParent(this);
                    return true;
                }
            }

            return false;
        }
    }

    private static class ReportTagGenerator {
        private final Map<UUID, String> tagCache = new HashMap<>();

        public String getTag(UUID id) {
            return tagCache.computeIfAbsent(id, $ -> ServerSignalDebugManager.generateSpreadsheetTag(tagCache.size()));
        }

        public MutableComponent getTag(Train train) {
            String tag = getTag(train.id);
            return Component.literal("[" + tag + "]")
                .withStyle(Style.EMPTY
                    .withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        train.name
                    ))
                    .withClickEvent(new ClickEvent(
                        ClickEvent.Action.SUGGEST_COMMAND,
                        "/semaphore train_tp @s " + train.id.toString()
                    ))
                );
        }
    }

    private static String generateSpreadsheetTag(int index) {
        StringBuilder tag = new StringBuilder();
        int currentIndex = index;

        do {
            int remainder = currentIndex % 26;
            tag.append((char) ('A' + remainder));
            currentIndex = (currentIndex / 26) - 1;
        } while (currentIndex >= 0);

        return tag.reverse().toString();
    }
}
