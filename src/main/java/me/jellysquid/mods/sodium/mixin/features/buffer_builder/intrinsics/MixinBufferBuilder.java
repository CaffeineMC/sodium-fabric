package me.jellysquid.mods.sodium.mixin.features.buffer_builder.intrinsics;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.util.Norm3b;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import me.jellysquid.mods.sodium.client.util.color.ColorU8;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.FixedColorVertexConsumer;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;

import net.minecraft.util.math.Vec3i;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@SuppressWarnings({ "SameParameterValue" })
@Mixin(BufferBuilder.class)
public abstract class MixinBufferBuilder extends FixedColorVertexConsumer {
    @Shadow
    private boolean canSkipElementChecks;

//    @Override
//    public void quad(MatrixStack.Entry matrices, BakedQuad quad, float[] brightnessTable, float r, float g, float b, int[] light, int overlay, boolean colorize) {
//        if (!this.canSkipElementChecks) {
//            super.quad(matrices, quad, brightnessTable, r, g, b, light, overlay, colorize);
//
//            return;
//        }
//
//        if (this.colorFixed) {
//            throw new IllegalStateException();
//        }
//
//        ModelQuadView quadView = (ModelQuadView) quad;
//
//        Matrix4f positionMatrix = matrices.getPositionMatrix();
//        Matrix3f normalMatrix = matrices.getNormalMatrix();
//
//        int norm = this.computeNormal(normalMatrix, quad.getFace());
//
//        QuadVertexSink drain = VertexDrain.of(this)
//                .createSink(VanillaVertexTypes.QUADS);
//        drain.ensureCapacity(4);
//
//        for (int i = 0; i < 4; i++) {
//            float x = quadView.getX(i);
//            float y = quadView.getY(i);
//            float z = quadView.getZ(i);
//
//            float fR;
//            float fG;
//            float fB;
//
//            float brightness = brightnessTable[i];
//
//            if (colorize) {
//                int color = quadView.getColor(i);
//
//                float oR = ColorU8.normalize(ColorABGR.unpackRed(color));
//                float oG = ColorU8.normalize(ColorABGR.unpackGreen(color));
//                float oB = ColorU8.normalize(ColorABGR.unpackBlue(color));
//
//                fR = oR * brightness * r;
//                fG = oG * brightness * g;
//                fB = oB * brightness * b;
//            } else {
//                fR = brightness * r;
//                fG = brightness * g;
//                fB = brightness * b;
//            }
//
//            float u = quadView.getTexU(i);
//            float v = quadView.getTexV(i);
//
//            int color = ColorABGR.pack(fR, fG, fB, 1.0F);
//
//            Vector4f pos = new Vector4f(x, y, z, 1.0F);
//            positionMatrix.transform(pos);
//
//            drain.writeQuad(pos.x(), pos.y(), pos.z(), color, u, v, light[i], overlay, norm);
//        }
//
//        drain.flush();
//    }
//
//    private int computeNormal(Matrix3f normalMatrix, Direction face) {
//        Vec3i faceNorm = face.getVector();
//
//        float x = faceNorm.getX();
//        float y = faceNorm.getY();
//        float z = faceNorm.getZ();
//
//        float x2 = normalMatrix.m00 * x + normalMatrix.m01 * y + normalMatrix.m02 * z;
//        float y2 = normalMatrix.m10 * x + normalMatrix.m11 * y + normalMatrix.m12 * z;
//        float z2 = normalMatrix.m20 * x + normalMatrix.m21 * y + normalMatrix.m22 * z;
//
//        return Norm3b.pack(x2, y2, z2);
//    }
}
