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

package io.github.slimeistdev.semaphore.content.train_debug;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.signal.SignalBoundary;
import com.simibubi.create.foundation.utility.Pair;
import io.github.slimeistdev.semaphore.Semaphore;
import io.github.slimeistdev.semaphore.mixin.common.AccessorNavigation;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.UUID;

public class NavigationWatchdog {
    private static final int THRESHOLD_TICKS = Integer.getInteger(
        "semaphore.navigation_watchdog.threshold_ticks",
        20 * 60 * 5 // 5 minutes by default
    );

    public static boolean tick(Train train, Level level, int waitingTicks) {
        if (THRESHOLD_TICKS <= 0) return false; // watchdog disabled
        if (!(level instanceof ServerLevel serverLevel)) return false;

        if (waitingTicks < THRESHOLD_TICKS || waitingTicks % THRESHOLD_TICKS != 0) return false;
        if (train.navigation.waitingForSignal == null) return false; // sanity check

        // verify that the signal isn't just forced red signal
        UUID signalId = train.navigation.waitingForSignal.getFirst();
        boolean primary = train.navigation.waitingForSignal.getSecond();
        SignalBoundary signal = train.graph.getPoint(EdgePointType.SIGNAL, signalId);
        if (signal != null && signal.isForcedRed(primary)) return false;

        // verify that none of the chained signals are forced red
        Map<UUID, Pair<SignalBoundary, Boolean>> chainedGroups = ((AccessorNavigation) train.navigation).semaphore$getWaitingForChainedGroups();
        for (Pair<SignalBoundary, Boolean> pair : chainedGroups.values()) {
            if (pair.getFirst().isForcedRed(pair.getSecond())) return false;
        }

        String tpCommand = String.format("/semaphore train_tp @s %s", train.id);

        var trainLabel = train.name.copy().withStyle(Style.EMPTY
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

        int minutesWaiting = waitingTicks / 20 / 60;
        int secondsWaiting = (waitingTicks / 20) % 60;

        var message = Component.translatable("semaphore.navigation_watchdog.stuck", trainLabel, minutesWaiting, secondsWaiting);

        serverLevel.getServer().getPlayerList().broadcastSystemMessage(message, false);
        Semaphore.LOGGER.warn("Train [{}] has been stuck at a signal for {} minutes and {} seconds. Teleport to it: {}",
            train.name.getString(), minutesWaiting, secondsWaiting, tpCommand);

        return true;
    }
}
