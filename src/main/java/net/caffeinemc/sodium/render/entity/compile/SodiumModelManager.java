package net.caffeinemc.sodium.render.entity.compile;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.nio.ByteBuffer;
import java.util.Map;

import net.caffeinemc.sodium.SodiumClientMod;
import net.caffeinemc.sodium.interop.vanilla.mixin.ModelManagerHolder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;

public class SodiumModelManager {
    private static final int INITIAL_BUILDER_CAPACITY = 32768; // should be enough i think
    // we have to allocate this only once because there is no way to free them.
    private static final BufferBuilder MODEL_BUFFER_BUILDER = new BufferBuilder(INITIAL_BUILDER_CAPACITY);

    private final Object2ObjectMap<EntityModelLayer, BuiltEntityModel> builtModelMap;

    public SodiumModelManager() {
        this.builtModelMap = new Object2ObjectOpenHashMap<>();
    }

    public static SodiumModelManager instance() {
        return ((ModelManagerHolder) MinecraftClient.getInstance().getEntityModelLoader()).getSodiumEntityModelManager();
    }

    public void buildModels(Map<EntityModelLayer, TexturedModelData> modelDataMap) {
        BufferBuilder bufferBuilder = MODEL_BUFFER_BUILDER;
        for (Map.Entry<EntityModelLayer, TexturedModelData> entry : modelDataMap.entrySet()) {
            EntityModelLayer layer = entry.getKey();
            ModelPart modelPart = entry.getValue().createModel();

            MatrixStack emptyMatrixStack = new MatrixStack();
            // to stay compatible with most mods, we use the vanilla model builder methods.
            try {
                // FIXME: use actual values for these?
                modelPart.render(
                        emptyMatrixStack,
                        bufferBuilder,
                        LightmapTextureManager.MAX_LIGHT_COORDINATE,
                        OverlayTexture.DEFAULT_UV,
                        1.0f,
                        1.0f,
                        1.0f,
                        1.0f
                );
            } catch (Exception e) {
                SodiumClientMod.logger().warn("Unable to build base model for entity layer " + layer);
            }
        }
        bufferBuilder.reset();
    }
}
