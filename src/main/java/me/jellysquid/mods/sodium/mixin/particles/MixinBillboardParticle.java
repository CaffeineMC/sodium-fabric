package me.jellysquid.mods.sodium.mixin.particles;

import me.jellysquid.mods.sodium.client.model.DirectVertexConsumer;
import me.jellysquid.mods.sodium.client.util.ColorARGB;
import net.minecraft.client.particle.BillboardParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
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
    public abstract float getSize(float float_1);

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
    public void buildGeometry(VertexConsumer vertices, Camera camera, float tickDelta) {
        Vec3d cameraPos = camera.getPos();

        float posX = (float) (MathHelper.lerp(tickDelta, this.prevPosX, this.x) - cameraPos.getX());
        float posY = (float) (MathHelper.lerp(tickDelta, this.prevPosY, this.y) - cameraPos.getY());
        float posZ = (float) (MathHelper.lerp(tickDelta, this.prevPosZ, this.z) - cameraPos.getZ());

        Quaternion rotation = camera.getRotation();

        if (this.angle != 0.0F) {
            rotation = this.rotateCamera(rotation, tickDelta);
        }

        float size = this.getSize(tickDelta);

        float minU = this.getMinU();
        float maxU = this.getMaxU();
        float minV = this.getMinV();
        float maxV = this.getMaxV();

        int color = ColorARGB.pack(this.colorRed, this.colorGreen, this.colorBlue, this.colorAlpha);
        int brightness = this.getColorMultiplier(tickDelta);

        this.addVertex(vertices, -1.0F, -1.0F, maxU, maxV, color, brightness, rotation, size, posX, posY, posZ);
        this.addVertex(vertices, -1.0F, 1.0F, maxU, minV, color, brightness, rotation, size, posX, posY, posZ);
        this.addVertex(vertices, 1.0F, 1.0F, minU, minV, color, brightness, rotation, size, posX, posY, posZ);
        this.addVertex(vertices, 1.0F, -1.0F, minU, maxV, color, brightness, rotation, size, posX, posY, posZ);
    }

    protected Quaternion rotateCamera(Quaternion rotation, float tickDelta) {
        float angle = MathHelper.lerp(tickDelta, this.prevAngle, this.angle);

        float rx = rotation.getX();
        float ry = rotation.getY();
        float rz = rotation.getZ();
        float rw = rotation.getW();

        float r0 = angle / 2.0F;
        float r1 = MathHelper.sin(r0);
        float r2 = MathHelper.cos(r0);

        float zx = rx * r2 + ry * r1;
        float zy = -(rx * r1) + ry * r2;
        float zz = rw * r1 + rz * r2;
        float zw = rw * r2 - rz * r1;

        return new Quaternion(zx, zy, zz, zw);
    }

    private void addVertex(VertexConsumer vertices, float x, float y, float u, float v, int color, int brightness, Quaternion rotation, float scale, float offsetX, float offsetY, float offsetZ) {
        float rx = rotation.getX();
        float ry = rotation.getY();
        float rz = rotation.getZ();
        float rw = rotation.getW();

        // Quaternion.hamiltonProduct(x, y, 1.0F, 0.0F)
        float qx = rw * x + ry - rz * y;
        float qy = rw * y - rx + rz * x;
        float qz = rw + rx * y - ry * x;
        float qw = -(rx * x) - (ry * y - rz);

        // Quaternion.conjugate
        float cx = -rx;
        float cy = -ry;
        float cz = -rz;

        // Quaternion.hamiltonProduct
        float fx = qw * cx + qx * rw + qy * cz - qz * cy;
        float fy = qw * cy - qx * cz + qy * rw + qz * cx;
        float fz = qw * cz + qx * cy - qy * cx + qz * rw;

        fx = fx * scale + offsetX;
        fy = fy * scale + offsetY;
        fz = fz * scale + offsetZ;

        DirectVertexConsumer directVertexConsumer = DirectVertexConsumer.getDirectVertexConsumer(vertices);

        if (directVertexConsumer != null) {
            directVertexConsumer.vertexParticle(fx, fy, fz, u, v, color, brightness);
        } else {
            vertices.vertex(fx, fy, fz).texture(u, v).color(this.colorRed, this.colorGreen, this.colorBlue, this.colorAlpha).light(brightness).next();
        }
    }
}
