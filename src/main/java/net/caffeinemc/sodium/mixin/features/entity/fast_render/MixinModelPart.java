package net.caffeinemc.sodium.mixin.features.entity.fast_render;

import net.caffeinemc.sodium.interop.vanilla.math.matrix.Matrix3fUtil;
import net.caffeinemc.sodium.interop.vanilla.math.matrix.Matrix4fUtil;
import net.caffeinemc.sodium.interop.vanilla.mixin.ModelCuboidAccessor;
import net.caffeinemc.sodium.interop.vanilla.vertex.VanillaVertexFormats;
import net.caffeinemc.sodium.interop.vanilla.vertex.formats.ModelQuadVertexSink;
import net.caffeinemc.sodium.render.vertex.VertexDrain;
import net.caffeinemc.sodium.util.packed.ColorABGR;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3f;
import org.joml.Math;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(ModelPart.class)
public class MixinModelPart {
    private static final float NORM = 1.0F / 16.0F;

    @Shadow
    @Final
    private List<ModelPart.Cuboid> cuboids;

    @Shadow
    public float pitch;

    @Shadow
    public float yaw;

    @Shadow
    public float roll;

    @Shadow
    public float pivotX;

    @Shadow
    public float pivotY;

    @Shadow
    public float pivotZ;

    @Shadow
    public float xScale;

    @Shadow
    public float yScale;

    @Shadow
    public float zScale;

