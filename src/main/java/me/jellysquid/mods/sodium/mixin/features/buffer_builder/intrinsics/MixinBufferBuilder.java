package me.jellysquid.mods.sodium.mixin.features.buffer_builder.intrinsics;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorSpecies;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.vertex.VanillaVertexTypes;
import me.jellysquid.mods.sodium.client.model.vertex.VertexDrain;
import me.jellysquid.mods.sodium.client.model.vertex.formats.quad.QuadVertexSink;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import me.jellysquid.mods.sodium.client.util.color.ColorU8;
import me.jellysquid.mods.sodium.client.util.math.MatrixUtil;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.FixedColorVertexConsumer;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@SuppressWarnings({ "SameParameterValue" })
@Mixin(BufferBuilder.class)
public abstract class MixinBufferBuilder extends FixedColorVertexConsumer {
    @Shadow
    private boolean textured;

    @Override
    public void quad(MatrixStack.Entry matrices, BakedQuad quad, float[] brightnessTable, float r, float g, float b, int[] light, int overlay, boolean colorize) {
        if (!this.textured) {
            super.quad(matrices, quad, brightnessTable, r, g, b, light, overlay, colorize);

            return;
        }

        if (this.colorFixed) {
            throw new IllegalStateException();
        }

        ModelQuadView quadView = (ModelQuadView) quad;

        Matrix4f modelMatrix = matrices.getModel();
        Matrix3f normalMatrix = matrices.getNormal();

        int norm = MatrixUtil.computeNormal(normalMatrix, quad.getFace());

        QuadVertexSink drain = VertexDrain.of(this)
                .createSink(VanillaVertexTypes.QUADS);
        drain.ensureCapacity(4);

        VectorSpecies<Float> SPECIES = FloatVector.SPECIES_128;
        for (int i = 0; i < 4; i++) {
            float x = quadView.getX(i);
            float y = quadView.getY(i);
            float z = quadView.getZ(i);

            FloatVector c;

            float brightness = brightnessTable[i];

            if (colorize) {
                int color = quadView.getColor(i);

                c = FloatVector.fromArray(
                        SPECIES,
                        new float[] {
                                ColorU8.normalize(ColorABGR.unpackRed(color)),
                                ColorU8.normalize(ColorABGR.unpackGreen(color)),
                                ColorU8.normalize(ColorABGR.unpackBlue(color)),
                                0
                        },
                        0
                ).mul(brightness).mul(
                        SPECIES.fromArray(
                                new float[]{r, g, b, 0},
                                0
                        )
                );
            } else {
                c = FloatVector.fromArray(
                        SPECIES,
                        new float[]{r, g, b, 0},
                        0
                ).mul(brightness);
            }

            float u = quadView.getTexU(i);
            float v = quadView.getTexV(i);

            int color = ColorABGR.pack(c.lane(0), c.lane(1), c.lane(2), 1.0F);

            Vector4f pos = new Vector4f(x, y, z, 1.0F);
            pos.transform(modelMatrix);

            drain.writeQuad(pos.getX(), pos.getY(), pos.getZ(), color, u, v, light[i], overlay, norm);
        }

        drain.flush();
    }
}
