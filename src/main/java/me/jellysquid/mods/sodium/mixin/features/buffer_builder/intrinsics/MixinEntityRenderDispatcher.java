package me.jellysquid.mods.sodium.mixin.features.buffer_builder.intrinsics;

import me.jellysquid.mods.sodium.client.render.RenderGlobal;
import me.jellysquid.mods.sodium.client.render.vertex.VertexBufferWriter;
import me.jellysquid.mods.sodium.client.render.vertex.formats.ModelVertex;
import me.jellysquid.mods.sodium.client.util.Norm3b;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderDispatcher {
    private static final int SHADOW_COLOR = ColorABGR.pack(1.0f, 1.0f, 1.0f);
    private static final Vector3fc SHADOW_NORMAL = new Vector3f(0.0f, 1.0f, 0.0f);

    /**
     * @author JellySquid
     * @reason Use intrinsics
     */
    @Overwrite
    private static void drawShadowVertex(MatrixStack.Entry entry, VertexConsumer vertices, float alpha, float x, float y, float z, float u, float v) {
        drawOptimizedShadowVertex(entry, vertices, alpha, x, y, z, u, v);
    }

    private static void drawOptimizedShadowVertex(MatrixStack.Entry entry, VertexConsumer vertices, float alpha, float x, float y, float z, float u, float v) {
        var writer = VertexBufferWriter.of(vertices);

        var matNormal = entry.getNormalMatrix();
        var matPosition = entry.getPositionMatrix();

        float nx = SHADOW_NORMAL.x();
        float ny = SHADOW_NORMAL.y();
        float nz = SHADOW_NORMAL.z();

        // The transformed normal vector
        float nxt = (matNormal.m00() * nx) + (matNormal.m10() * ny) + (matNormal.m20() * nz);
        float nyt = (matNormal.m01() * nx) + (matNormal.m11() * ny) + (matNormal.m21() * nz);
        float nzt = (matNormal.m02() * nx) + (matNormal.m12() * ny) + (matNormal.m22() * nz);

        int norm = Norm3b.pack(nxt, nyt, nzt);

        try (MemoryStack stack = RenderGlobal.VERTEX_DATA.push()) {
            long buffer = stack.nmalloc(1 * ModelVertex.STRIDE);

            // The transformed position vector
            float xt = (matPosition.m00() * x) + (matPosition.m10() * y) + (matPosition.m20() * z) + matPosition.m30();
            float yt = (matPosition.m01() * x) + (matPosition.m11() * y) + (matPosition.m21() * z) + matPosition.m31();
            float zt = (matPosition.m02() * x) + (matPosition.m12() * y) + (matPosition.m22() * z) + matPosition.m32();

            ModelVertex.write(buffer, xt, yt, zt, ColorABGR.withAlpha(SHADOW_COLOR, alpha), u, v, OverlayTexture.DEFAULT_UV, LightmapTextureManager.MAX_LIGHT_COORDINATE, norm);

            writer.push(stack, buffer, 1, ModelVertex.FORMAT);
        }
    }
}
