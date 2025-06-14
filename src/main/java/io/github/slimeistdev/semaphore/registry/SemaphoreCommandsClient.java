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
import io.github.slimeistdev.semaphore.content.commands.client.ReloadCommandsCommand;
import io.github.slimeistdev.semaphore.content.commands.client.ToggleDebugCommand;
import io.github.slimeistdev.semaphore.utils.Utils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

@Environment(EnvType.CLIENT)
public class SemaphoreCommandsClient {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext context) {
        var semaphoreCommand = literal("semaphorec")
            .then(ToggleDebugCommand.register());

        if (Utils.isDevEnv()) {
            semaphoreCommand = semaphoreCommand
                .then(ReloadCommandsCommand.register(dispatcher, context, SemaphoreCommandsClient::register));
        }

        dispatcher.register(semaphoreCommand);
    }
}
