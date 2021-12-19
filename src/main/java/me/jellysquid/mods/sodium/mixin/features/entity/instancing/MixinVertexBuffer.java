package me.jellysquid.mods.sodium.mixin.features.entity.instancing;

import me.jellysquid.mods.sodium.interop.vanilla.buffer.VertexBufferAccessor;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(VertexBuffer.class)
public abstract class MixinVertexBuffer implements VertexBufferAccessor {
    @Shadow
    private int vertexCount;

    @Shadow
    private VertexFormat.DrawMode drawMode;

    @Shadow
    private VertexFormat.IntType vertexFormat;

    @Shadow
    private int vertexBufferId;

    @Shadow
    protected abstract void bindVertexArray();

    @Shadow
    public abstract void bind();

    @Override
    public int getIndexCount() {
        return vertexCount;
    }

    @Override
    public VertexFormat.DrawMode getDrawMode() {
        return drawMode;
    }

    @Override
    public VertexFormat.IntType getIndexType() {
        return vertexFormat;
    }

    @Override
    public int getVertexBufferId() {
        return vertexBufferId;
    }

    @Override
    public void invokeBindVertexArray() {
        bindVertexArray();
    }

    @Override
    public void invokeBind() {
        bind();
    }
}
