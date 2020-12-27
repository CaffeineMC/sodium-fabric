package me.jellysquid.mods.sodium.mixin.features.particle.fast_render;

import me.jellysquid.mods.sodium.client.model.vertex.DefaultVertexTypes;
import me.jellysquid.mods.sodium.client.model.vertex.VertexDrain;
import me.jellysquid.mods.sodium.client.model.vertex.formats.particle.ParticleVertexSink;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import net.minecraft.client.particle.BillboardParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BillboardParticle.class)
public abstract class MixinBillboardParticle extends Particle {
    @Shadow
    public abstract float getSize(float tickDelta);

    @Shadow
    protected abstract float getMinU();

    @Shadow
    protected abstract float getMaxU();

    @Shadow
    protected abstract float getMinV();

    @Shadow
    protected abstract float getMaxV();

    protected MixinBillboardParticle(ClientWorld world, double x, double y, double z) {
        super(world, x, y, z);
    }

    /**
     * @reason Optimize function
     * @author JellySquid
     */
    @Overwrite
    public void buildGeometry(VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
        Vec3d vec3d = camera.getPos();

        float x = (float) (MathHelper.lerp(tickDelta, this.prevPosX, this.x) - vec3d.getX());
        float y = (float) (MathHelper.lerp(tickDelta, this.prevPosY, this.y) - vec3d.getY());
        float z = (float) (MathHelper.lerp(tickDelta, this.prevPosZ, this.z) - vec3d.getZ());

        Quaternion quaternion;

        if (this.angle == 0.0F) {
            quaternion = camera.getRotation();
        } else {
            float angle = MathHelper.lerp(tickDelta, this.prevAngle, this.angle);

            quaternion = new Quaternion(camera.getRotation());
            quaternion.hamiltonProduct(Vector3f.POSITIVE_Z.getRadialQuaternion(angle));
        }

        float size = this.getSize(tickDelta);
        int light = this.getColorMultiplier(tickDelta);

        float minU = this.getMinU();
        float maxU = this.getMaxU();
        float minV = this.getMinV();
        float maxV = this.getMaxV();

        int color = ColorABGR.pack(this.colorRed, this.colorGreen, this.colorBlue, this.colorAlpha);

        ParticleVertexSink drain = VertexDrain.of(vertexConsumer)
                .createSink(DefaultVertexTypes.PARTICLES);

        addVertex(drain, quaternion,-1.0F, -1.0F, x, y, z, maxU, maxV, color, light, size);
        addVertex(drain, quaternion,-1.0F, 1.0F, x, y, z, maxU, minV, color, light, size);
        addVertex(drain, quaternion,1.0F, 1.0F, x, y, z, minU, minV, color, light, size);
        addVertex(drain, quaternion,1.0F, -1.0F, x, y, z, minU, maxV, color, light, size);

        drain.flush();
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    private static void addVertex(ParticleVertexSink drain, Quaternion rotation,
                                  float x, float y, float posX, float posY, float posZ, float u, float v, int color, int light, float size) {
        // Quaternion q0 = new Quaternion(rotation);
        float q0x = rotation.getX();
        float q0y = rotation.getY();
        float q0z = rotation.getZ();
        float q0w = rotation.getW();

        // q0.hamiltonProduct(x, y, 0.0f, 0.0f)
        float q1x = (q0w * x) - (q0z * y);
        float q1y = (q0w * y) + (q0z * x);
        float q1w = (q0x * y) - (q0y * x);
        float q1z = -(q0x * x) - (q0y * y);

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
        float fx = (q3x * size) + posX;
        float fy = (q3y * size) + posY;
        float fz = (q3z * size) + posZ;

        drain.writeParticle(fx, fy, fz, u, v, color, light);
    }
}
