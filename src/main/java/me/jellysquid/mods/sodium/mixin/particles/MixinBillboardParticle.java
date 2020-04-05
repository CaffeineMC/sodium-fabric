package me.jellysquid.mods.sodium.mixin.particles;

import me.jellysquid.mods.sodium.client.render.pipeline.DirectVertexConsumer;
import me.jellysquid.mods.sodium.client.util.ColorUtil;
import net.minecraft.client.particle.BillboardParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BillboardParticle.class)
public abstract class MixinBillboardParticle extends Particle {
    @Shadow
    public abstract float getSize(float float_1);

    @Shadow
    protected abstract float getMinU();

    @Shadow
    protected abstract float getMaxU();

    @Shadow
    protected abstract float getMinV();

    @Shadow
    protected abstract float getMaxV();

    protected MixinBillboardParticle(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    /**
     * @reason Optimize function
     * @author JellySquid
     */
    @Overwrite
    public void buildGeometry(VertexConsumer vertices, Camera camera, float tickDelta) {
        Vec3d cameraPos = camera.getPos();

        float posX = (float) (MathHelper.lerp(tickDelta, this.prevPosX, this.x) - cameraPos.getX());
        float posY = (float) (MathHelper.lerp(tickDelta, this.prevPosY, this.y) - cameraPos.getY());
        float posZ = (float) (MathHelper.lerp(tickDelta, this.prevPosZ, this.z) - cameraPos.getZ());

        Quaternion rotation = camera.getRotation();

        if (this.angle != 0.0F) {
            float angle = MathHelper.lerp(tickDelta, this.prevAngle, this.angle);

            float r1 = angle * (float) Math.sin(angle / 2.0F);
            float r2 = (float) Math.cos(angle / 2.0F);

            float x = rotation.getB();
            float y = rotation.getC();
            float z = rotation.getD();
            float a = rotation.getA();

            float b2 = (x * r2) + (y * r1);
            float c2 = (x * r1) + (y * r2);
            float d2 = (a * r1) + (z * r2);
            float a2 = (a * r2) - (z * r1);

            rotation = new Quaternion(b2, c2, d2, a2);
        }

        float size = this.getSize(tickDelta);

        float minU = this.getMinU();
        float maxU = this.getMaxU();
        float minV = this.getMinV();
        float maxV = this.getMaxV();

        int color = ColorUtil.encodeRGBA(this.colorRed, this.colorGreen, this.colorBlue, this.colorAlpha);
        int brightness = this.getColorMultiplier(tickDelta);

        this.addVertex(vertices, -1.0F, -1.0F, maxU, maxV, color, brightness, rotation, size, posX, posY, posZ);
        this.addVertex(vertices, -1.0F,  1.0F, maxU, minV, color, brightness, rotation, size, posX, posY, posZ);
        this.addVertex(vertices,  1.0F,  1.0F, minU, minV, color, brightness, rotation, size, posX, posY, posZ);
        this.addVertex(vertices,  1.0F, -1.0F, minU, maxV, color, brightness, rotation, size, posX, posY, posZ);
    }

    private void addVertex(VertexConsumer vertices, float x, float y, float u, float v, int color, int brightness, Quaternion rotation, float scale, float offsetX, float offsetY, float offsetZ) {
        float rx = rotation.getB();
        float ry = rotation.getC();
        float rz = rotation.getD();
        float rw = rotation.getA();

        float b1 = (rw * x) - (rz * y);
        float c2 = (rw * y) + (rz * x);
        float d2 = (rx * y) - (ry * x);
        float a2 = (rx * x) - (ry * y);

        float cb = -rx;
        float cc = -ry;
        float cd = -rz;

        float fx = (a2 * cb) + (b1 * rw) + (c2 * cd) - (d2 * cc);
        float fy = (a2 * cc) - (b1 * cd) + (c2 * rw) + (d2 * cb);
        float fz = (a2 * cd) + (b1 * cc) - (c2 * cb) + (d2 * rw);

        fx = (fx * scale) + offsetX;
        fy = (fy * scale) + offsetY;
        fz = (fz * scale) + offsetZ;

        ((DirectVertexConsumer) vertices).vertexParticle(fx, fy, fz, u, v, color, brightness);
    }
}
