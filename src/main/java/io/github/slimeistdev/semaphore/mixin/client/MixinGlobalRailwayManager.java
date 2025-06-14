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

package io.github.slimeistdev.semaphore.mixin.client;

import com.simibubi.create.content.trains.GlobalRailwayManager;
import com.simibubi.create.content.trains.entity.Train;
import io.github.slimeistdev.semaphore.content.signal_debug.ClientSignalDebugManager;
import io.github.slimeistdev.semaphore.content.train_debug.TrainDebugManager;
import io.github.slimeistdev.semaphore.mixin_ducks.client.TrainDuck;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;

@Mixin(GlobalRailwayManager.class)
public class MixinGlobalRailwayManager {
    @Shadow(remap = false) public Map<UUID, Train> trains;

    @Inject(method = "tickSignalOverlay", at = @At(value = "INVOKE", target = "Ljava/util/Map;values()Ljava/util/Collection;"), remap = false)
    private void prepareDebug(CallbackInfo ci) {
        ClientSignalDebugManager.prepareDebug();
    }

    @Inject(method = "clientTick", at = @At("HEAD"), remap = false)
    private void debugTick(CallbackInfo ci) {
        TrainDebugManager.earlyTick();
        for (Train train : this.trains.values()) {
            ((TrainDuck) train).semaphore$getDebugManager().tick(train);
        }
    }
}
