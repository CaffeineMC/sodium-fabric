package me.jellysquid.mods.sodium.mixin.features.entity.instancing;

import java.nio.ByteBuffer;

import me.jellysquid.mods.sodium.interop.vanilla.consumer.BufferBuilderAccessor;
import me.jellysquid.mods.sodium.interop.vanilla.layer.BufferBuilderExtended;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BufferBuilder.class)
public class MixinBufferBuilder implements BufferBuilderExtended, BufferBuilderAccessor {
    @Shadow
    private ByteBuffer buffer;
    @Shadow
    private int elementOffset;
    @Shadow
    private int vertexCount;
    private RenderLayer renderLayer;

    @Override
    public RenderLayer getRenderLayer() {
        return renderLayer;
    }

    @Override
    public void setRenderLayer(RenderLayer renderLayer) {
        this.renderLayer = renderLayer;
    }

    @Override
    public ByteBuffer getBuffer() {
        return buffer;
    }

    @Override
    public int getElementOffset() {
        return elementOffset;
    }

    @Override
    public int getVertexCount() {
        return vertexCount;
    }
}
