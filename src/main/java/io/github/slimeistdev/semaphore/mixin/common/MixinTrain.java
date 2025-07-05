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
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.TrackGraph;
import io.github.slimeistdev.semaphore.Semaphore;
import io.github.slimeistdev.semaphore.mixin_ducks.common.TrainDuck;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.function.Consumer;

@Mixin(Train.class)
public class MixinTrain implements TrainDuck {
    @Shadow public List<Carriage> carriages;

    @Unique
    private boolean semaphore$navigationWatchdogDisabled = false;

    @Unique
    private boolean semaphore$reduceSignalsOnly = false;

    @Unique
    @Nullable
    private Set<UUID> semaphore$oldOccupiedSignalBlocks = null;

    @Unique
    @Nullable
    private Consumer<Component> semaphore$feedback = null;

    @Override
    public boolean semaphore$isNavigationWatchdogDisabled() {
        return semaphore$navigationWatchdogDisabled;
    }

    @Override
    public void semaphore$setNavigationWatchdogDisabled(boolean disabled) {
        semaphore$navigationWatchdogDisabled = disabled;
    }

    @Override
    public void semaphore$reduceSignalsOnly(@Nullable Consumer<Component> feedback) {
        semaphore$reduceSignalsOnly = true;
        semaphore$feedback = feedback;
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

    @WrapOperation(method = "earlyTick", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/entity/Train;collectInitiallyOccupiedSignalBlocks()V"))
    private void reduceOnly(Train instance, Operation<Void> original) {
        if (semaphore$reduceSignalsOnly) {
            semaphore$oldOccupiedSignalBlocks = new HashSet<>(instance.occupiedSignalBlocks.keySet());
        }

        original.call(instance);

        if (semaphore$reduceSignalsOnly) {
            if (semaphore$oldOccupiedSignalBlocks != null) { // sanity check
                // prevent occupation of new signal blocks
                instance.occupiedSignalBlocks.keySet().retainAll(semaphore$oldOccupiedSignalBlocks);

                // convert oldOccupiedSignalBlocks to a set of signal blocks that were removed by the recalculation
                semaphore$oldOccupiedSignalBlocks.removeAll(instance.occupiedSignalBlocks.keySet());

                if (!semaphore$oldOccupiedSignalBlocks.isEmpty()) {
                    String tpCommand = String.format("/semaphore train_tp @s %s", instance.id);

                    var trainLabel = instance.name.copy().withStyle(Style.EMPTY
                        .withClickEvent(new ClickEvent(
                            ClickEvent.Action.SUGGEST_COMMAND,
                            tpCommand
                        ))
                        .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            Component.translatable("semaphore.tooltip.train_tp")
                        ))
                        .withColor(ChatFormatting.GOLD)
                    );

                    String removedStr = semaphore$oldOccupiedSignalBlocks.stream()
                        .map(UUID::toString)
                        .toList().toString();

                    if (semaphore$feedback != null) {
                        semaphore$feedback.accept(Component.translatable(
                            "semaphore.command.recalculate_signals.success.train",
                            trainLabel,
                            semaphore$oldOccupiedSignalBlocks.size(),
                            removedStr
                        ).withStyle(ChatFormatting.GREEN));
                    }

                    Semaphore.LOGGER.warn("Removed {} wrongly occupied signal sections from train '{}' ({}): {}",
                        semaphore$oldOccupiedSignalBlocks.size(),
                        instance.name.getString(),
                        instance.id,
                        removedStr
                    );
                }
            }
        }

        semaphore$reduceSignalsOnly = false;
        semaphore$oldOccupiedSignalBlocks = null;
        semaphore$feedback = null;
    }
}
