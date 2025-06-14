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

package io.github.slimeistdev.semaphore.events;

import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Train;
import io.github.slimeistdev.semaphore.Semaphore;
import io.github.slimeistdev.semaphore.network.SemaphorePackets;
import io.github.slimeistdev.semaphore.network.SemaphorePlayerSelection;
import io.github.slimeistdev.semaphore.network.packets.s2c.CheckVersionPacket;
import io.github.slimeistdev.semaphore.network.packets.s2c.SignalOccupationUpdatePacket;
import io.github.slimeistdev.semaphore.registry.SemaphoreCommandsServer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

public class CommonEvents {
    public static void register() {
        CommandRegistrationCallback.EVENT.register(SemaphoreCommandsServer::register);
        ServerPlayConnectionEvents.JOIN.register(CommonEvents::onPlayerJoin);
        ServerPlayConnectionEvents.DISCONNECT.register(CommonEvents::onPlayerDisconnect);
        ServerTickEvents.END_SERVER_TICK.register(CommonEvents::onServerTick);
    }

    private static void onPlayerJoin(ServerGamePacketListenerImpl handler, PacketSender sender, MinecraftServer server) {
        SemaphorePackets.PACKETS.sendTo(handler.player, new CheckVersionPacket(SemaphorePackets.PACKETS.version));
    }

    private static void onPlayerDisconnect(ServerGamePacketListenerImpl handler, MinecraftServer server) {
        Semaphore.playersDebuggingSignals.remove(handler.player.getUUID());
    }

    private static void onServerTick(MinecraftServer server) {
        long gameTime = server.overworld().getGameTime();

        if (!Semaphore.playersDebuggingSignals.isEmpty()) {
            for (Train train : Create.RAILWAYS.trains.values()) {
                if (!train.shouldCarriageSyncThisTick(gameTime, 5)) continue;

                SemaphorePackets.PACKETS.sendTo(
                    SemaphorePlayerSelection.tracking(train).filtered(
                        player -> Semaphore.playersDebuggingSignals.contains(player.getUUID())
                    ),
                    new SignalOccupationUpdatePacket(train)
                );
            }
        }
    }
}
