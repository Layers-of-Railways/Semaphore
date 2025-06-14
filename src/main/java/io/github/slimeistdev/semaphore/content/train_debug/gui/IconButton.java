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

package io.github.slimeistdev.semaphore.content.train_debug.gui;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import net.minecraft.client.gui.GuiGraphics;

public abstract class IconButton extends SimpleButton {
    private final AllIcons icon;

    public IconButton(int x, int y, AllIcons icon) {
        super(x, y, 18, 18);
        this.icon = icon;
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY) {
        PoseStack ms = gui.pose();

        (isHovered(mouseX, mouseY) ? AllGuiTextures.BUTTON_HOVER : AllGuiTextures.BUTTON).render(gui, x, y);

        {
            ms.pushPose();

            TransformStack.cast(ms)
                .translateZ(1);

            icon.render(gui, x + 1, y + 1);

            ms.popPose();
        }
    }
}
