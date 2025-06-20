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

package io.github.slimeistdev.semaphore.registry;

import com.mojang.brigadier.CommandDispatcher;
import io.github.slimeistdev.semaphore.content.commands.server.RecalculateSignalsCommand;
import io.github.slimeistdev.semaphore.content.commands.server.ReloadCommandsCommand;
import io.github.slimeistdev.semaphore.content.commands.server.TrainTeleportCommand;
import io.github.slimeistdev.semaphore.content.commands.server.WatchdogCommand;
import io.github.slimeistdev.semaphore.utils.Utils;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import static net.minecraft.commands.Commands.literal;

public class SemaphoreCommandsServer {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context, Commands.CommandSelection selection) {
        var semaphoreCommand = literal("semaphore")
            .then(TrainTeleportCommand.register())
            .then(WatchdogCommand.register())
            .then(RecalculateSignalsCommand.register());

        if (Utils.isDevEnv()) {
            semaphoreCommand = semaphoreCommand
                .then(ReloadCommandsCommand.register(dispatcher, context, selection, SemaphoreCommandsServer::register));
        }

        dispatcher.register(semaphoreCommand);
    }
}
