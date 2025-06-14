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

import com.railwayteam.railways.multiloader.PlayerSelection;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.function.Predicate;

public class SemaphorePlayerSelection extends PlayerSelection {
    private final Collection<ServerPlayer> players;

    private SemaphorePlayerSelection(Collection<ServerPlayer> players) {
        this.players = players;
    }

    @Override
    public void accept(ResourceLocation id, FriendlyByteBuf buffer) {
        Packet<?> packet = ServerPlayNetworking.createS2CPacket(id, buffer);
        for (ServerPlayer player : players) {
            ServerPlayNetworking.getSender(player).sendPacket(packet);
        }
    }

    public static SemaphorePlayerSelection tracking(Train train) {
        Set<ServerPlayer> players = new HashSet<>();

        for (Carriage carriage : train.carriages) {
            carriage.forEachPresentEntity(cce -> {
                players.addAll(PlayerLookup.tracking(cce));
            });
        }

        return new SemaphorePlayerSelection(players);
    }

    public SemaphorePlayerSelection filtered(Predicate<ServerPlayer> player) {
        List<ServerPlayer> filteredPlayers = new ArrayList<>(players.size());
        for (ServerPlayer p : players) {
            if (player.test(p)) {
                filteredPlayers.add(p);
            }
        }
        return new SemaphorePlayerSelection(filteredPlayers);
    }
}
