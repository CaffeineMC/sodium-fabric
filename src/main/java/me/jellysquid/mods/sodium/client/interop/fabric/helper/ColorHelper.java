/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.jellysquid.mods.sodium.client.interop.fabric.helper;

/**
 * Static routines of general utility for renderer implementations.
 * Renderers are not required to use these helpers, but they were
 * designed to be usable without the default renderer.
 */
public abstract class ColorHelper {
	private ColorHelper() { }

    /**
	 * Component-wise max.
	 */
	public static int maxBrightness(int b0, int b1) {
		if (b0 == 0) return b1;
		if (b1 == 0) return b0;

		return Math.max(b0 & 0x0000FFFF, b1 & 0x0000FFFF) |
                Math.max(b0 & 0xFFFF0000, b1 & 0xFFFF0000);
	}
}
