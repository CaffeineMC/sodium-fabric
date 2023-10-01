package me.jellysquid.mods.sodium.mixin.features.render.particle;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeFormat;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.particle.*;
import me.jellysquid.mods.sodium.client.render.particle.cache.ParticleTextureCache;
import me.jellysquid.mods.sodium.client.render.particle.shader.BillboardParticleData;
import me.jellysquid.mods.sodium.client.render.particle.shader.ParticleShaderInterface;
import net.caffeinemc.mods.sodium.api.buffer.UnmanagedBufferBuilder;
import net.caffeinemc.mods.sodium.api.util.RawUVs;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.particle.*;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;

@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {
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
    private final ParticleTextureCache particleTexCache = new ParticleTextureCache();

    @Unique
    private static final Object2BooleanMap<Class<? extends BillboardParticle>> classOverridesBuild = new Object2BooleanOpenHashMap<>();

    @Unique
    private ParticleRenderView renderView;

    @Unique
    private static final String BUILD_GEOMETRY_METHOD = FabricLoader.getInstance().getMappingResolver().mapMethodName(
            "intermediary",
            "net.minecraft.class_703",
            "method_3074",
            "(Lnet/minecraft/class_4588;Lnet/minecraft/class_4184;F)V"
    );

    @Unique
    private int glVertexArray;

    @Unique
    private UnmanagedBufferBuilder dataBufferBuilder;

    @Unique
    private ParticleDataBuffer dataBuffer = null;

    @Unique
    private Identifier prevTexture = null;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void postInit(ClientWorld world, TextureManager textureManager, CallbackInfo ci) {
        this.glVertexArray = GlStateManager._glGenVertexArrays();
        this.dataBufferBuilder = new UnmanagedBufferBuilder(1);
        this.renderView = new ParticleRenderView(world);
    }

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

    @Inject(method = "renderParticles", at = @At("HEAD"))
    private void preRenderParticles(MatrixStack matrices,
                                    VertexConsumerProvider.Immediate vertexConsumers,
                                    LightmapTextureManager lightmapTextureManager,
                                    Camera camera,
                                    float tickDelta,
                                    CallbackInfo ci) {
        this.renderView.resetCache();
    }

    @Inject(
            method = "renderParticles",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;applyModelViewMatrix()V", ordinal = 0, shift = At.Shift.AFTER),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    public void renderParticles(
            MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers,
            LightmapTextureManager lightmapTextureManager, Camera camera, float tickDelta,
            CallbackInfo ci, MatrixStack matrixStack
    ) {
        RenderDevice.enterManagedCode();
        try (CommandList commands = RenderDevice.INSTANCE.createCommandList()) {
            if (this.dataBuffer == null) {
                this.dataBuffer = new ParticleDataBuffer(commands);
            }

            particleRenderer.begin();
            ParticleShaderInterface shader = this.particleRenderer.getActiveProgram().getInterface();
            shader.setProjectionMatrix(RenderSystem.getProjectionMatrix());
            shader.setModelViewMatrix(RenderSystem.getModelViewMatrix());

            for (ParticleTextureSheet particleTextureSheet : PARTICLE_TEXTURE_SHEETS) {
                Queue<BillboardParticle> iterable = this.billboardParticles.get(particleTextureSheet);
                if (iterable != null && !iterable.isEmpty()) {
                    int numParticles = iterable.size();
                    bindParticleTextureSheet(particleTextureSheet);
                    this.dataBuffer.bind();
                    particleRenderer.setupState();

                    for (BillboardParticle particle : iterable) {
                        ((BillboardExtended) particle).sodium$buildParticleData(
                                dataBufferBuilder,
                                particleTexCache,
                                camera, tickDelta
                        );
                    }

                    drawParticleTextureSheet(commands, particleTextureSheet, numParticles);
                }
            }
        } finally {
            prevTexture = null;
            particleRenderer.end();
            RenderDevice.exitManagedCode();
        }
    }

    @Unique
    @SuppressWarnings("deprecation")
    private void bindParticleTextureSheet(ParticleTextureSheet sheet) {
        RenderSystem.depthMask(true);
        Identifier texture = null;
        if (sheet == ParticleTextureSheet.PARTICLE_SHEET_LIT || sheet == ParticleTextureSheet.PARTICLE_SHEET_OPAQUE) {
            RenderSystem.disableBlend();
            texture = SpriteAtlasTexture.PARTICLE_ATLAS_TEXTURE;
        } else if (sheet == ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT) {
            RenderSystem.enableBlend();
            texture = SpriteAtlasTexture.PARTICLE_ATLAS_TEXTURE;
        } else if (sheet == ParticleTextureSheet.TERRAIN_SHEET) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            texture = SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
        } else if (sheet == ParticleTextureSheet.CUSTOM) {
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
        }

        if (texture != null && !texture.equals(prevTexture)) {
            RenderSystem.setShaderTexture(0, texture);
            this.prevTexture = texture;
        }
    }

    @Unique
    private void drawParticleTextureSheet(CommandList commands, ParticleTextureSheet sheet, int numParticles) {
        if (sheet == ParticleTextureSheet.TERRAIN_SHEET || sheet == ParticleTextureSheet.PARTICLE_SHEET_LIT || sheet == ParticleTextureSheet.PARTICLE_SHEET_OPAQUE || sheet == ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT) {
            uploadParticleBuffer(commands, numParticles);
            bindDummyVao();
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, numParticles * 6);
        }
    }

    @Unique
    private void bindDummyVao() {
        GlStateManager._glBindVertexArray(this.glVertexArray);
        GL20C.glVertexAttribPointer(0, 1, GlVertexAttributeFormat.UNSIGNED_BYTE.typeId(), false, 1, 0);
        GL20C.glEnableVertexAttribArray(0);
    }

    @Unique
    private void uploadParticleBuffer(CommandList commands, int numParticles) {
        RawUVs[] toUpload = this.particleTexCache.update();
        int maxUploadIndex = this.particleTexCache.getTopIndex();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            int size = RawUVs.STRIDE * maxUploadIndex;

            long buffer = stack.nmalloc(size);
            long ptr = buffer;
            for (int i = 0; i < maxUploadIndex; i++) {
                RawUVs uvs = toUpload[i];
                if (uvs == null) {
                    RawUVs.putNull(ptr);
                } else {
                    uvs.put(ptr);
                }
                ptr += RawUVs.STRIDE;
            }
            dataBufferBuilder.push(stack, buffer, size);
        }

        particleRenderer.getActiveProgram()
                .getInterface()
                .setTextureOffset((numParticles * BillboardParticleData.STRIDE) / 4);

        UnmanagedBufferBuilder.Built data = dataBufferBuilder.end();
        this.dataBuffer.uploadParticleData(commands, data);
    }

    @Inject(method = "setWorld", at = @At("RETURN"))
    private void postSetWorld(ClientWorld world, CallbackInfo ci) {
        this.renderView = new ParticleRenderView(world);
    }

    @Inject(method = "addParticle(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"))
    private void preAddParticle(Particle particle, CallbackInfo ci) {
        if (particle instanceof ParticleExtended extension) {
            extension.sodium$configure(this.renderView);
        }
    }
}
