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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.simibubi.create.content.trains.graph.TrackGraphVisualizer;
import com.simibubi.create.content.trains.signal.SignalEdgeGroup;
import com.simibubi.create.foundation.outliner.Outline;
import com.simibubi.create.foundation.outliner.Outliner;
import io.github.slimeistdev.semaphore.content.signal_debug.ClientSignalDebugManager;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Mixin(TrackGraphVisualizer.class)
public class MixinTrackGraphVisualizer {
    @WrapOperation(
        method = "visualiseSignalEdgeGroups",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"
        ),
        remap = false
    )
    private static <K, V> V captureSignalEdgeGroup(
        Map<K, V> instance,
        Object o,
        Operation<V> original,
        @Share(value = "signalGroup") LocalRef<SignalEdgeGroup> sharedGroup
    ) {
        V v = original.call(instance, o);
        if (v instanceof SignalEdgeGroup group) {
            sharedGroup.set(group);
        }
        return v;
    }

    @WrapOperation(
        method = "visualiseSignalEdgeGroups",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/foundation/outliner/Outliner;showLine(Ljava/lang/Object;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Lcom/simibubi/create/foundation/outliner/Outline$OutlineParams;"
        ),
        remap = false
    )
    private static Outline.OutlineParams dbg(
        Outliner instance,
        Object slot,
        Vec3 start,
        Vec3 end,
        Operation<Outline.OutlineParams> original,
        @Share(value = "signalGroup") LocalRef<SignalEdgeGroup> group
    ) {
        ClientSignalDebugManager.debugEdge(instance, group.get(), start, end);
        return original.call(instance, slot, start, end);
    }
}
