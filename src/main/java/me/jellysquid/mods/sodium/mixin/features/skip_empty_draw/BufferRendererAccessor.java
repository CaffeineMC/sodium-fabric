package me.jellysquid.mods.sodium.mixin.features.skip_empty_draw;

import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.nio.ByteBuffer;

@Mixin(BufferRenderer.class)
public interface BufferRendererAccessor {
    @Invoker("draw")
    static void drawInternal(ByteBuffer buffer, VertexFormat.DrawMode drawMode, VertexFormat vertexFormat, int count, VertexFormat.IntType elementFormat, int vertexCount, boolean textured) {
        throw new RuntimeException("how");
    }
}
