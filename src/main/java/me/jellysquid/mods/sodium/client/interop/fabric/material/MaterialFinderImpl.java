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

package me.jellysquid.mods.sodium.client.interop.fabric.material;

import com.google.common.base.Preconditions;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.MaterialFinder;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;

public class MaterialFinderImpl extends RenderMaterialFlags implements MaterialFinder {
    @Override
    public RenderMaterial find() {
        return RenderMaterialValue.byIndex(this.bits);
    }

    @Override
    public MaterialFinder clear() {
        this.bits = 0;
        return this;
    }

    @Override
    public MaterialFinder blendMode(int textureIndex, BlendMode blendMode) {
        if (blendMode == null) {
            blendMode = BlendMode.DEFAULT;
        }

        this.bits = (this.bits & ~BLEND_MODE_MASK) | blendMode.ordinal();
        return this;
    }

    @Override
    public MaterialFinder disableColorIndex(int textureIndex, boolean disable) {
        this.bits = disable ? (this.bits | COLOR_DISABLE_FLAG) : (this.bits & ~COLOR_DISABLE_FLAG);
        return this;
    }

    @Override
    public MaterialFinder spriteDepth(int depth) {
        Preconditions.checkArgument(depth == 1, "Unsupported sprite depth: %s", depth);

        return this;
    }

    @Override
    public MaterialFinder emissive(int textureIndex, boolean isEmissive) {
        this.bits = isEmissive ? (this.bits | EMISSIVE_FLAG) : (this.bits & ~EMISSIVE_FLAG);
        return this;
    }

    @Override
    public MaterialFinder disableDiffuse(int textureIndex, boolean disable) {
        this.bits = disable ? (this.bits | DIFFUSE_FLAG) : (this.bits & ~DIFFUSE_FLAG);
        return this;
    }

    @Override
    public MaterialFinder disableAo(int textureIndex, boolean disable) {
        this.bits = disable ? (this.bits | AO_FLAG) : (this.bits & ~AO_FLAG);
        return this;
    }
}
