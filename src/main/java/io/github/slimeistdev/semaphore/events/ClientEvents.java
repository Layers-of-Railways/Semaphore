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

import io.github.slimeistdev.semaphore.content.commands.client.ToggleDebugCommand;
import io.github.slimeistdev.semaphore.content.signal_debug.ClientSignalDebugManager;
import io.github.slimeistdev.semaphore.content.train_debug.TrainDebugManager;
import io.github.slimeistdev.semaphore.registry.SemaphoreCommandsClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;

@Environment(EnvType.CLIENT)
public class ClientEvents {
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register(SemaphoreCommandsClient::register);
        ClientTickEvents.START_CLIENT_TICK.register(ClientEvents::onClientTick);
        ClientPlayConnectionEvents.DISCONNECT.register(ClientEvents::onClientDisconnect);
        ClientPreAttackCallback.EVENT.register(ClientEvents::preAttack);
    }

    private static boolean preAttack(Minecraft mc, LocalPlayer player, int clickCount) {
        if (clickCount != 0) {
            return TrainDebugManager.clicked();
        }
        return false;
    }

    private static void onClientTick(Minecraft mc) {
        ClientSignalDebugManager.tick();
    }

    private static void onClientDisconnect(ClientPacketListener connection, Minecraft mc) {
        ToggleDebugCommand.disableAll();
    }
}
