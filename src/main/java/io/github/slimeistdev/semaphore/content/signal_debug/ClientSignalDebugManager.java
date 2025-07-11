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

import com.simibubi.create.CreateClient;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.signal.SignalEdgeGroup;
import com.simibubi.create.foundation.outliner.Outliner;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.Iterate;
import io.github.slimeistdev.semaphore.content.commands.client.ToggleDebugCommand;
import io.github.slimeistdev.semaphore.content.train_debug.TrainDebugManager;
import io.github.slimeistdev.semaphore.mixin_ducks.client.TrainDuck;
import io.github.slimeistdev.semaphore.utils.OutlineKeyCache;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

@Environment(EnvType.CLIENT)
public class ClientSignalDebugManager {
    private static final Map<UUID, List<Color>> occupiedSignalBlocks = new HashMap<>();
    private static final Map<UUID, Color> reservedSignalBlocks = new HashMap<>();
    private static final Map<UUID, List<Color>> awaitingSignalBlocks = new HashMap<>();

    private static final OutlineKeyCache.Sequential keys = new OutlineKeyCache.Sequential();
    private static final OutlineKeyCache.Sequential highlightKeys = new OutlineKeyCache.Sequential();

    private static final ItemStack occupiedStack = new ItemStack(Items.RED_WOOL);
    private static final ItemStack reservedStack = new ItemStack(Items.YELLOW_WOOL);
    private static final ItemStack clearStack = new ItemStack(Items.LIME_WOOL);

    public static void prepareDebug() {
        if (!ToggleDebugCommand.signalBlocksEnabled) return;

        keys.resetCounter();
        highlightKeys.resetCounter();
    }

    public static void tick() {
        if (!ToggleDebugCommand.signalBlocksEnabled) return;

        occupiedSignalBlocks.clear();
        reservedSignalBlocks.clear();
        awaitingSignalBlocks.clear();

        for (Train train : CreateClient.RAILWAYS.trains.values()) {
            Color color = TrainDebugManager.getTrainColor(train);
            TrainDebugManager debugManager = ((TrainDuck) train).semaphore$getDebugManager();

            for (UUID occupiedBlock : debugManager.getOccupiedSignalBlocks()) {
                occupiedSignalBlocks.computeIfAbsent(occupiedBlock, k -> new ArrayList<>()).add(color);
            }

            for (UUID reservedBlock : debugManager.getReservedSignalBlocks()) {
                reservedSignalBlocks.put(reservedBlock, color);
            }

            for (UUID awaitingBlock : debugManager.getAwaitingSignalBlocks()) {
                awaitingSignalBlocks.computeIfAbsent(awaitingBlock, k -> new ArrayList<>()).add(color);
            }
        }
    }

    public static void debugEdge(Outliner outliner, SignalEdgeGroup group, Vec3 start, Vec3 end) {
        if (!ToggleDebugCommand.signalBlocksEnabled) return;

        ItemStack stack;
        List<Color> occupiedColors = occupiedSignalBlocks.get(group.id);
        Color reservedColor;
        List<Color> awaitingColors = awaitingSignalBlocks.get(group.id);
        if (occupiedColors != null && !occupiedColors.isEmpty()) {
            stack = occupiedStack;
            reservedColor = null;
        } else if ((reservedColor = reservedSignalBlocks.get(group.id)) != null) {
            stack = reservedStack;
        } else {
            stack = clearStack;
        }

        int offset = ToggleDebugCommand.signalBlocksSecondaryOffset;
        for (boolean secondary : Iterate.falseAndTrue) {
            if (secondary && offset <= 0) continue;

            Vec3 pos = start.add(end).scale(0.5);

            if (secondary) {
                pos = pos.add(0, offset, 0);
            }

            outliner.showItem(keys.getNextKey(), pos, stack);
            if (reservedColor != null) {
                outliner.showAABB(highlightKeys.getNextKey(), AABB.ofSize(pos, 0.5, 0.5, 0.5))
                    .colored(reservedColor)
                    .lineWidth(1 / 16f);
            }
            renderRingStack(pos, awaitingColors, 0.6);
            renderRingStack(pos.add(0, 0.25, 0), occupiedColors, 0.3);
        }
    }

    private static void renderRingStack(Vec3 pos, List<Color> colors, double radius) {
        if (colors == null || colors.isEmpty()) return;

//        pos = pos.add(0, 0.25, 0);

        Outliner outliner = CreateClient.OUTLINER;
        for (Color color : colors) {
            outliner.showAABB(highlightKeys.getNextKey(), AABB.ofSize(pos, radius, 0.0, radius), 3)
                .colored(color)
                .lineWidth(1 / 16f);

            pos = pos.add(0, 0.125, 0);
        }
    }
}
