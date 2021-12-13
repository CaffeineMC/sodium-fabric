package me.jellysquid.mods.sodium.mixin.features.entity.fast_render;

import me.jellysquid.mods.sodium.SodiumClient;
import me.jellysquid.mods.sodium.SodiumRender;
import me.jellysquid.mods.sodium.interop.vanilla.cuboid.ModelCuboidAccessor;
import me.jellysquid.mods.sodium.interop.vanilla.matrix.Matrix3fUtil;
import me.jellysquid.mods.sodium.interop.vanilla.matrix.Matrix4fUtil;
import me.jellysquid.mods.sodium.model.vertex.VanillaVertexTypes;
import me.jellysquid.mods.sodium.model.vertex.VertexDrain;
import me.jellysquid.mods.sodium.model.vertex.formats.ModelQuadVertexSink;
import me.jellysquid.mods.sodium.render.entity.renderer.InstancedEntityRenderer;
import me.jellysquid.mods.sodium.util.color.ColorABGR;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ModelPart.class)
public class MixinModelPart {
    private static final float NORM = 1.0F / 16.0F;

    @Shadow
    @Final
    private List<ModelPart.Cuboid> cuboids;

    /**
     * @author JellySquid
     * @reason Use optimized vertex writer, avoid allocations, use quick matrix transformations
     */
    @Inject(method = "renderCuboids", at = @At("HEAD"), cancellable = true)
    private void renderCuboids(MatrixStack.Entry entry, VertexConsumer vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha, CallbackInfo ci) {
        // skip if instancing is enabled
        if (!SodiumClient.options().performance.useModelInstancing || !InstancedEntityRenderer.isSupported(SodiumRender.DEVICE)) {
            Matrix3f normalMatrix = entry.getNormal();
            Matrix4f modelMatrix = entry.getModel();

            ModelQuadVertexSink drain = VertexDrain.of(vertexConsumer).createSink(VanillaVertexTypes.QUADS);
            drain.ensureCapacity(this.cuboids.size() * 6 * 4);

            int color = ColorABGR.pack(red, green, blue, alpha);

            for (ModelPart.Cuboid cuboid : this.cuboids) {
                for (ModelPart.Quad quad : ((ModelCuboidAccessor) cuboid).getQuads()) {
                    int norm = Matrix3fUtil.transformNormal(normalMatrix, quad.direction);

                    for (ModelPart.Vertex vertex : quad.vertices) {
                        Vec3f pos = vertex.pos;

                        float x1 = pos.getX() * NORM;
                        float y1 = pos.getY() * NORM;
                        float z1 = pos.getZ() * NORM;

                        float x2 = Matrix4fUtil.transformVectorX(modelMatrix, x1, y1, z1);
                        float y2 = Matrix4fUtil.transformVectorY(modelMatrix, x1, y1, z1);
                        float z2 = Matrix4fUtil.transformVectorZ(modelMatrix, x1, y1, z1);

                        drain.writeQuad(x2, y2, z2, color, vertex.u, vertex.v, light, overlay, norm);
                    }
                }
            }

            drain.flush();
            ci.cancel();
        }
    }
}
