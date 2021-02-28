package me.jellysquid.mods.sodium.client.compat;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.compat.impl.fabric.rendering.BlockOutlineContextImpl;
import me.jellysquid.mods.sodium.client.compat.impl.fabric.rendering.WorldRenderContextImpl;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.rendering.v1.InvalidateRenderStateCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.chunk.WorldChunk;
import org.apache.logging.log4j.Logger;

/**
 * Utility class that initializes and holds all compatibility-related hook interfaces.
 */
public final class CompatibilityHooks {
    private CompatibilityHooks() { }

    public static void initialize() {
        // This method exists purely to run the static block on startup.
    }

    private static final Logger LOGGER = SodiumClientMod.logger();

    public static final FabricRenderingHooks FABRIC_RENDERING;
    public static final FabricLifecycleEventsHooks FABRIC_LIFECYCLE_EVENTS;

    static {
        final FabricLoader loader = FabricLoader.getInstance();
        LOGGER.info("Sodium is now looking for mods to enable compatibility hooks for...");
        FABRIC_RENDERING = createFabricRenderingHooks(loader);
        FABRIC_LIFECYCLE_EVENTS = createFabricLifecycleEventsHooks(loader);
        LOGGER.info("Compatibility hooks done! Enjoy your improved performance!");
    }

