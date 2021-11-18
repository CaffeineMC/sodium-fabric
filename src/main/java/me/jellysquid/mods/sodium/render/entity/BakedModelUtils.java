package me.jellysquid.mods.sodium.render.entity;

import me.jellysquid.mods.sodium.interop.vanilla.consumer.ModelVboBufferBuilder;
import me.jellysquid.mods.sodium.interop.vanilla.consumer.SpriteTexturedVertexConsumerAccessor;
import me.jellysquid.mods.sodium.render.entity.data.BakingData;
import me.jellysquid.mods.sodium.render.entity.renderer.GlSsboRenderDispatcher;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.SpriteTexturedVertexConsumer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

public class BakedModelUtils {

    public static final long MODEL_STRUCT_SIZE = 4 * Float.BYTES + 2 * Integer.BYTES + 2 * Integer.BYTES + 3 * Float.BYTES + Integer.BYTES;
    public static final long PART_STRUCT_SIZE = 16 * Float.BYTES + 12 * Float.BYTES;

    public static final MatrixStack.Entry IDENTITY_STACK_ENTRY = new MatrixStack().peek();
    // FIXME: not thread safe, but making one per instance is slow
    private static ModelVboBufferBuilder MODEL_VBO_BUFFER_BUILDER;
    private static GlSsboRenderDispatcher INSTANCED_RENDER_DISPATCHER;
    private static BakingData BAKING_DATA;

    public static ModelVboBufferBuilder getModelVboBufferBuilder() {
        if (MODEL_VBO_BUFFER_BUILDER == null) {
            MODEL_VBO_BUFFER_BUILDER = new ModelVboBufferBuilder(new BufferBuilder(32768), 256); // just some random sizes lol
        }
        return MODEL_VBO_BUFFER_BUILDER;
    }

    public static GlSsboRenderDispatcher getInstancedRenderDispatcher() {
        if (INSTANCED_RENDER_DISPATCHER == null) {
            INSTANCED_RENDER_DISPATCHER = new GlSsboRenderDispatcher();
        }
        return INSTANCED_RENDER_DISPATCHER;
    }

    public static BakingData getBakingData() {
        if (BAKING_DATA == null) {
            GlSsboRenderDispatcher instancedRenderDispatcher = getInstancedRenderDispatcher();
            BAKING_DATA = new BakingData(instancedRenderDispatcher.modelPersistentSsbo, instancedRenderDispatcher.partPersistentSsbo, instancedRenderDispatcher.translucencyPersistentEbo);
        }
        return BAKING_DATA;
    }

    public static void close() {
        if (BAKING_DATA != null) BAKING_DATA.close();
        if (INSTANCED_RENDER_DISPATCHER != null) INSTANCED_RENDER_DISPATCHER.close();
    }

    public static VertexConsumer getNestedBufferBuilder(VertexConsumer consumer) { // TODO: add more possibilities with this method, ex outline consumers
        return consumer instanceof SpriteTexturedVertexConsumer ?
                (BufferBuilder) ((SpriteTexturedVertexConsumerAccessor) consumer).getParent() :
                consumer;
    }
}
