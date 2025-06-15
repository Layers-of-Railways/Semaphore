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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.entity.Navigation;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.foundation.utility.Pair;
import io.github.slimeistdev.semaphore.content.train_debug.NavigationWatchdog;
import net.minecraft.world.level.Level;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(Navigation.class)
public abstract class MixinNavigation {
    @Shadow public int ticksWaitingForSignal;
    @Shadow public Train train;

    @Shadow public Pair<UUID, Boolean> waitingForSignal;
    @Unique
    private int semaphore$secondaryTicksWaitingForSignal;

    @WrapOperation(method = "tick", at = @At(value = "FIELD", target = "Lcom/simibubi/create/content/trains/entity/Navigation;ticksWaitingForSignal:I", opcode = Opcodes.PUTFIELD))
    private void storeWaitingTicks(Navigation instance, int value, Operation<Void> original) {
        if (value == 0) {
            this.semaphore$secondaryTicksWaitingForSignal += instance.ticksWaitingForSignal;
        }
        original.call(instance, value);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void clearWaitingTicks(Level level, CallbackInfo ci) {
        if (this.waitingForSignal == null) {
            this.semaphore$secondaryTicksWaitingForSignal = 0;
        } else {
            if (NavigationWatchdog.tick(train, level, this.semaphore$secondaryTicksWaitingForSignal + this.ticksWaitingForSignal)) {
                this.semaphore$secondaryTicksWaitingForSignal++; // ticksWaitingForSignal isn't always incremented, prevent double notifications
            }
        }
    }
}
