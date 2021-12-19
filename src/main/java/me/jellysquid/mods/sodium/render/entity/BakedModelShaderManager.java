package me.jellysquid.mods.sodium.render.entity;

import java.io.IOException;

import net.minecraft.client.render.Shader;
import net.minecraft.resource.ResourceManager;
import org.jetbrains.annotations.Nullable;

public class BakedModelShaderManager {
    public static @Nullable Shader ENTITY_CUTOUT_NO_CULL_INSTANCED;
    public static @Nullable Shader ENTITY_TRANSLUCENT_BATCHED;

    public static void loadShaders(ResourceManager manager) {
        try {
            ENTITY_CUTOUT_NO_CULL_INSTANCED = new Shader(manager, "rendertype_entity_cutout_no_cull_instanced", BakedModelVertexFormats.SMART_ENTITY_FORMAT);
            ENTITY_TRANSLUCENT_BATCHED = new Shader(manager, "rendertype_entity_translucent_batched", BakedModelVertexFormats.SMART_ENTITY_FORMAT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