    private static FabricRenderingHooks createFabricRenderingHooks(FabricLoader loader) {
        if (loader.isModLoaded("fabric-rendering-v1")) {
            LOGGER.info("Sodium has detected that Fabric Rendering v1 is installed. Activating compatibility hooks...");
            return new FabricRenderingHooks() {
                private final WorldRenderContextImpl ctx = new WorldRenderContextImpl();
                private final BlockOutlineContextImpl blockOutlineCtx = new BlockOutlineContextImpl();

                @Override
                public void prepareContext(WorldRenderer worldRenderer, MatrixStack matrixStack, float tickDelta, long limitTime, boolean blockOutlines, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f projectionMatrix, ClientWorld world, Profiler profiler, boolean advancedTranslucency, VertexConsumerProvider consumers) {
                    ctx.worldRenderer = worldRenderer;
                    ctx.matrixStack = matrixStack;
                    ctx.tickDelta = tickDelta;
                    ctx.limitTime = limitTime;
                    ctx.blockOutlines = blockOutlines;
                    ctx.camera = camera;
                    ctx.gameRenderer = gameRenderer;
                    ctx.lightmapTextureManager = lightmapTextureManager;
                    ctx.projectionMatrix = projectionMatrix;
                    ctx.world = world;
                    ctx.profiler = profiler;
                    ctx.advancedTranslucency = advancedTranslucency;
                    ctx.consumers = consumers;
                    ctx.frustum = null;
                }

                @Override
                public void invokeStartEvent() {
                    WorldRenderEvents.START.invoker().onStart(ctx);
                }

                @Override
                public void setContextFrustum(Frustum frustum) {
                    ctx.frustum = frustum;
                }

                @Override
                public void invokeAfterSetupEvent() {
                    WorldRenderEvents.AFTER_SETUP.invoker().afterSetup(ctx);
                }

                @Override
                public void invokeBeforeEntitiesEvent() {
                    WorldRenderEvents.BEFORE_ENTITIES.invoker().beforeEntities(ctx);
                }

                @Override
                public void invokeAfterEntitiesEvent() {
                    WorldRenderEvents.AFTER_ENTITIES.invoker().afterEntities(ctx);
                }

                @Override
                public void invokeBeforeBlockOutlineEvent() {
                    ctx.blockOutlines = WorldRenderEvents.BEFORE_BLOCK_OUTLINE.invoker().beforeBlockOutline(ctx, MinecraftClient.getInstance().crosshairTarget);
                }

                @Override
                public boolean shouldRenderBlockOutline() {
                    return ctx.blockOutlines;
                }

                @Override
                public void prepareBlockOutlineContext(VertexConsumer vertexConsumer, Entity entity, double cameraX, double cameraY, double cameraZ, BlockPos blockPos, BlockState blockState) {
                    blockOutlineCtx.vertexConsumer = vertexConsumer;
                    blockOutlineCtx.entity = entity;
                    blockOutlineCtx.cameraX = cameraX;
                    blockOutlineCtx.cameraY = cameraY;
                    blockOutlineCtx.cameraZ = cameraZ;
                    blockOutlineCtx.blockPos = blockPos;
                    blockOutlineCtx.blockState = blockState;
                }

                @Override
                public void invokeBlockOutlineEvent() {
                    WorldRenderEvents.BLOCK_OUTLINE.invoker().onBlockOutline(ctx, blockOutlineCtx);
                }

                @Override
                public void invokeBeforeDebugRenderEvent() {
                    WorldRenderEvents.BEFORE_DEBUG_RENDER.invoker().beforeDebugRender(ctx);
                }

                @Override
                public void invokeAfterTranslucentEvent() {
                    WorldRenderEvents.AFTER_TRANSLUCENT.invoker().afterTranslucent(ctx);
                }

                @Override
                public void invokeLastEvent() {
                    WorldRenderEvents.LAST.invoker().onLast(ctx);
                }

                @Override
                public void invokeEndEvent() {
                    WorldRenderEvents.END.invoker().onEnd(ctx);
                }

                @Override
                public void invokeInvalidateRenderStateEvent() {
                    InvalidateRenderStateCallback.EVENT.invoker().onInvalidate();
                }
            };
        } else {
            LOGGER.info("Sodium has detected that Fabric Rendering v1 is NOT installed.");
            return new FabricRenderingHooks() {
                @Override
                public void prepareContext(WorldRenderer worldRenderer, MatrixStack matrixStack, float tickDelta, long limitTime, boolean blockOutlines, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f projectionMatrix, ClientWorld world, Profiler profiler, boolean advancedTranslucency, VertexConsumerProvider consumers) { }

                @Override
                public void invokeStartEvent() { }

                @Override
                public void setContextFrustum(Frustum frustum) { }

                @Override
                public void invokeAfterSetupEvent() { }

                @Override
                public void invokeBeforeEntitiesEvent() { }

                @Override
                public void invokeAfterEntitiesEvent() { }

                @Override
                public void invokeBeforeBlockOutlineEvent() { }

                @Override
                public boolean shouldRenderBlockOutline() { return true; }

                @Override
                public void prepareBlockOutlineContext(VertexConsumer vertexConsumer, Entity entity, double cameraX, double cameraY, double cameraZ, BlockPos blockPos, BlockState blockState) { }

                @Override
                public void invokeBlockOutlineEvent() { }

                @Override
                public void invokeBeforeDebugRenderEvent() { }

                @Override
                public void invokeAfterTranslucentEvent() { }

                @Override
                public void invokeLastEvent() { }

                @Override
                public void invokeEndEvent() { }

                @Override
                public void invokeInvalidateRenderStateEvent() { }
            };
        }
    }

    private static FabricLifecycleEventsHooks createFabricLifecycleEventsHooks(FabricLoader loader) {
        if (loader.isModLoaded("fabric-lifecycle-events-v1")) {
            LOGGER.info("Sodium has detected that Fabric Lifecycle Events v1 is installed. Activating compatibility hooks...");
            return new FabricLifecycleEventsHooks() {
                @Override
                public void invokeOnClientChunkLoad(ClientWorld world, WorldChunk chunk) {
                    ClientChunkEvents.CHUNK_LOAD.invoker().onChunkLoad(world, chunk);
                }

                @Override
                public void invokeOnClientChunkUnload(ClientWorld world, WorldChunk chunk) {
                    ClientChunkEvents.CHUNK_UNLOAD.invoker().onChunkUnload(world, chunk);
                }
            };
        } else {
            LOGGER.info("Sodium has detected that Fabric Lifecycle Events v1 is NOT installed.");
            return new FabricLifecycleEventsHooks() {
                @Override
                public void invokeOnClientChunkLoad(ClientWorld world, WorldChunk chunk) { }

                @Override
                public void invokeOnClientChunkUnload(ClientWorld world, WorldChunk chunk) { }
            };
        }
    }

}
