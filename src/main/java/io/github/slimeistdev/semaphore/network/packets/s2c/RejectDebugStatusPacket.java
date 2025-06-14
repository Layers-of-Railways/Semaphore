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

package io.github.slimeistdev.semaphore.network.packets.s2c;

import com.railwayteam.railways.multiloader.S2CPacket;
import io.github.slimeistdev.semaphore.content.commands.client.ToggleDebugCommand;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

public record RejectDebugStatusPacket() implements S2CPacket {
    public RejectDebugStatusPacket(FriendlyByteBuf buf) {
        this();
    }

    @Override
    public void write(FriendlyByteBuf buffer) {}

    @Environment(EnvType.CLIENT)
    @Override
    public void handle(Minecraft mc) {
        ToggleDebugCommand.trainEnabled = false;
        ToggleDebugCommand.signalBlocksEnabled = false;
        mc.getChatListener().handleSystemMessage(Component.literal("You tried to enable debugging, but you are not authorized to do so. You will not receive any debug information."), false);
    }
}
