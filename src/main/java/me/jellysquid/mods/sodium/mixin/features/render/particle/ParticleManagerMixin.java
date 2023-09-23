package me.jellysquid.mods.sodium.mixin.features.render.particle;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.sodium.client.render.particle.ShaderBillboardParticleRenderer;
import me.jellysquid.mods.sodium.client.render.particle.shader.BillboardParticleVertex;
import me.jellysquid.mods.sodium.client.render.particle.shader.ParticleShaderInterface;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.particle.*;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.*;

@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {
    @Unique
    private final BufferBuilder bufferBuilder = new BufferBuilder(1);

    @Shadow
    protected ClientWorld world;

    @Shadow
    @Final
    private static List<ParticleTextureSheet> PARTICLE_TEXTURE_SHEETS;

    @Shadow
    @Final
    private Queue<EmitterParticle> newEmitterParticles;

    @Shadow
    @Final
    private Queue<Particle> newParticles;

    @Shadow
    @Final
    private Map<ParticleTextureSheet, Queue<Particle>> particles;

    @Unique
    private final Map<ParticleTextureSheet, Queue<BillboardParticle>> billboardParticles = Maps.newIdentityHashMap();

    @Unique
    private final ShaderBillboardParticleRenderer particleRenderer = new ShaderBillboardParticleRenderer();

    @Unique
    private static final Map<Class<? extends BillboardParticle>, Boolean> classOverridesBuild = Maps.newIdentityHashMap();

    @Unique
    private static final String BUILD_GEOMETRY_METHOD = FabricLoader.getInstance().getMappingResolver().mapMethodName(
            "intermediary",
            "net.minecraft.class_703",
            "method_3074",
            "(Lnet/minecraft/class_4588;Lnet/minecraft/class_4184;F)V"
    );

    @Shadow
    protected abstract void tickParticles(Collection<Particle> particles);

    /**
     * @author BeljihnWahfl
     * @reason Could not feasibly inject all needed functionality
     */
    @Overwrite
    public void tick() {
        this.particles.forEach((sheet, queue) -> {
            this.world.getProfiler().push(sheet.toString());
            this.tickParticles(queue);
            this.world.getProfiler().pop();
        });

        this.billboardParticles.forEach((sheet, queue) -> {
            this.world.getProfiler().push(sheet.toString());
            // This is safe because tickParticles never adds to the collection.
            this.tickParticles((Collection) queue);
            this.world.getProfiler().pop();
        });

        if (!this.newEmitterParticles.isEmpty()) {
            List<EmitterParticle> list = Lists.newArrayList();

            for(EmitterParticle emitterParticle : this.newEmitterParticles) {
                emitterParticle.tick();
                if (!emitterParticle.isAlive()) {
                    list.add(emitterParticle);
                }
            }

            this.newEmitterParticles.removeAll(list);
        }

        Particle particle;
        if (!this.newParticles.isEmpty()) {
            while((particle = this.newParticles.poll()) != null) {
                if (particle instanceof BillboardParticle bParticle && !classOverridesBuild.computeIfAbsent(
                        bParticle.getClass(),
                        this::testClassOverrides
                )) {
                    this.billboardParticles
                            .computeIfAbsent(particle.getType(), sheet -> EvictingQueue.create(16384))
                            .add((BillboardParticle) particle);
                } else {
                    this.particles
                            .computeIfAbsent(particle.getType(), sheet -> EvictingQueue.create(16384))
                            .add(particle);
                }
            }
        }
    }

    @Unique
    private boolean testClassOverrides(Class<? extends BillboardParticle> particleClass) {
        try {
            return particleClass.getDeclaredMethod(
                    BUILD_GEOMETRY_METHOD,
                    VertexConsumer.class,
                    Camera.class,
                    float.class
            ).getDeclaringClass() != BillboardParticle.class;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    @Inject(method = "clearParticles", at = @At("TAIL"))
    private void clearParticles(CallbackInfo ci) {
        this.billboardParticles.clear();
    }

    @Inject(method = "renderParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V"), locals = LocalCapture.CAPTURE_FAILHARD)
    public void renderParticles(
            MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers,
            LightmapTextureManager lightmapTextureManager, Camera camera, float tickDelta,
            CallbackInfo ci, MatrixStack matrixStack
    ) {
        for(ParticleTextureSheet particleTextureSheet : PARTICLE_TEXTURE_SHEETS) {
            Queue<BillboardParticle> iterable = this.billboardParticles.get(particleTextureSheet);
            if (iterable != null && !iterable.isEmpty()) {
                bindParticleTextureSheet(particleTextureSheet);
                bufferBuilder.begin(VertexFormat.DrawMode.QUADS, BillboardParticleVertex.MC_VERTEX_FORMAT);

                particleRenderer.begin();
                ParticleShaderInterface shader = this.particleRenderer.getActiveProgram().getInterface();
                shader.setProjectionMatrix(RenderSystem.getProjectionMatrix());
                shader.setModelViewMatrix(RenderSystem.getModelViewMatrix());
                shader.setCameraRotation(camera.getRotation());

                for(BillboardParticle particle : iterable) {
                    particle.buildGeometry(bufferBuilder, camera, tickDelta);
                }

                BufferBuilder.BuiltBuffer built = bufferBuilder.end();
                VertexBuffer buffer = built.getParameters().format().getBuffer();

                buffer.bind();
                buffer.upload(built);
                BillboardParticleVertex.bindVertexFormat();

                int numParticles = iterable.size();
                int indexType = RenderSystem.getSequentialBuffer(VertexFormat.DrawMode.QUADS).getIndexType().glType;
                RenderSystem.drawElements(4, numParticles * 6, indexType);

                particleRenderer.end();
            }
        }
    }

    @Unique
    private static void bindParticleTextureSheet(ParticleTextureSheet sheet) {
        if (sheet == ParticleTextureSheet.PARTICLE_SHEET_LIT ||
                sheet == ParticleTextureSheet.PARTICLE_SHEET_OPAQUE ||
                sheet == ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT
        ) {
            RenderSystem.setShaderTexture(0, SpriteAtlasTexture.PARTICLE_ATLAS_TEXTURE);
        } else if (sheet == ParticleTextureSheet.TERRAIN_SHEET) {
            RenderSystem.setShaderTexture(0, SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
        }
    }
}
