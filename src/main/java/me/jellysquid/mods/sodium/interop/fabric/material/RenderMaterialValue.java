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

import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;

public class RenderMaterialValue extends RenderMaterialFlags implements RenderMaterial {
    private static final RenderMaterialValue[] VALUES = new RenderMaterialValue[VALUE_COUNT];

    static {
        for (int i = 0; i < VALUE_COUNT; i++) {
            VALUES[i] = new RenderMaterialValue(i);
        }
    }

    RenderMaterialValue(int bits) {
        this.bits = bits;
    }

    public int index() {
        return this.bits;
    }

    public static RenderMaterialValue byIndex(int index) {
        return VALUES[index];
    }
}
