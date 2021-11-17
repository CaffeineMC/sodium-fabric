package me.jellysquid.mods.sodium.render.entity;

import me.jellysquid.mods.sodium.interop.vanilla.consumer.SmartBufferBuilderWrapper;
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
    public static final SmartBufferBuilderWrapper VBO_BUFFER_BUILDER = new SmartBufferBuilderWrapper(new BufferBuilder(32768), 256); // just some random sizes lol
    public static final GlSsboRenderDispatcher INSTANCED_RENDER_DISPATCHER = new GlSsboRenderDispatcher();
    public static final BakingData bakingData = new BakingData(INSTANCED_RENDER_DISPATCHER.modelPersistentSsbo, INSTANCED_RENDER_DISPATCHER.partPersistentSsbo, INSTANCED_RENDER_DISPATCHER.translucencyPersistentEbo);

    public static VertexConsumer getNestedBufferBuilder(VertexConsumer consumer) { // TODO: add more possibilities with this method, ex outline consumers
        return consumer instanceof SpriteTexturedVertexConsumer ?
                (BufferBuilder) ((SpriteTexturedVertexConsumerAccessor) consumer).getParent() :
                consumer;
    }
}
