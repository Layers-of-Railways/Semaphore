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

package io.github.slimeistdev.semaphore.content.commands.server;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Train;
import io.github.slimeistdev.semaphore.mixin_ducks.common.TrainDuck;
import io.github.slimeistdev.semaphore.utils.AuthUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import static net.minecraft.commands.Commands.literal;

public class RecalculateSignalsCommand {
    public static ArgumentBuilder<CommandSourceStack, ?> register() {
        return literal("recalculate_signals")
            .requires(AuthUtils::isRecalculateSignalsAuthorized)
            .executes(ctx -> recalculateSignals(ctx.getSource()));
    }

    private static int recalculateSignals(CommandSourceStack source) {
        int count = 0;

        for (Train train : Create.RAILWAYS.trains.values()) {
            if (train.updateSignalBlocks) continue;
            count++;
            train.updateSignalBlocks = true;
            ((TrainDuck) train).semaphore$reduceSignalsOnly(c -> source.sendSuccess(() -> c, true));
        }

        if (count == 0) {
            source.sendFailure(Component.translatable("semaphore.command.recalculate_signals.no_trains"));
        } else {
            final int count$ = count;
            source.sendSuccess(() -> Component.translatable("semaphore.command.recalculate_signals.success", count$), true);
        }

        return count;
    }
}
