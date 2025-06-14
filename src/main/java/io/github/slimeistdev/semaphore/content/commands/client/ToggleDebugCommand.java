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

package io.github.slimeistdev.semaphore.content.commands.client;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import io.github.slimeistdev.semaphore.content.train_debug.TrainDebugManager;
import io.github.slimeistdev.semaphore.network.SemaphorePackets;
import io.github.slimeistdev.semaphore.network.packets.c2s.DebugStatusPacket;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.Map;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class ToggleDebugCommand {
    public static boolean trainEnabled = false;
    public static boolean signalBlocksEnabled = false;
    public static int signalBlocksSecondaryOffset = 0;

    public static ArgumentBuilder<FabricClientCommandSource, ?> register() {
        return literal("toggle_debug")
            .then($train())
            .then($signalBlocks());
    }

    private static ArgumentBuilder<FabricClientCommandSource, ?> $train() {
        return literal("train")
            .executes(ctx -> {
                trainEnabled = !trainEnabled;
                if (!trainEnabled) TrainDebugManager.setDebugLabels(Map.of());
                ctx.getSource().sendFeedback(Component.translatable("semaphore.command.toggle_debug.train." + (trainEnabled ? "enabled" : "disabled")));
                return 1;
            });
    }

    private static ArgumentBuilder<FabricClientCommandSource, ?> $signalBlocks() {
        return literal("signal_blocks")
            .executes(ctx -> {
                signalBlocksEnabled = !signalBlocksEnabled;
                SemaphorePackets.PACKETS.send(new DebugStatusPacket(signalBlocksEnabled));
                ctx.getSource().sendFeedback(Component.translatable("semaphore.command.toggle_debug.signal_blocks." + (signalBlocksEnabled ? "enabled" : "disabled")));
                return 1;
            })
            .then(argument("secondary_offset", IntegerArgumentType.integer())
                .executes(ctx -> {
                    signalBlocksEnabled = true;
                    signalBlocksSecondaryOffset = IntegerArgumentType.getInteger(ctx, "secondary_offset");
                    SemaphorePackets.PACKETS.send(new DebugStatusPacket(signalBlocksEnabled));
                    ctx.getSource().sendFeedback(Component.translatable("semaphore.command.toggle_debug.signal_blocks.secondary_offset", signalBlocksSecondaryOffset));
                    return 1;
                }));
    }

    public static void disableAll() {
        trainEnabled = false;
        signalBlocksEnabled = false;
        TrainDebugManager.setDebugLabels(Map.of());
    }
}
