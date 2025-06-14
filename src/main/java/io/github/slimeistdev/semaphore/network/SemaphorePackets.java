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

package io.github.slimeistdev.semaphore.network;

import com.railwayteam.railways.multiloader.PacketSet;
import io.github.slimeistdev.semaphore.network.packets.c2s.DebugStatusPacket;
import io.github.slimeistdev.semaphore.network.packets.c2s.RequestSignalDebugPacket;
import io.github.slimeistdev.semaphore.network.packets.s2c.CheckVersionPacket;
import io.github.slimeistdev.semaphore.network.packets.s2c.RejectDebugStatusPacket;
import io.github.slimeistdev.semaphore.network.packets.s2c.SignalOccupationUpdatePacket;
import io.github.slimeistdev.semaphore.network.packets.s2c.TrainDebugLabelsPacket;

public class SemaphorePackets {
    public static final PacketSet PACKETS = PacketSet.builder("semaphore", 1)
        .s2c(CheckVersionPacket.class, CheckVersionPacket::new)
        .s2c(RejectDebugStatusPacket.class, RejectDebugStatusPacket::new)
        .s2c(SignalOccupationUpdatePacket.class, SignalOccupationUpdatePacket::new)
        .s2c(TrainDebugLabelsPacket.class, TrainDebugLabelsPacket::new)

        .c2s(DebugStatusPacket.class, DebugStatusPacket::new)
        .c2s(RequestSignalDebugPacket.class, RequestSignalDebugPacket::new)
        .build();
}
