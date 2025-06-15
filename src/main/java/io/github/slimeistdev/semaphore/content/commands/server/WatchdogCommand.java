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
import com.simibubi.create.foundation.utility.Components;
import io.github.slimeistdev.semaphore.mixin_ducks.common.TrainDuck;
import io.github.slimeistdev.semaphore.utils.AuthUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class WatchdogCommand {
    public static ArgumentBuilder<CommandSourceStack, ?> register() {
        return literal("watchdog")
            .requires(AuthUtils::isAuthorized)
            .then(literal("disable")
                .then(argument("uuid", UuidArgument.uuid())
                    .executes(ctx -> setDisabled(
                        ctx.getSource(),
                        UuidArgument.getUuid(ctx, "uuid"),
                        true
                    ))
                )
            )
            .then(literal("enable")
                .then(argument("uuid", UuidArgument.uuid())
                    .executes(ctx -> setDisabled(
                        ctx.getSource(),
                        UuidArgument.getUuid(ctx, "uuid"),
                        false
                    ))
                )
            );
    }

    private static int setDisabled(CommandSourceStack source, UUID trainId, boolean disabled) {
        Train train = Create.RAILWAYS.trains.get(trainId);
        if (train == null || train.carriages.isEmpty()) {
            source.sendFailure(Component.translatable("semaphore.command.train_not_found", trainId));
            return 0;
        }

        ((TrainDuck) train).semaphore$setNavigationWatchdogDisabled(disabled);

        MutableComponent trainLabel = train.name.copy().withStyle(Style.EMPTY.withHoverEvent(new HoverEvent(
            HoverEvent.Action.SHOW_TEXT,
            Components.literal(trainId.toString())
        )).withColor(ChatFormatting.GOLD));

        String action = disabled ? "disabled" : "enabled";
        source.sendSuccess(
            () -> Component.translatable("semaphore.command.watchdog." + action, trainLabel),
            true
        );

        return 1;
    }
}
