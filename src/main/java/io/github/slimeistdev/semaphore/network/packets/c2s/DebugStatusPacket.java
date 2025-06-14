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

package io.github.slimeistdev.semaphore.network.packets.c2s;

import com.railwayteam.railways.multiloader.C2SPacket;
import io.github.slimeistdev.semaphore.Semaphore;
import io.github.slimeistdev.semaphore.network.SemaphorePackets;
import io.github.slimeistdev.semaphore.network.packets.s2c.RejectDebugStatusPacket;
import io.github.slimeistdev.semaphore.utils.AuthUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public record DebugStatusPacket(boolean debuggingSignals) implements C2SPacket {
    public DebugStatusPacket(FriendlyByteBuf buf) {
        this(buf.readBoolean());
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeBoolean(debuggingSignals);
    }

    @Override
    public void handle(ServerPlayer sender) {
        if (AuthUtils.isAuthorized(sender)) {
            if (debuggingSignals) {
                Semaphore.playersDebuggingSignals.add(sender.getUUID());
            } else {
                Semaphore.playersDebuggingSignals.remove(sender.getUUID());
            }
        } else {
            Semaphore.playersDebuggingSignals.remove(sender.getUUID());

            SemaphorePackets.PACKETS.sendTo(sender, new RejectDebugStatusPacket());
        }
    }
}
