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

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.railwayteam.railways.compat.journeymap.UsernameUtils;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.UIRenderHelper;
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;
import com.simibubi.create.foundation.utility.Color;
import io.github.slimeistdev.semaphore.base.render.OutlineStyleRenderer;
import io.github.slimeistdev.semaphore.content.commands.client.ToggleDebugCommand;
import io.github.slimeistdev.semaphore.content.train_debug.gui.IconButton;
import io.github.slimeistdev.semaphore.content.train_debug.gui.SimpleButton;
import io.github.slimeistdev.semaphore.mixin_ducks.client.TrainDuck;
import io.github.slimeistdev.semaphore.network.SemaphorePackets;
import io.github.slimeistdev.semaphore.network.packets.c2s.RequestSignalDebugPacket;
import io.github.slimeistdev.semaphore.registry.SemaphoreIcons;
import io.github.slimeistdev.semaphore.utils.Utils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.lang.ref.WeakReference;
import java.util.*;

@Environment(EnvType.CLIENT)
public class TrainDebugManager {
    private static PerCarriageData topData = null;

    private static final Map<UUID, String> debugLabels = new HashMap<>();

    private final WeakReference<Train> trainRef;
    private final List<PerCarriageData> perCarriageData = new ArrayList<>();

    private int ticksSinceLastUpdate = 0;
    private final Set<UUID> occupiedSignalBlocks = new HashSet<>();
    private final Set<UUID> reservedSignalBlocks = new HashSet<>();
    private final Set<UUID> awaitingSignalBlocks = new HashSet<>();

    public TrainDebugManager(Train train) {
        this.trainRef = new WeakReference<>(train);
    }

    public static boolean clicked() {
        if (!ToggleDebugCommand.trainEnabled || topData == null) return false;

        topData.clicked();
        return true;
    }

    public static void setDebugLabels(Map<UUID, String> labels) {
        debugLabels.clear();
        debugLabels.putAll(labels);
    }

    public void updateFromPacket(Set<UUID> occupiedSignalBlocks, Set<UUID> reservedSignalBlocks, Set<UUID> awaitingSignalBlocks) {
        this.occupiedSignalBlocks.clear();
        this.reservedSignalBlocks.clear();
        this.awaitingSignalBlocks.clear();

        this.occupiedSignalBlocks.addAll(occupiedSignalBlocks);
        this.reservedSignalBlocks.addAll(reservedSignalBlocks);
        this.awaitingSignalBlocks.addAll(awaitingSignalBlocks);
        this.ticksSinceLastUpdate = 0;
    }

    public Set<UUID> getOccupiedSignalBlocks() {
        return occupiedSignalBlocks;
    }

    public Set<UUID> getReservedSignalBlocks() {
        return reservedSignalBlocks;
    }

    public Set<UUID> getAwaitingSignalBlocks() {
        return awaitingSignalBlocks;
    }

    public static void earlyTick() {
        topData = null;
    }

    public void tick(Train this$) {
        if (ticksSinceLastUpdate++ == 60) {
            occupiedSignalBlocks.clear();
            reservedSignalBlocks.clear();
        }

        if (!ToggleDebugCommand.trainEnabled) return;

        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        while (perCarriageData.size() > this$.carriages.size()) {
            perCarriageData.remove(perCarriageData.size() - 1);
        }
        while (perCarriageData.size() < this$.carriages.size()) {
            perCarriageData.add(new PerCarriageData());
        }

        for (int i = 0; i < this$.carriages.size(); i++) {
            Carriage carriage = this$.carriages.get(i);
            PerCarriageData carriage$ = perCarriageData.get(i);
            var dce = carriage.getDimensional(level);
            var entity = dce.entity.get();
            if (entity == null) continue;

            Contraption contraption = entity.getContraption();
            if (contraption == null) return;

            // Calculate UI interactions

            Minecraft mc = Minecraft.getInstance();
            Camera mainCamera = mc.gameRenderer.getMainCamera();
            Vector3f camera = mainCamera.getPosition().toVector3f();
            Vector3f facing = mainCamera.getLookVector();

            float viewYRot = entity.getViewYRot(1);
            float viewXRot = entity.getViewXRot(1);

            AABB bb = new AABB(BlockPos.ZERO);
            for (BlockPos pos : contraption.getBlocks().keySet()) {
                bb = bb.minmax(new AABB(pos));
            }
            bb = bb.move(-0.5, -0.5, -0.5);

            PoseStack ms = new PoseStack();
            TransformStack.cast(ms)
                    .translate(entity.getPosition(1));
            transformForGUI(ms, viewYRot, viewXRot, bb);

            Matrix4f invMat = new Matrix4f(ms.last().pose()).invert();
            Vector3f guiCamera = transformPos(camera, invMat);
            Vector3f guiFacing = transformDirection(facing, invMat).normalize();

            carriage$.pos = guiCamera;

            boolean worldHit;
            if (mc.hitResult == null || mc.hitResult.getType() == HitResult.Type.MISS) {
                worldHit = false;
            } else {
                worldHit = transformPos(mc.hitResult.getLocation().toVector3f(), invMat).z >= 0;
            }

            float hitTime = Math.abs(guiCamera.z / guiFacing.z);

            if (guiCamera.z <= 0 || worldHit) {
                carriage$.highlighted = false;
                carriage$.depth = Float.MAX_VALUE;
            } else {
                int x = (int) (guiCamera.x + guiFacing.x * hitTime);
                int y = (int) (guiCamera.y + guiFacing.y * hitTime);

                carriage$.highlighted = x >= 0 && x <= 128
                    && y >= 0 && y <= 64;
                carriage$.x = x;
                carriage$.y = y;

                carriage$.depth = hitTime;

                if (carriage$.highlighted && (topData == null || topData.depth > carriage$.depth)) {
                    topData = carriage$;
                }
            }
        }
    }

