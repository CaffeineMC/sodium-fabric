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

package me.jellysquid.mods.sodium.interop.fabric.material;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.minecraft.util.math.MathHelper;

/**
 * Default implementation of the standard render materials.
 * The underlying representation is simply an int with bit-wise
 * packing of the various material properties. This offers
 * easy/fast interning via int/object hashmap.
 */
public abstract class RenderMaterialFlags {
    public static final BlendMode[] BLEND_MODES = BlendMode.values();

    public static final int BLEND_MODE_MASK = MathHelper.smallestEncompassingPowerOfTwo(BLEND_MODES.length) - 1;

    public static final int COLOR_DISABLE_FLAG = BLEND_MODE_MASK + 1;
    public static final int EMISSIVE_FLAG = COLOR_DISABLE_FLAG << 1;
    public static final int DIFFUSE_FLAG = EMISSIVE_FLAG << 1;
    public static final int AO_FLAG = DIFFUSE_FLAG << 1;
    public static final int VALUE_COUNT = (AO_FLAG << 1);

    protected int bits;

    public BlendMode blendMode(int textureIndex) {
        return BLEND_MODES[this.bits & BLEND_MODE_MASK];
    }

    public boolean disableColorIndex(int textureIndex) {
        return (this.bits & COLOR_DISABLE_FLAG) != 0;
    }

    public int spriteDepth() {
        return 1;
    }

    public boolean emissive(int textureIndex) {
        return (this.bits & EMISSIVE_FLAG) != 0;
    }

    public boolean disableDiffuse(int textureIndex) {
        return (this.bits & DIFFUSE_FLAG) != 0;
    }

    public boolean disableAo(int textureIndex) {
        return (this.bits & AO_FLAG) != 0;
    }

}
