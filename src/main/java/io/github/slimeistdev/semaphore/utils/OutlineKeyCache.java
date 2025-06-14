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

import java.util.ArrayList;
import java.util.List;

public class OutlineKeyCache {
    private final List<Object> keys = new ArrayList<>();

    public Object getKey(int i) {
        while (keys.size() <= i) {
            keys.add(new Object());
        }
        return keys.get(i);
    }

    public static class Sequential extends OutlineKeyCache {
        private int nextKey = 0;

        public void resetCounter() {
            nextKey = 0;
        }

        public Object getNextKey() {
            return super.getKey(nextKey++);
        }
    }
}