    /**
     * @author JellySquid
     * @reason Use optimized vertex writer, avoid allocations, use quick matrix transformations
     */
    @Overwrite
    private void renderCuboids(MatrixStack.Entry matrices, VertexConsumer vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha) {
        Matrix3f normalMatrix = matrices.getNormalMatrix();
        Matrix4f positionMatrix = matrices.getPositionMatrix();

        ModelQuadVertexSink drain = VertexDrain.of(vertexConsumer).createSink(VanillaVertexFormats.QUADS);
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

                    float x2 = Matrix4fUtil.transformVectorX(positionMatrix, x1, y1, z1);
                    float y2 = Matrix4fUtil.transformVectorY(positionMatrix, x1, y1, z1);
                    float z2 = Matrix4fUtil.transformVectorZ(positionMatrix, x1, y1, z1);

                    drain.writeQuad(x2, y2, z2, color, vertex.u, vertex.v, light, overlay, norm);
                }
            }
        }

        drain.flush();
    }

    /**
     * @author burgerdude
     * @reason Inline and combine all rotation matrix math, remove math that zeroes out, reduce allocations
     */
    @SuppressWarnings("DuplicatedCode")
    @Overwrite
    public void rotate(MatrixStack matrices) {
        MatrixStack.Entry currentStackEntry = matrices.peek();

        Matrix4f modelMat = currentStackEntry.getPositionMatrix();

        modelMat.multiplyByTranslation(this.pivotX * NORM, this.pivotY * NORM, this.pivotZ * NORM);

        float sx = MathHelper.sin(this.pitch);
        float cx = MathHelper.cos(this.pitch);
        float sy = MathHelper.sin(this.yaw);
        float cy = MathHelper.cos(this.yaw);
        float sz = MathHelper.sin(this.roll);
        float cz = MathHelper.cos(this.roll);

        // create 3-axis combined rotation matrix, individual entries are stored here (that weren't 0s)
        float rot00 = cy * cz;
        float rot01 = (sx * sy * cz) - (cx * sz);
        float rot02 = (cx * sy * cz) + (sx * sz);
        float rot10 = cy * sz;
        float rot11 = (sx * sy * sz) + (cx * cz);
        float rot12 = (cx * sy * sz) - (sx * cz);
        float rot20 = -sy;
        float rot21 = sx * cy;
        float rot22 = cx * cy;

        // multiply components (that don't result in an equivalent value) individually. pray for autovectorization.
        // if JOML's FMA mode is enabled, it will use FMA, otherwise it will do the typical floating point operation
        float newModel00 = Math.fma(modelMat.a00, rot00, Math.fma(modelMat.a01, rot10, modelMat.a02 * rot20));
        float newModel01 = Math.fma(modelMat.a00, rot01, Math.fma(modelMat.a01, rot11, modelMat.a02 * rot21));
        float newModel02 = Math.fma(modelMat.a00, rot02, Math.fma(modelMat.a01, rot12, modelMat.a02 * rot22));
        float newModel10 = Math.fma(modelMat.a10, rot00, Math.fma(modelMat.a11, rot10, modelMat.a12 * rot20));
        float newModel11 = Math.fma(modelMat.a10, rot01, Math.fma(modelMat.a11, rot11, modelMat.a12 * rot21));
        float newModel12 = Math.fma(modelMat.a10, rot02, Math.fma(modelMat.a11, rot12, modelMat.a12 * rot22));
        float newModel20 = Math.fma(modelMat.a20, rot00, Math.fma(modelMat.a21, rot10, modelMat.a22 * rot20));
        float newModel21 = Math.fma(modelMat.a20, rot01, Math.fma(modelMat.a21, rot11, modelMat.a22 * rot21));
        float newModel22 = Math.fma(modelMat.a20, rot02, Math.fma(modelMat.a21, rot12, modelMat.a22 * rot22));
        float newModel30 = Math.fma(modelMat.a30, rot00, Math.fma(modelMat.a31, rot10, modelMat.a32 * rot20));
        float newModel31 = Math.fma(modelMat.a30, rot01, Math.fma(modelMat.a31, rot11, modelMat.a32 * rot21));
        float newModel32 = Math.fma(modelMat.a30, rot02, Math.fma(modelMat.a31, rot12, modelMat.a32 * rot22));

        modelMat.a00 = newModel00;
        modelMat.a01 = newModel01;
        modelMat.a02 = newModel02;
        modelMat.a10 = newModel10;
        modelMat.a11 = newModel11;
        modelMat.a12 = newModel12;
        modelMat.a20 = newModel20;
        modelMat.a21 = newModel21;
        modelMat.a22 = newModel22;
        modelMat.a30 = newModel30;
        modelMat.a31 = newModel31;
        modelMat.a32 = newModel32;

        Matrix3f normalMatEx = currentStackEntry.getNormalMatrix();

        // multiply all components and pray for autovectorization
        // if JOML's FMA mode is enabled, it will use FMA, otherwise it will do the typical floating point operation
        float newNormal00 = Math.fma(normalMatEx.a00, rot00, Math.fma(normalMatEx.a01, rot10, normalMatEx.a02 * rot20));
        float newNormal01 = Math.fma(normalMatEx.a00, rot01, Math.fma(normalMatEx.a01, rot11, normalMatEx.a02 * rot21));
        float newNormal02 = Math.fma(normalMatEx.a00, rot02, Math.fma(normalMatEx.a01, rot12, normalMatEx.a02 * rot22));
        float newNormal10 = Math.fma(normalMatEx.a10, rot00, Math.fma(normalMatEx.a11, rot10, normalMatEx.a12 * rot20));
        float newNormal11 = Math.fma(normalMatEx.a10, rot01, Math.fma(normalMatEx.a11, rot11, normalMatEx.a12 * rot21));
        float newNormal12 = Math.fma(normalMatEx.a10, rot02, Math.fma(normalMatEx.a11, rot12, normalMatEx.a12 * rot22));
        float newNormal20 = Math.fma(normalMatEx.a20, rot00, Math.fma(normalMatEx.a21, rot10, normalMatEx.a22 * rot20));
        float newNormal21 = Math.fma(normalMatEx.a20, rot01, Math.fma(normalMatEx.a21, rot11, normalMatEx.a22 * rot21));
        float newNormal22 = Math.fma(normalMatEx.a20, rot02, Math.fma(normalMatEx.a21, rot12, normalMatEx.a22 * rot22));

        normalMatEx.a00 = newNormal00;
        normalMatEx.a01 = newNormal01;
        normalMatEx.a02 = newNormal02;
        normalMatEx.a10 = newNormal10;
        normalMatEx.a11 = newNormal11;
        normalMatEx.a12 = newNormal12;
        normalMatEx.a20 = newNormal20;
        normalMatEx.a21 = newNormal21;
        normalMatEx.a22 = newNormal22;

        if (this.xScale != 1.0F || this.yScale != 1.0F || this.zScale != 1.0F) {
            matrices.scale(this.xScale, this.yScale, this.zScale);
        }
    }
}
