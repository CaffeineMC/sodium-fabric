package me.jellysquid.mods.sodium.mixin.features.particle.cull;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;

@Mixin(ParticleManager.class)
public class MixinParticleManager {
    @Shadow
    @Final
    private Map<ParticleTextureSheet, Queue<Particle>> particles;

    private final Queue<Particle> cachedQueue = new ArrayDeque<>();
    private boolean useCulling;

    @Inject(method = "renderParticles", at = @At("HEAD"))
    private void preRenderParticles(MatrixStack matrixStack, VertexConsumerProvider.Immediate immediate, LightmapTextureManager lightmapTextureManager, Camera camera, float f, CallbackInfo ci) {
        // Setup the frustum state before rendering particles
        this.useCulling = SodiumClientMod.options().advanced.useParticleCulling;
    }

    @SuppressWarnings({ "SuspiciousMethodCalls", "unchecked" })
    @Redirect(method = "renderParticles", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private <V> V filterParticleList(Map<ParticleTextureSheet, Queue<Particle>> map, Object key, MatrixStack matrixStack, VertexConsumerProvider.Immediate immediate, LightmapTextureManager lightmapTextureManager, Camera camera, float f) {
        Queue<Particle> queue = this.particles.get(key);

        if (queue == null || queue.isEmpty()) {
            return null;
        }

        // If culling isn't enabled, simply return the queue as-is
        if (!this.useCulling) {
            return (V) queue;
        }

        // Filter particles which are not visible
        Queue<Particle> filtered = this.cachedQueue;
        filtered.clear();

        SodiumWorldRenderer worldRenderer = SodiumWorldRenderer.getInstance();

        for (Particle particle : queue) {
            Box box = particle.getBoundingBox();

            // Hack: Grow the particle's bounding box in order to work around mis-behaved particles
            if (!worldRenderer.isBoxVisible(box.minX - 1.0D, box.minY - 1.0D, box.minZ - 1.0D, box.maxX + 1.0D, box.maxY + 1.0D, box.maxZ + 1.0D)) {
                continue;
            }

            filtered.add(particle);
        }

        return (V) filtered;
    }

    @Inject(method = "renderParticles", at = @At("RETURN"))
    private void postRenderParticles(MatrixStack matrixStack, VertexConsumerProvider.Immediate immediate, LightmapTextureManager lightmapTextureManager, Camera camera, float f, CallbackInfo ci) {
        // Ensure particles don't linger in the temporary collection
        this.cachedQueue.clear();
    }
}
