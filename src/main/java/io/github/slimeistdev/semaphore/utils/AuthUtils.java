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

package io.github.slimeistdev.semaphore.utils;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.player.Player;

public class AuthUtils {
    public static final String DEBUG_PERMISSION = "semaphore.debug";
    public static final String TELEPORT_PERMISSION = "semaphore.teleport";
    public static final String RECALCULATE_SIGNALS_PERMISSION = "semaphore.recalculate_signals";

    public static boolean isDebugAuthorized(Player player) {
        return Permissions.check(player, DEBUG_PERMISSION, 2);
    }

    public static boolean isDebugAuthorized(CommandSourceStack commandSourceStack) {
        return Permissions.check(commandSourceStack, DEBUG_PERMISSION, 2);
    }

    public static boolean isTeleportAuthorized(CommandSourceStack commandSourceStack) {
        return Permissions.check(commandSourceStack, TELEPORT_PERMISSION, 2);
    }

    public static boolean isRecalculateSignalsAuthorized(CommandSourceStack commandSourceStack) {
        return Permissions.check(commandSourceStack, RECALCULATE_SIGNALS_PERMISSION, 2);
    }
}