    private static Vector3f transformPos(Vector3f pos, Matrix4f mat) {
        Vector4f v4 = new Vector4f(pos, 1.0f);
        v4.mul(mat);
        return new Vector3f(v4.x, v4.y, v4.z);
    }

    private static Vector3f transformDirection(Vector3f dir, Matrix4f mat) {
        Vector4f v4 = new Vector4f(dir, 0.0f);
        v4.mul(mat);
        return new Vector3f(v4.x, v4.y, v4.z);
    }

    public static Color getTrainColor(Train train) {
        if (train == null) return Color.WHITE;

        int colorId = train.id.hashCode();
        return Color.rainbowColor(colorId);
    }

    public static void render(CarriageContraptionEntity entity, float yaw, float partialTicks, PoseStack ms,
                              MultiBufferSource buffers, int overlay) {
        if (!ToggleDebugCommand.trainEnabled) return;

        Carriage carriage = entity.getCarriage();
        Contraption contraption = entity.getContraption();
        if (carriage == null || contraption == null) return;

        Train train = carriage.train;
        if (train == null) return;
        TrainDebugManager train$ = ((TrainDuck) train).semaphore$getDebugManager();

        float viewYRot = entity.getViewYRot(partialTicks);
        float viewXRot = entity.getViewXRot(partialTicks);

        AABB bb = new AABB(BlockPos.ZERO);
        for (BlockPos pos : contraption.getBlocks().keySet()) {
            bb = bb.minmax(new AABB(pos));
        }

        bb = bb.move(-0.5, -0.5, -0.5);

        {
            ms.pushPose();

            TransformStack.cast(ms)
                    .translate(0, 0.5f, 0);

            OutlineStyleRenderer.renderOBB(
                bb,
                viewYRot,
                viewXRot,
                ms,
                SuperRenderTypeBuffer.getInstance(),
                1 / 16f,
                new Vector4f(
                    getTrainColor(carriage.train).asVectorF(),
                    1.0f
                ),
                LightTexture.FULL_BRIGHT,
                false
            );

            ms.popPose();
        }

        {
            ms.pushPose();

            transformForGUI(ms, viewYRot, viewXRot, bb);
            train$.renderTrainUI(train, carriage, ms, buffers);

            ms.popPose();
        }
    }

    private static void transformForGUI(PoseStack ms, float viewYRot, float viewXRot, AABB bb) {
        Minecraft mc = Minecraft.getInstance();
        Vector3f facing = mc.gameRenderer.getMainCamera().getLookVector();

        double horizAngle = Mth.atan2(facing.z, facing.x);
        //double vertAngle = Mth.atan2(facing.y, Math.sqrt(facing.x * facing.x + facing.z * facing.z));
        TransformStack.cast(ms)
            .translateY(0.5)

            .rotateY(viewYRot + 180)
            .rotateZ(-viewXRot)

            .translate(bb.getCenter())
            .translateY(bb.getYsize() / 2)
            .translateY(3)

            .rotateZ(viewXRot)
            .rotateY(-(viewYRot + 180))

            .rotateY(270 - Math.toDegrees(horizAngle))
            //.rotateX(Math.toDegrees(vertAngle))

            .scale(1, -1, 1)
            .scale(1/32f);

        int width = 128;

        TransformStack.cast(ms)
            .translateX(-width/2f)
            .scale(1, 1, 1/64f);
    }

