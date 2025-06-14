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

package io.github.slimeistdev.semaphore.base.render;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.outliner.AABBOutline;
import com.simibubi.create.foundation.render.RenderTypes;
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class OutlineStyleRenderer {
    private static final MethodHolder INSTANCE = new MethodHolder(new AABB(BlockPos.ZERO));

    /**
     * Render an oriented bounding box, with center-of-rotation at (0, 0, 0)
     */
    public static void renderOBB(AABB bb, float yRot, float xRot, PoseStack ms, SuperRenderTypeBuffer buffer, float lineWidth, Vector4f color, int lightmap, boolean disableNormals) {
        VertexConsumer consumer = buffer.getBuffer(RenderTypes.getOutlineSolid());

        Vector3f center = bb.getCenter().toVector3f();

        Vector3f minPos = new Vector3f((float) bb.getXsize(), (float) bb.getYsize(), (float) bb.getZsize()).mul(-0.5f).add(center);
        Vector3f maxPos = new Vector3f((float) bb.getXsize(), (float) bb.getYsize(), (float) bb.getZsize()).mul(0.5f).add(center);

        {
            ms.pushPose();

            TransformStack.cast(ms)
                .rotateY(yRot + 180)
                .rotateZ(-xRot);

            INSTANCE.renderBoxEdges(ms, consumer, minPos, maxPos, lineWidth, color, lightmap, disableNormals);

            ms.popPose();
        }
    }

    private static class MethodHolder extends AABBOutline {
        private MethodHolder(AABB bb) {
            super(bb);
        }

        @Override
        protected void renderBoxEdges(PoseStack ms, VertexConsumer consumer, Vector3f minPos, Vector3f maxPos, float lineWidth, Vector4f color, int lightmap, boolean disableNormals) {
            super.renderBoxEdges(ms, consumer, minPos, maxPos, lineWidth, color, lightmap, disableNormals);
        }
    }
}
