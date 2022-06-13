package net.caffeinemc.sodium.mixin.features.entity.fast_render;

import net.caffeinemc.sodium.interop.vanilla.math.matrix.Matrix3fExtended;
import net.caffeinemc.sodium.interop.vanilla.math.matrix.Matrix4fExtended;
import net.caffeinemc.sodium.interop.vanilla.math.matrix.MatrixUtil;
import net.caffeinemc.sodium.interop.vanilla.mixin.ModelCuboidAccessor;
import net.caffeinemc.sodium.interop.vanilla.vertex.VanillaVertexFormats;
import net.caffeinemc.sodium.interop.vanilla.vertex.formats.quad.QuadVertexSink;
import net.caffeinemc.sodium.render.vertex.VertexDrain;
import net.caffeinemc.sodium.util.packed.ColorABGR;
import net.caffeinemc.sodium.util.packed.Normal3b;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3f;
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
        Matrix3fExtended normalExt = MatrixUtil.getExtendedMatrix(matrices.getNormalMatrix());
        Matrix4fExtended positionExt = MatrixUtil.getExtendedMatrix(matrices.getPositionMatrix());

        QuadVertexSink drain = VertexDrain.of(vertexConsumer).createSink(VanillaVertexFormats.QUADS);
        drain.ensureCapacity(this.cuboids.size() * 6 * 4);

        int color = ColorABGR.pack(red, green, blue, alpha);

        for (ModelPart.Cuboid cuboid : this.cuboids) {
            for (ModelPart.Quad quad : ((ModelCuboidAccessor) cuboid).getQuads()) {
                float normX = normalExt.transformVecX(quad.direction);
                float normY = normalExt.transformVecY(quad.direction);
                float normZ = normalExt.transformVecZ(quad.direction);

                int norm = Normal3b.pack(normX, normY, normZ);

                for (ModelPart.Vertex vertex : quad.vertices) {
                    Vec3f pos = vertex.pos;

                    float x1 = pos.getX() * NORM;
                    float y1 = pos.getY() * NORM;
                    float z1 = pos.getZ() * NORM;

                    float x2 = positionExt.transformVecX(x1, y1, z1);
                    float y2 = positionExt.transformVecY(x1, y1, z1);
                    float z2 = positionExt.transformVecZ(x1, y1, z1);

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
        matrices.translate(this.pivotX * NORM, this.pivotY * NORM, this.pivotZ * NORM);

        MatrixStack.Entry currentStackEntry = matrices.peek();

        Matrix4fExtended modelMat = MatrixUtil.getExtendedMatrix(currentStackEntry.getPositionMatrix());

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
        float newModel00 = modelMat.getA00() * rot00 + modelMat.getA01() * rot10 + modelMat.getA02() * rot20;
        float newModel01 = modelMat.getA00() * rot01 + modelMat.getA01() * rot11 + modelMat.getA02() * rot21;
        float newModel02 = modelMat.getA00() * rot02 + modelMat.getA01() * rot12 + modelMat.getA02() * rot22;
        float newModel10 = modelMat.getA10() * rot00 + modelMat.getA11() * rot10 + modelMat.getA12() * rot20;
        float newModel11 = modelMat.getA10() * rot01 + modelMat.getA11() * rot11 + modelMat.getA12() * rot21;
        float newModel12 = modelMat.getA10() * rot02 + modelMat.getA11() * rot12 + modelMat.getA12() * rot22;
        float newModel20 = modelMat.getA20() * rot00 + modelMat.getA21() * rot10 + modelMat.getA22() * rot20;
        float newModel21 = modelMat.getA20() * rot01 + modelMat.getA21() * rot11 + modelMat.getA22() * rot21;
        float newModel22 = modelMat.getA20() * rot02 + modelMat.getA21() * rot12 + modelMat.getA22() * rot22;
        float newModel30 = modelMat.getA30() * rot00 + modelMat.getA31() * rot10 + modelMat.getA32() * rot20;
        float newModel31 = modelMat.getA30() * rot01 + modelMat.getA31() * rot11 + modelMat.getA32() * rot21;
        float newModel32 = modelMat.getA30() * rot02 + modelMat.getA31() * rot12 + modelMat.getA32() * rot22;
//        float newModel00 = Math.fma(modelMat.getA00(), rot00, Math.fma(modelMat.getA01(), rot10, modelMat.getA02() * rot20));
//        float newModel01 = Math.fma(modelMat.getA00(), rot01, Math.fma(modelMat.getA01(), rot11, modelMat.getA02() * rot21));
//        float newModel02 = Math.fma(modelMat.getA00(), rot02, Math.fma(modelMat.getA01(), rot12, modelMat.getA02() * rot22));
//        float newModel10 = Math.fma(modelMat.getA10(), rot00, Math.fma(modelMat.getA11(), rot10, modelMat.getA12() * rot20));
//        float newModel11 = Math.fma(modelMat.getA10(), rot01, Math.fma(modelMat.getA11(), rot11, modelMat.getA12() * rot21));
//        float newModel12 = Math.fma(modelMat.getA10(), rot02, Math.fma(modelMat.getA11(), rot12, modelMat.getA12() * rot22));
//        float newModel20 = Math.fma(modelMat.getA20(), rot00, Math.fma(modelMat.getA21(), rot10, modelMat.getA22() * rot20));
//        float newModel21 = Math.fma(modelMat.getA20(), rot01, Math.fma(modelMat.getA21(), rot11, modelMat.getA22() * rot21));
//        float newModel22 = Math.fma(modelMat.getA20(), rot02, Math.fma(modelMat.getA21(), rot12, modelMat.getA22() * rot22));
//        float newModel30 = Math.fma(modelMat.getA30(), rot00, Math.fma(modelMat.getA31(), rot10, modelMat.getA32() * rot20));
//        float newModel31 = Math.fma(modelMat.getA30(), rot01, Math.fma(modelMat.getA31(), rot11, modelMat.getA32() * rot21));
//        float newModel32 = Math.fma(modelMat.getA30(), rot02, Math.fma(modelMat.getA31(), rot12, modelMat.getA32() * rot22));

        modelMat.setA00(newModel00);
        modelMat.setA01(newModel01);
        modelMat.setA02(newModel02);
        modelMat.setA10(newModel10);
        modelMat.setA11(newModel11);
        modelMat.setA12(newModel12);
        modelMat.setA20(newModel20);
        modelMat.setA21(newModel21);
        modelMat.setA22(newModel22);
        modelMat.setA30(newModel30);
        modelMat.setA31(newModel31);
        modelMat.setA32(newModel32);

        Matrix3fExtended normalMat = MatrixUtil.getExtendedMatrix(currentStackEntry.getNormalMatrix());

        // multiply all components and pray for autovectorization
        float newNormal00 = normalMat.getA00() * rot00 + normalMat.getA01() * rot10 + normalMat.getA02() * rot20;
        float newNormal01 = normalMat.getA00() * rot01 + normalMat.getA01() * rot11 + normalMat.getA02() * rot21;
        float newNormal02 = normalMat.getA00() * rot02 + normalMat.getA01() * rot12 + normalMat.getA02() * rot22;
        float newNormal10 = normalMat.getA10() * rot00 + normalMat.getA11() * rot10 + normalMat.getA12() * rot20;
        float newNormal11 = normalMat.getA10() * rot01 + normalMat.getA11() * rot11 + normalMat.getA12() * rot21;
        float newNormal12 = normalMat.getA10() * rot02 + normalMat.getA11() * rot12 + normalMat.getA12() * rot22;
        float newNormal20 = normalMat.getA20() * rot00 + normalMat.getA21() * rot10 + normalMat.getA22() * rot20;
        float newNormal21 = normalMat.getA20() * rot01 + normalMat.getA21() * rot11 + normalMat.getA22() * rot21;
        float newNormal22 = normalMat.getA20() * rot02 + normalMat.getA21() * rot12 + normalMat.getA22() * rot22;
//        float newNormal00 = Math.fma(normalMat.getA00(), rot00, Math.fma(normalMat.getA01(), rot10, normalMat.getA02() * rot20));
//        float newNormal01 = Math.fma(normalMat.getA00(), rot01, Math.fma(normalMat.getA01(), rot11, normalMat.getA02() * rot21));
//        float newNormal02 = Math.fma(normalMat.getA00(), rot02, Math.fma(normalMat.getA01(), rot12, normalMat.getA02() * rot22));
//        float newNormal10 = Math.fma(normalMat.getA10(), rot00, Math.fma(normalMat.getA11(), rot10, normalMat.getA12() * rot20));
//        float newNormal11 = Math.fma(normalMat.getA10(), rot01, Math.fma(normalMat.getA11(), rot11, normalMat.getA12() * rot21));
//        float newNormal12 = Math.fma(normalMat.getA10(), rot02, Math.fma(normalMat.getA11(), rot12, normalMat.getA12() * rot22));
//        float newNormal20 = Math.fma(normalMat.getA20(), rot00, Math.fma(normalMat.getA21(), rot10, normalMat.getA22() * rot20));
//        float newNormal21 = Math.fma(normalMat.getA20(), rot01, Math.fma(normalMat.getA21(), rot11, normalMat.getA22() * rot21));
//        float newNormal22 = Math.fma(normalMat.getA20(), rot02, Math.fma(normalMat.getA21(), rot12, normalMat.getA22() * rot22));

        normalMat.setA00(newNormal00);
        normalMat.setA01(newNormal01);
        normalMat.setA02(newNormal02);
        normalMat.setA10(newNormal10);
        normalMat.setA11(newNormal11);
        normalMat.setA12(newNormal12);
        normalMat.setA20(newNormal20);
        normalMat.setA21(newNormal21);
        normalMat.setA22(newNormal22);

        if (this.xScale != 1.0F || this.yScale != 1.0F || this.zScale != 1.0F) {
            matrices.scale(this.xScale, this.yScale, this.zScale);
        }
    }
}
