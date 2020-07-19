package me.jellysquid.mods.sodium.mixin.features.entity.shadow_clipping;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderDispatcher {
    @Redirect(method = "drawShadowVertex", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexConsumer;vertex(Lnet/minecraft/util/math/Matrix4f;FFF)Lnet/minecraft/client/render/VertexConsumer;"))
    private static VertexConsumer preWriteVertex(VertexConsumer vertexConsumer, Matrix4f matrix, float x, float y, float z) {
        // FIX: Render the shadow slightly above the block to fix clipping issues
        // This happens in vanilla too, but is exacerbated by the Compact Vertex Format option.
        return vertexConsumer.vertex(matrix, x, y + 0.001f, z);
    }

}
