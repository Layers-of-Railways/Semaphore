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

package io.github.slimeistdev.semaphore.mixin.common;

import io.github.slimeistdev.semaphore.mixin_ducks.common.ServerPlayerDuck;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Stack;

@Mixin(ServerPlayer.class)
public class MixinServerPlayer implements ServerPlayerDuck {
    @Unique
    private final Stack<Vec3> semaphore$trainTeleportStack = new Stack<>();

    @Override
    public Stack<Vec3> semaphore$getTrainTeleportStack() {
        return semaphore$trainTeleportStack;
    }
}
