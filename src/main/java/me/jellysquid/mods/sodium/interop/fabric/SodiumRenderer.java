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

package me.jellysquid.mods.sodium.interop.fabric;

import me.jellysquid.mods.sodium.interop.fabric.material.MaterialFinderImpl;
import me.jellysquid.mods.sodium.interop.fabric.material.RenderMaterialValue;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.material.MaterialFinder;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder;
import me.jellysquid.mods.sodium.interop.fabric.mesh.MeshBuilderImpl;
import net.minecraft.util.Identifier;

import java.util.HashMap;

/**
 * The Fabric default renderer implementation. Supports all
 * features defined in the API except shaders and offers no special materials.
 */
public class SodiumRenderer implements Renderer {
    public static final SodiumRenderer INSTANCE = new SodiumRenderer();

    public static final RenderMaterialValue MATERIAL_STANDARD = (RenderMaterialValue) INSTANCE.materialFinder().find();

    static {
        INSTANCE.registerMaterial(RenderMaterial.MATERIAL_STANDARD, MATERIAL_STANDARD);
    }

    private final HashMap<Identifier, RenderMaterial> materialMap = new HashMap<>();

    private SodiumRenderer() {
    }

    @Override
    public MeshBuilder meshBuilder() {
        return new MeshBuilderImpl();
    }

    @Override
    public MaterialFinder materialFinder() {
        return new MaterialFinderImpl();
    }

    @Override
    public RenderMaterial materialById(Identifier id) {
        return this.materialMap.get(id);
    }

    @Override
    public boolean registerMaterial(Identifier id, RenderMaterial material) {
        if (this.materialMap.containsKey(id)) {
            return false;
        }

        // cast to prevent acceptance of impostor implementations
        this.materialMap.put(id, material);

        return true;
    }
}
