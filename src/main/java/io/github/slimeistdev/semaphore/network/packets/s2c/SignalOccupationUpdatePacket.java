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
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.trains.entity.Train;
import io.github.slimeistdev.semaphore.content.train_debug.TrainDebugManager;
import io.github.slimeistdev.semaphore.mixin.common.AccessorNavigation;
import io.github.slimeistdev.semaphore.mixin_ducks.client.TrainDuck;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public record SignalOccupationUpdatePacket(
    UUID train,
    Set<UUID> occupiedSignalBlocks,
    Set<UUID> reservedSignalBlocks,
    Set<UUID> awaitingSignalBlocks
) implements S2CPacket {

    private static Set<UUID> getAwaitingSignalBlocks(Train train) {
        if (train.navigation == null) return Set.of();
        if (train.navigation.waitingForSignal == null) return Set.of();
        Set<UUID> awaitingBlocks = new HashSet<>();
        awaitingBlocks.add(train.navigation.waitingForSignal.getFirst());
        awaitingBlocks.addAll(((AccessorNavigation) train.navigation).semaphore$getWaitingForChainedGroups().keySet());
        return awaitingBlocks;
    }

    public SignalOccupationUpdatePacket(Train train) {
        this(
            train.id,
            new HashSet<>(train.occupiedSignalBlocks.keySet()),
            new HashSet<>(train.reservedSignalBlocks),
            getAwaitingSignalBlocks(train)
        );
    }

    public SignalOccupationUpdatePacket(FriendlyByteBuf buf) {
        this(
            buf.readUUID(),
            buf.readCollection(HashSet::new, FriendlyByteBuf::readUUID),
            buf.readCollection(HashSet::new, FriendlyByteBuf::readUUID),
            buf.readCollection(HashSet::new, FriendlyByteBuf::readUUID)
        );
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeUUID(train);
        buffer.writeCollection(occupiedSignalBlocks, FriendlyByteBuf::writeUUID);
        buffer.writeCollection(reservedSignalBlocks, FriendlyByteBuf::writeUUID);
        buffer.writeCollection(awaitingSignalBlocks, FriendlyByteBuf::writeUUID);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void handle(Minecraft mc) {
        Train train = CreateClient.RAILWAYS.trains.get(this.train);
        if (train == null) return;

        TrainDebugManager debugManager = ((TrainDuck) train).semaphore$getDebugManager();
        debugManager.updateFromPacket(occupiedSignalBlocks, reservedSignalBlocks, awaitingSignalBlocks);
    }
}
