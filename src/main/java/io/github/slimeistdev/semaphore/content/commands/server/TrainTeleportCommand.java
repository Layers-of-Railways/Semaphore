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
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.foundation.utility.Components;
import io.github.slimeistdev.semaphore.mixin_ducks.common.ServerPlayerDuck;
import io.github.slimeistdev.semaphore.utils.AuthUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class TrainTeleportCommand {
    private static final SimpleCommandExceptionType INVALID_POSITION = new SimpleCommandExceptionType(Component.translatable("commands.teleport.invalidPosition"));

    public static ArgumentBuilder<CommandSourceStack, ?> register() {
        return literal("train_tp")
            .requires(AuthUtils::isTeleportAuthorized)
            .then(argument("uuid", UuidArgument.uuid())
                .executes(ctx -> teleportToTrain(
                    ctx.getSource(),
                    Collections.singleton(ctx.getSource().getPlayerOrException()),
                    UuidArgument.getUuid(ctx, "uuid")
                ))
            )
            .then(literal("back")
                .executes(ctx -> teleportBack(
                    ctx.getSource(),
                    Collections.singleton(ctx.getSource().getPlayerOrException())
                ))
            )
            .then(argument("targets", EntityArgument.players())
                .then(argument("uuid", UuidArgument.uuid())
                    .executes(ctx -> teleportToTrain(
                        ctx.getSource(),
                        EntityArgument.getPlayers(ctx, "targets"),
                        UuidArgument.getUuid(ctx, "uuid")
                    ))
                )
                .then(literal("back")
                    .executes(ctx -> teleportBack(
                        ctx.getSource(),
                        EntityArgument.getPlayers(ctx, "targets")
                    ))
                )
            );
    }

    private static int teleportToTrain(CommandSourceStack source, Collection<? extends ServerPlayer> targets, UUID trainId) throws CommandSyntaxException {
        Train train = Create.RAILWAYS.trains.get(trainId);
        if (train == null || train.carriages.isEmpty()) {
            source.sendFailure(Component.translatable("semaphore.command.train_not_found", trainId));
            return 0;
        }

        MutableComponent trainLabel = train.name.copy().withStyle(Style.EMPTY.withHoverEvent(new HoverEvent(
            HoverEvent.Action.SHOW_TEXT,
            Components.literal(trainId.toString())
        )));

        Carriage carriage = train.carriages.get(0);
        ResourceKey<Level> dimension = carriage.leadingBogey().getDimension();
        Vec3 pos = carriage.leadingBogey().getAnchorPosition();
        if (pos == null) {
            source.sendFailure(Component.translatable("semaphore.command.train_tp.lost", trainLabel));
            return 0;
        }

        ServerLevel targetLevel = source.getServer().getLevel(dimension);

        for (ServerPlayer player : targets) {
            ((ServerPlayerDuck) player).semaphore$getTrainTeleportStack().push(player.position());

            performTeleport(player, targetLevel, pos.x, pos.y + 7, pos.z);
        }

        if (targets.size() == 1) {
            source.sendSuccess(
                () -> Component.translatable(
                    "semaphore.command.train_tp.success.single",
                    targets.iterator().next().getDisplayName(),
                    trainLabel
                ),
                true
            );
        } else {
            source.sendSuccess(
                () -> Component.translatable(
                    "semaphore.command.train_tp.success.multiple",
                    targets.size(),
                    trainLabel
                ),
                true
            );
        }

        return targets.size();
    }

    private static int teleportBack(CommandSourceStack source, Collection<? extends ServerPlayer> targets) throws CommandSyntaxException {
        int successCount = 0;
        ServerPlayer successPlayer = null;

        for (ServerPlayer player : targets) {
            Stack<Vec3> teleportStack = ((ServerPlayerDuck) player).semaphore$getTrainTeleportStack();

            if (teleportStack.isEmpty()) {
                source.sendFailure(Component.translatable("semaphore.command.train_tp.back.empty_stack", player.getDisplayName()));
                continue;
            }

            Vec3 lastPosition = teleportStack.pop();
            performTeleport(player, source.getLevel(), lastPosition.x, lastPosition.y, lastPosition.z);
            successCount++;
            successPlayer = player;
        }

        if (successCount == 1) {
            final ServerPlayer successPlayer$ = successPlayer;
            source.sendSuccess(
                () -> Component.translatable(
                    "semaphore.command.train_tp.back.success.single",
                    successPlayer$
                ),
                true
            );
        } else if (successCount > 1) {
            final int successCount$ = successCount;
            source.sendSuccess(
                () -> Component.translatable(
                    "semaphore.command.train_tp.back.success.multiple",
                    successCount$
                ),
                true
            );
        }

        return successCount;
    }

    private static void performTeleport(
        Entity entity,
        ServerLevel level,
        double x,
        double y,
        double z
    ) throws CommandSyntaxException {
        BlockPos blockPos = BlockPos.containing(x, y, z);
        if (!Level.isInSpawnableBounds(blockPos)) {
            throw INVALID_POSITION.create();
        } else {
            float f = Mth.wrapDegrees(entity.getYRot());
            float g = Mth.wrapDegrees(entity.getXRot());
            if (entity.teleportTo(level, x, y, z, Set.of(), f, g)) {
                if (!(entity instanceof LivingEntity livingEntity) || !livingEntity.isFallFlying()) {
                    entity.setDeltaMovement(entity.getDeltaMovement().multiply(1.0, 0.0, 1.0));
                    entity.setOnGround(true);
                }

                if (entity instanceof PathfinderMob pathfinderMob) {
                    pathfinderMob.getNavigation().stop();
                }
            }
        }
    }
}
