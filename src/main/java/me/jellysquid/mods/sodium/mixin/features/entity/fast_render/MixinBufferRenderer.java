package me.jellysquid.mods.sodium.mixin.features.entity.fast_render;

import com.mojang.blaze3d.platform.GlStateManager;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormat;
import org.lwjgl.opengl.GL15;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.ByteBuffer;

@Mixin(BufferRenderer.class)
public class MixinBufferRenderer {
    private static GlMutableBuffer globalGpuBuffer;

    /**
     * @author JellySquid
     * @reason Avoid client side memory to speed up rendering
     */
    @Overwrite
    private static void draw(ByteBuffer buffer, int mode, VertexFormat vertexFormat, int count) {
        buffer.clear();

        if (count <= 0) {
            return;
        }

        if (globalGpuBuffer == null) {
            globalGpuBuffer = new GlMutableBuffer(GL15.GL_STREAM_DRAW);
        }

        GlMutableBuffer glBuffer = globalGpuBuffer;
        glBuffer.bind(GL15.GL_ARRAY_BUFFER);
        glBuffer.upload(GL15.GL_ARRAY_BUFFER, buffer);

        vertexFormat.startDrawing(0L);

        GlStateManager.drawArrays(mode, 0, count);

        vertexFormat.endDrawing();

        glBuffer.invalidate(GL15.GL_ARRAY_BUFFER);
        glBuffer.unbind(GL15.GL_ARRAY_BUFFER);
    }
}
