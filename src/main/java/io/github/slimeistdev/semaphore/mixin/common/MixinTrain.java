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

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.TrackGraph;
import io.github.slimeistdev.semaphore.mixin_ducks.common.TrainDuck;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.UUID;

@Mixin(Train.class)
public class MixinTrain implements TrainDuck {
    @Unique
    private boolean semaphore$navigationWatchdogDisabled = false;

    @Override
    public boolean semaphore$isNavigationWatchdogDisabled() {
        return semaphore$navigationWatchdogDisabled;
    }

    @Override
    public void semaphore$setNavigationWatchdogDisabled(boolean disabled) {
        semaphore$navigationWatchdogDisabled = disabled;
    }

    @Inject(method = "write", at = @At("RETURN"))
    private void semaphore$write(DimensionPalette dimensions, CallbackInfoReturnable<CompoundTag> cir) {
        if (semaphore$navigationWatchdogDisabled) {
            cir.getReturnValue().putBoolean("SemaphoreNavigationWatchdogDisabled", true);
        }
    }

    @Inject(method = "read", at = @At("RETURN"))
    private static void semaphore$read(CompoundTag tag, Map<UUID, TrackGraph> trackNetworks, DimensionPalette dimensions, CallbackInfoReturnable<Train> cir) {
        if (tag.getBoolean("SemaphoreNavigationWatchdogDisabled")) {
            Train train = cir.getReturnValue();
            if (train != null) {
                ((MixinTrain) (Object) train).semaphore$navigationWatchdogDisabled = true;
            }
        }
    }
}
