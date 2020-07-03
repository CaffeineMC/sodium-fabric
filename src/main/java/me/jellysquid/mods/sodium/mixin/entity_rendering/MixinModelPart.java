package me.jellysquid.mods.sodium.mixin.entity_rendering;

import it.unimi.dsi.fastutil.objects.ObjectList;
import me.jellysquid.mods.sodium.client.model.ModelPartCuboidExtended;
import me.jellysquid.mods.sodium.client.model.ModelPartQuadExtended;
import me.jellysquid.mods.sodium.client.model.QuadVertexConsumer;
import me.jellysquid.mods.sodium.client.util.ColorARGB;
import me.jellysquid.mods.sodium.client.util.Norm3b;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelPart.class)
public class MixinModelPart {
    @Shadow
    @Final
    private ObjectList<ModelPart.Cuboid> cuboids;

    private final Vector4f posVec = new Vector4f();
    private final Vector3f normVec = new Vector3f();

    /**
     * @author JellySquid
     * @reason Use optimized vertex writer
     */
    @Overwrite
    private void renderCuboids(MatrixStack.Entry matrices, VertexConsumer vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha) {
        QuadVertexConsumer vertices = (QuadVertexConsumer) vertexConsumer;
        Matrix4f modelMatrix = matrices.getModel();
        Matrix3f normalMatrix = matrices.getNormal();

        int color = ColorARGB.pack(red, green, blue, alpha);

        final Vector3f normVec1 = this.normVec;
        final Vector4f posVec1 = this.posVec;

        for (ModelPart.Cuboid cuboid : this.cuboids) {
            for (ModelPart.Quad quad : ((ModelPartCuboidExtended) cuboid).getQuads()) {
                Vector3f dir = quad.direction;
                normVec1.set(dir.getX(), dir.getY(), dir.getZ());
                normVec1.transform(normalMatrix);

                final float[] data = ((ModelPartQuadExtended) quad).getFlattenedData();

                int i = 0;

                while (i < data.length) {
                    float x = data[i++];
                    float y = data[i++];
                    float z = data[i++];

                    float u = data[i++];
                    float v = data[i++];

                    posVec1.set(x, y, z, 1.0f);
                    posVec1.transform(modelMatrix);

                    vertices.vertex(posVec1.getX(), posVec1.getY(), posVec1.getZ(), color, u, v, overlay, light, Norm3b.pack(normVec1));
                }
            }
        }
    }

    @Mixin(ModelPart.Quad.class)
    private static class MixinQuad implements ModelPartQuadExtended {
        private float[] data;

        @Inject(method = "<init>", at = @At("RETURN"))
        private void init(ModelPart.Vertex[] vertices, float float_1, float float_2, float float_3, float float_4, float float_5, float float_6, boolean boolean_1, Direction direction_1, CallbackInfo ci) {
            this.data = new float[vertices.length * 5];

            for (int i = 0; i < vertices.length; i++) {
                ModelPart.Vertex vertex = vertices[i];

                int j = i * 5;

                this.data[j] = vertex.pos.getX() / 16.0F;
                this.data[j + 1] = vertex.pos.getY() / 16.0F;
                this.data[j + 2] = vertex.pos.getZ() / 16.0F;

                this.data[j + 3] = vertex.u;
                this.data[j + 4] = vertex.v;
            }
        }

        @Override
        public float[] getFlattenedData() {
            return this.data;
        }
    }
}
