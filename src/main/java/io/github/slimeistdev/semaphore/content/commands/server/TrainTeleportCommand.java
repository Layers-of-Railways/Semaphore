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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class TrainTeleportCommand {
    private static final SimpleCommandExceptionType INVALID_POSITION = new SimpleCommandExceptionType(Component.translatable("commands.teleport.invalidPosition"));

    public static ArgumentBuilder<CommandSourceStack, ?> register() {
        return literal("train_tp")
            .requires(AuthUtils::isAuthorized)
            .then(argument("uuid", UuidArgument.uuid())
                .executes(ctx -> teleportToTrain(
                    ctx.getSource(),
                    Collections.singleton(ctx.getSource().getEntityOrException()),
                    UuidArgument.getUuid(ctx, "uuid")
                ))
            )
            .then(argument("targets", EntityArgument.entities())
                .then(argument("uuid", UuidArgument.uuid())
                    .executes(ctx -> teleportToTrain(
                        ctx.getSource(),
                        EntityArgument.getEntities(ctx, "targets"),
                        UuidArgument.getUuid(ctx, "uuid")
                    ))
                )
            );
    }

    private static int teleportToTrain(CommandSourceStack source, Collection<? extends Entity> targets, UUID trainId) throws CommandSyntaxException {
        Train train = Create.RAILWAYS.trains.get(trainId);
        if (train == null || train.carriages.isEmpty()) {
            source.sendFailure(Component.translatable("command.semaphore.train_tp.not_found", trainId));
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
            source.sendFailure(Component.translatable("command.semaphore.train_tp.lost", trainLabel));
            return 0;
        }

        ServerLevel targetLevel = source.getServer().getLevel(dimension);

        for (Entity entity : targets) {
            performTeleport(entity, targetLevel, pos.x, pos.y + 7, pos.z);
        }

        if (targets.size() == 1) {
            source.sendSuccess(
                () -> Component.translatable(
                    "command.semaphore.train_tp.success.single",
                    targets.iterator().next().getDisplayName(),
                    trainLabel
                ),
                true
            );
        } else {
            source.sendSuccess(
                () -> Component.translatable(
                    "command.semaphore.train_tp.success.multiple",
                    targets.size(),
                    trainLabel
                ),
                true
            );
        }

        return targets.size();
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
