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
import com.simibubi.create.foundation.utility.Components;
import io.github.slimeistdev.semaphore.network.SemaphorePackets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

public record CheckVersionPacket(int serverVersion) implements S2CPacket {
	public CheckVersionPacket(FriendlyByteBuf buf) {
		this(buf.readVarInt());
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeVarInt(serverVersion);
	}

	@Environment(EnvType.CLIENT)
	@Override
	public void handle(Minecraft mc) {
		if (SemaphorePackets.PACKETS.version == serverVersion)
			return;
		Component error = Components.literal("Semaphore on the client uses a different network format than the server.")
				.append(" You should use the same version of the mod on both sides.");
		mc.getConnection().onDisconnect(error);
	}
}