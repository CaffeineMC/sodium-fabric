package me.jellysquid.mods.sodium.mixin.features.render.particle;

import me.jellysquid.mods.sodium.client.render.particle.BillboardExtended;
import me.jellysquid.mods.sodium.client.render.particle.shader.BillboardParticleData;
import net.caffeinemc.mods.sodium.api.buffer.UnmanagedBufferBuilder;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.minecraft.client.particle.BillboardParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BillboardParticle.class)
public abstract class BillboardParticleMixin extends Particle implements BillboardExtended {
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

    protected BillboardParticleMixin(ClientWorld world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @Override
    public void sodium$buildParticleData(UnmanagedBufferBuilder builder, Camera camera, float tickDelta) {
        Vec3d vec3d = camera.getPos();

        float minU = this.getMinU();
        float maxU = this.getMaxU();
        float minV = this.getMinV();
        float maxV = this.getMaxV();

        float x = (float) (MathHelper.lerp(tickDelta, this.prevPosX, this.x) - vec3d.getX());
        float y = (float) (MathHelper.lerp(tickDelta, this.prevPosY, this.y) - vec3d.getY());
        float z = (float) (MathHelper.lerp(tickDelta, this.prevPosZ, this.z) - vec3d.getZ());

        float size = this.getSize(tickDelta);
        int light = this.getBrightness(tickDelta);

        int color = ColorABGR.pack(this.red , this.green, this.blue, this.alpha);

        float angle = MathHelper.lerp(tickDelta, this.prevAngle, this.angle);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            long ptr = stack.nmalloc(BillboardParticleData.STRIDE);
            BillboardParticleData.put(
                    ptr, x, y, z, color, light, size, angle,
                    minU, minV, maxU, maxV
            );
            builder.push(stack, ptr, BillboardParticleData.STRIDE);
        }
    }

    /**
     * @reason Remove function
     * @author BeljihnWahfl
     */
    @Overwrite
    public void buildGeometry(VertexConsumer vertexConsumer, Camera camera, float tickDelta) {}
}
