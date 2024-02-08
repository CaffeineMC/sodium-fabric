package me.jellysquid.mods.sodium.mixin.features.render.particle;

import net.caffeinemc.mods.sodium.api.vertex.format.common.ParticleVertex;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SingleQuadParticle.class)
public abstract class SingleQuadParticleMixin extends Particle {
    @Shadow
    public abstract float getQuadSize(float tickDelta);

    @Shadow
    protected abstract float getU0();

    @Shadow
    protected abstract float getU1();

    @Shadow
    protected abstract float getV0();

    @Shadow
    protected abstract float getV1();

    protected SingleQuadParticleMixin(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
    }

    /**
     * @reason Optimize function
     * @author JellySquid
     */
    @Overwrite
    public void render(VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
        Vec3 vec3d = camera.getPosition();

        float x = (float) (Mth.lerp(tickDelta, this.xo, this.x) - vec3d.x());
        float y = (float) (Mth.lerp(tickDelta, this.yo, this.y) - vec3d.y());
        float z = (float) (Mth.lerp(tickDelta, this.zo, this.z) - vec3d.z());

        Quaternionf quaternion;

        if (this.roll == 0.0F) {
            quaternion = camera.rotation();
        } else {
            float angle = Mth.lerp(tickDelta, this.oRoll, this.roll);

            quaternion = new Quaternionf(camera.rotation());
            quaternion.rotateZ(angle);
        }

        float size = this.getQuadSize(tickDelta);
        int light = this.getLightColor(tickDelta);

        float minU = this.getU0();
        float maxU = this.getU1();
        float minV = this.getV0();
        float maxV = this.getV1();

        int color = ColorABGR.pack(this.rCol , this.gCol, this.bCol, this.alpha);

        var writer = VertexBufferWriter.of(vertexConsumer);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            long buffer = stack.nmalloc(4 * ParticleVertex.STRIDE);
            long ptr = buffer;

            writeVertex(ptr, quaternion,-1.0F, -1.0F, x, y, z, maxU, maxV, color, light, size);
            ptr += ParticleVertex.STRIDE;

            writeVertex(ptr, quaternion,-1.0F, 1.0F, x, y, z, maxU, minV, color, light, size);
            ptr += ParticleVertex.STRIDE;

            writeVertex(ptr, quaternion,1.0F, 1.0F, x, y, z, minU, minV, color, light, size);
            ptr += ParticleVertex.STRIDE;

            writeVertex(ptr, quaternion,1.0F, -1.0F, x, y, z, minU, maxV, color, light, size);
            ptr += ParticleVertex.STRIDE;

            writer.push(stack, buffer, 4, ParticleVertex.FORMAT);
        }

    }

    @Unique
    @SuppressWarnings("UnnecessaryLocalVariable")
    private static void writeVertex(long buffer,
                                    Quaternionf rotation,
                                    float posX, float posY,
                                    float originX, float originY, float originZ,
                                    float u, float v, int color, int light, float size) {
        // Quaternion q0 = new Quaternion(rotation);
        float q0x = rotation.x();
        float q0y = rotation.y();
        float q0z = rotation.z();
        float q0w = rotation.w();

        // q0.hamiltonProduct(x, y, 0.0f, 0.0f)
        float q1x = (q0w * posX) - (q0z * posY);
        float q1y = (q0w * posY) + (q0z * posX);
        float q1w = (q0x * posY) - (q0y * posX);
        float q1z = -(q0x * posX) - (q0y * posY);

        // Quaternion q2 = new Quaternion(rotation);
        // q2.conjugate()
        float q2x = -q0x;
        float q2y = -q0y;
        float q2z = -q0z;
        float q2w = q0w;

        // q2.hamiltonProduct(q1)
        float q3x = q1z * q2x + q1x * q2w + q1y * q2z - q1w * q2y;
        float q3y = q1z * q2y - q1x * q2z + q1y * q2w + q1w * q2x;
        float q3z = q1z * q2z + q1x * q2y - q1y * q2x + q1w * q2w;

        // Vector3f f = new Vector3f(q2.getX(), q2.getY(), q2.getZ())
        // f.multiply(size)
        // f.add(pos)
        float fx = (q3x * size) + originX;
        float fy = (q3y * size) + originY;
        float fz = (q3z * size) + originZ;

        ParticleVertex.put(buffer, fx, fy, fz, u, v, color, light);
    }
}
