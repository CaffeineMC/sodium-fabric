package me.jellysquid.mods.sodium.mixin.features.render.particle;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import me.jellysquid.mods.sodium.client.render.particle.ExtendedParticle;
import me.jellysquid.mods.sodium.client.render.particle.ParticleExtended;
import me.jellysquid.mods.sodium.client.render.particle.ParticleRenderView;
import me.jellysquid.mods.sodium.client.render.particle.ShaderBillboardParticleRenderer;
import me.jellysquid.mods.sodium.client.render.particle.shader.BillboardParticleVertex;
import me.jellysquid.mods.sodium.client.render.particle.shader.ParticleShaderInterface;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.particle.*;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleEffect;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.*;

@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {
    @Unique
    private final BufferBuilder bufferBuilder = new BufferBuilder(1);

    @Unique
    private final BufferBuilder testBuffer = new BufferBuilder(1);

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
    private static final Object2BooleanMap<Class<? extends BillboardParticle>> classOverridesBuild = new Object2BooleanOpenHashMap<>();

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
        testBuffer.begin(VertexFormat.DrawMode.QUADS, BillboardParticleVertex.MC_VERTEX_FORMAT);
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
                        (pClass) -> this.testClassOverrides(bParticle)
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

        testBuffer.end().release();
    }

    @Unique
    private boolean testClassOverrides(BillboardParticle particle) {
        particle.buildGeometry(testBuffer, MinecraftClient.getInstance().gameRenderer.getCamera(), 0);
        return !((ExtendedParticle) particle).sodium$reachedBillboardDraw();
    }

    @Inject(method = "clearParticles", at = @At("TAIL"))
    private void clearParticles(CallbackInfo ci) {
        this.billboardParticles.clear();
    }

    @Inject(method = "renderParticles", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;applyModelViewMatrix()V", ordinal = 0, shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD)
    public void renderParticles(
            MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers,
            LightmapTextureManager lightmapTextureManager, Camera camera, float tickDelta,
            CallbackInfo ci, MatrixStack matrixStack
    ) {
        particleRenderer.begin();
        ParticleShaderInterface shader = this.particleRenderer.getActiveProgram().getInterface();
        shader.setProjectionMatrix(RenderSystem.getProjectionMatrix());
        shader.setModelViewMatrix(RenderSystem.getModelViewMatrix());

        for (ParticleTextureSheet particleTextureSheet : PARTICLE_TEXTURE_SHEETS) {
            Queue<BillboardParticle> iterable = this.billboardParticles.get(particleTextureSheet);
            if (iterable != null && !iterable.isEmpty()) {
                bindParticleTextureSheet(particleTextureSheet);
                particleRenderer.setupState();
                bufferBuilder.begin(VertexFormat.DrawMode.QUADS, BillboardParticleVertex.MC_VERTEX_FORMAT);

                for (BillboardParticle particle : iterable) {
                    particle.buildGeometry(bufferBuilder, camera, tickDelta);
                }

                drawParticleTextureSheet(particleTextureSheet, bufferBuilder, iterable.size());

            }
        }

        particleRenderer.end();
    }

    @Unique
    private static void bindParticleTextureSheet(ParticleTextureSheet sheet) {
        RenderSystem.depthMask(true);
        if (sheet == ParticleTextureSheet.PARTICLE_SHEET_LIT || sheet == ParticleTextureSheet.PARTICLE_SHEET_OPAQUE) {
            RenderSystem.disableBlend();
            RenderSystem.setShaderTexture(0, SpriteAtlasTexture.PARTICLE_ATLAS_TEXTURE);
        } else if (sheet == ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT) {
            RenderSystem.enableBlend();
            RenderSystem.setShaderTexture(0, SpriteAtlasTexture.PARTICLE_ATLAS_TEXTURE);
        } else if (sheet == ParticleTextureSheet.TERRAIN_SHEET) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderTexture(0, SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
        } else if (sheet == ParticleTextureSheet.CUSTOM) {
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
        }
    }

    @Unique
    private static void drawParticleTextureSheet(ParticleTextureSheet sheet, BufferBuilder builder, int numParticles) {
        if (sheet == ParticleTextureSheet.TERRAIN_SHEET || sheet == ParticleTextureSheet.PARTICLE_SHEET_LIT || sheet == ParticleTextureSheet.PARTICLE_SHEET_OPAQUE || sheet == ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT) {
            BufferBuilder.BuiltBuffer built = builder.end();
            VertexBuffer buffer = built.getParameters().format().getBuffer();

            buffer.bind();
            buffer.upload(built);
            BillboardParticleVertex.bindVertexFormat();

            buffer.draw();
        }
    }

    @Unique
    private ParticleRenderView renderView;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void postInit(ClientWorld world, TextureManager textureManager, CallbackInfo ci) {
        this.renderView = new ParticleRenderView(world);
    }

    @Inject(method = "setWorld", at = @At("RETURN"))
    private void postSetWorld(ClientWorld world, CallbackInfo ci) {
        this.renderView = new ParticleRenderView(world);
    }

    @Inject(method = "renderParticles", at = @At("HEAD"))
    private void preRenderParticles(MatrixStack matrices,
                                   VertexConsumerProvider.Immediate vertexConsumers,
                                   LightmapTextureManager lightmapTextureManager,
                                   Camera camera,
                                   float tickDelta,
                                   CallbackInfo ci) {
        this.renderView.resetCache();
    }

    @Inject(method = "createParticle", at = @At("RETURN"))
    private <T extends ParticleEffect> void postCreateParticle(T parameters,
                                                               double x,
                                                               double y,
                                                               double z,
                                                               double velocityX,
                                                               double velocityY,
                                                               double velocityZ,
                                                               CallbackInfoReturnable<@Nullable Particle> cir) {
        var particle = cir.getReturnValue();

        if (particle instanceof ParticleExtended extension) {
            extension.sodium$configure(this.renderView);
        }
    }
}
