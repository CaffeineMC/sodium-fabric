package me.jellysquid.mods.sodium.mixin.features.buffer_builder.intrinsics;

import me.jellysquid.mods.sodium.client.render.vertex.VertexBufferWriter;
import me.jellysquid.mods.sodium.client.render.vertex.formats.LineVertex;
import me.jellysquid.mods.sodium.client.render.vertex.formats.ModelVertex;
import me.jellysquid.mods.sodium.client.util.Norm3b;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderDispatcher {
    private static final int SHADOW_COLOR = ColorABGR.pack(1.0f, 1.0f, 1.0f);
    private static final int SHADOW_NORMAL = Norm3b.pack(0.0f, 1.0f, 0.0f);

    /**
     * @author JellySquid
     * @reason Use intrinsics
     */
    @Overwrite
    private static void drawShadowVertex(MatrixStack.Entry entry, VertexConsumer vertices, float alpha, float x, float y, float z, float u, float v) {
        var writer = VertexBufferWriter.of(vertices);

        try (MemoryStack stack = VertexBufferWriter.STACK.push()) {
            long buffer = writer.buffer(stack, 1, ModelVertex.STRIDE, ModelVertex.FORMAT);
            ModelVertex.write(buffer, x, y, z, ColorABGR.withAlpha(SHADOW_COLOR, alpha), u, v, OverlayTexture.DEFAULT_UV, LightmapTextureManager.MAX_LIGHT_COORDINATE, SHADOW_NORMAL);
            writer.push(buffer, 1, ModelVertex.STRIDE, ModelVertex.FORMAT);
        }
    }

}
