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

import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.GuiGraphics;

public class ToggleButton extends IconButton {
    private boolean checked = false;
    private BooleanConsumer changeListener;

    public ToggleButton(int x, int y, AllIcons icon) {
        super(x, y, icon);
    }

    public ToggleButton setChangeListener(BooleanConsumer changeListener) {
        this.changeListener = changeListener;
        return this;
    }

    @Override
    public final void clicked() {
        checked = !checked;

        if (changeListener != null) {
            changeListener.accept(checked);
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY) {
        var indicator = checked ? AllGuiTextures.INDICATOR_GREEN : AllGuiTextures.INDICATOR_RED;
        indicator.render(gui, x, y - indicator.height);

        super.render(gui, mouseX, mouseY);
    }
}