    private void renderTrainUI(
        Train this$, Carriage carriage,
        PoseStack ms, MultiBufferSource buffers
    ) {
        Minecraft mc = Minecraft.getInstance();

        int i = this$.carriages.indexOf(carriage);
        if (i < 0 || i >= perCarriageData.size()) return;
        PerCarriageData carriage$ = perCarriageData.get(i);

        if (carriage$ != topData) carriage$.highlighted = false;

        int width = 128;
        int height = 64;

        GuiGraphics gui = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
        gui.pose().last().pose().set(ms.last().pose());
        gui.pose().last().normal().set(ms.last().normal());

        RenderSystem.enableDepthTest();

        UIRenderHelper.drawStretched(gui, -2, -2, width + 4, height + 4,
            -1, AllGuiTextures.VALUE_SETTINGS_BAR_BG);
        renderBrassFrame(gui, -4, -4, width + 8, height + 8);

        gui.drawCenteredString(mc.font, this$.name, width/2, 0, 0xFFFFFF);

        String ownerName = UsernameUtils.INSTANCE.getName(this$.owner);
        gui.drawCenteredString(mc.font, ownerName, width/2, 10, 0xFFFFFF);

        if (Utils.isDevEnv()) {
            gui.drawCenteredString(mc.font, "DBG: " + carriage$.x + ", " + carriage$.y, width / 2, 20,
                carriage$.highlighted ? 0xFF0000 : 0xFFFFFF);
        }

        gui.drawString(mc.font, Component.translatable("semaphore.train_debug.debug_signalling"), 4, height - 18, 0xFFFFFF);

        carriage$.render(gui);

        String debugLabel = debugLabels.get(this$.id);
        if (debugLabel != null) {
            gui.pose().pushPose();

            int lblHeight = mc.font.lineHeight + 12;
            int lblWidth = mc.font.width(debugLabel) + 12;

            int scale = 4;

            TransformStack.cast(gui.pose())
                .translate((width - lblWidth*scale) / 2f, -lblHeight*scale - 8, 0)
                .scale(scale);

            UIRenderHelper.drawStretched(gui, 2, 2, lblWidth - 4, lblHeight - 4,
                -8, AllGuiTextures.VALUE_SETTINGS_BAR_BG);
            renderBrassFrame(gui, 0, 0, lblWidth, lblHeight);

            gui.drawString(mc.font, debugLabel, 6, 6, 0xFFAA00);

            gui.pose().popPose();
        }

        RenderSystem.disableDepthTest();
    }

    @SuppressWarnings("SameParameterValue")
    private static void renderBrassFrame(GuiGraphics graphics, int x, int y, int w, int h) {
        AllGuiTextures.BRASS_FRAME_TL.render(graphics, x, y);
        AllGuiTextures.BRASS_FRAME_TR.render(graphics, x + w - 4, y);
        AllGuiTextures.BRASS_FRAME_BL.render(graphics, x, y + h - 4);
        AllGuiTextures.BRASS_FRAME_BR.render(graphics, x + w - 4, y + h - 4);
        int zLevel = 0;

        if (h > 8) {
            UIRenderHelper.drawStretched(graphics, x, y + 4, 3, h - 8, zLevel, AllGuiTextures.BRASS_FRAME_LEFT);
            UIRenderHelper.drawStretched(graphics, x + w - 3, y + 4, 3, h - 8, zLevel, AllGuiTextures.BRASS_FRAME_RIGHT);
        }

        if (w > 8) {
            UIRenderHelper.drawCropped(graphics, x + 4, y, w - 8, 3, zLevel, AllGuiTextures.BRASS_FRAME_TOP);
            UIRenderHelper.drawCropped(graphics, x + 4, y + h - 3, w - 8, 3, zLevel, AllGuiTextures.BRASS_FRAME_BOTTOM);
        }
    }

    private class PerCarriageData {
        boolean highlighted = false;
        Vector3f pos = new Vector3f(0, 0, 0);
        int x;
        int y;
        float depth;

        final List<SimpleButton> buttons = List.of(
            new IconButton(128 - 18 - 4, 64 - 18 - 4, SemaphoreIcons.I_DEBUG) {
                @Override
                public void clicked() {
                    TrainDebugManager.setDebugLabels(Map.of());
                    Train train = trainRef.get();
                    if (train == null) return;
                    SemaphorePackets.PACKETS.send(new RequestSignalDebugPacket(train.id));
                }
            }
        );

        void render(GuiGraphics gui) {
            int x$ = highlighted ? x : -2048;
            int y$ = highlighted ? y : -2048;
            for (SimpleButton button : buttons) {
                button.render(gui, x$, y$);
            }
        }

        public void clicked() {
            for (SimpleButton button : buttons) {
                if (button.isHovered(x, y)) {
                    button.clicked();
                    Minecraft.getInstance().getSoundManager()
                        .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    return;
                }
            }
        }
    }
}
