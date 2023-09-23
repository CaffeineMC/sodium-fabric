package me.jellysquid.mods.sodium.mixin.features.render.particle;

import me.jellysquid.mods.sodium.client.render.particle.shader.BillboardParticleVertex;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
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
import org.spongepowered.asm.mixin.Unique;

@Mixin(BillboardParticle.class)
public abstract class BillboardParticleMixin extends Particle {
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

        float size = this.getSize(tickDelta);
        int light = this.getBrightness(tickDelta);

        float minU = this.getMinU();
        float maxU = this.getMaxU();
        float minV = this.getMinV();
        float maxV = this.getMaxV();

        int color = ColorABGR.pack(this.red , this.green, this.blue, this.alpha);

        float angle = MathHelper.lerp(tickDelta, this.prevAngle, this.angle);

        var writer = VertexBufferWriter.of(vertexConsumer);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            long buffer = stack.nmalloc(4 * BillboardParticleVertex.STRIDE);
            long ptr = buffer;

            writeVertex(ptr, x, y, z, maxU, maxV, color, light, size, angle);
            ptr += BillboardParticleVertex.STRIDE;

            writeVertex(ptr, x, y, z, maxU, minV, color, light, size, angle);
            ptr += BillboardParticleVertex.STRIDE;

            writeVertex(ptr, x, y, z, minU, minV, color, light, size, angle);
            ptr += BillboardParticleVertex.STRIDE;

            writeVertex(ptr, x, y, z, minU, maxV, color, light, size, angle);
            ptr += BillboardParticleVertex.STRIDE;

            writer.push(stack, buffer, 4, BillboardParticleVertex.VERTEX_FORMAT_DESCRIPTION);
        }
    }

    @Unique
    private static void writeVertex(long buffer, float originX, float originY, float originZ,
                                    float u, float v, int color, int light, float size, float angle) {

        BillboardParticleVertex.put(buffer, originX, originY, originZ, u, v, color, light, size, angle);
    }
}
