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
import io.github.slimeistdev.semaphore.content.train_debug.TrainDebugManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Map;
import java.util.UUID;

public record TrainDebugLabelsPacket(Map<UUID, String> labels) implements S2CPacket {
    public TrainDebugLabelsPacket(FriendlyByteBuf buf) {
        this(buf.readMap(FriendlyByteBuf::readUUID, FriendlyByteBuf::readUtf));
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeMap(labels, FriendlyByteBuf::writeUUID, FriendlyByteBuf::writeUtf);
    }

    @Override
    public void handle(Minecraft mc) {
        TrainDebugManager.setDebugLabels(labels);
    }
}
