package me.jellysquid.mods.sodium.client.compat;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.impl.client.rendering.WorldRenderContextImpl;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
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

    private static final Logger LOGGER = SodiumClientMod.logger();

    public static final FabricRenderingHooks FABRIC_RENDERING;
    public static final FabricLifecycleEventsHooks FABRIC_LIFECYCLE_EVENTS;

    static {
        final FabricLoader loader = FabricLoader.getInstance();
        if (loader.isModLoaded("fabric-rendering-v1")) {
            LOGGER.info("Sodium has detected that Fabric Rendering v1 is installed. Activating compatibility hooks...");
            FABRIC_RENDERING = new FabricRenderingHooks() {
                private final WorldRenderContextImpl renderContext = new WorldRenderContextImpl();

                @Override
                public void rendering_prepareContext(WorldRenderer worldRenderer, MatrixStack matrixStack, float tickDelta, long limitTime, boolean blockOutlines, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f projectionMatrix, ClientWorld world, Profiler profiler, boolean advancedTranslucency, VertexConsumerProvider consumers) {
                    renderContext.prepare(worldRenderer, matrixStack, tickDelta, limitTime, blockOutlines, camera, gameRenderer, lightmapTextureManager, projectionMatrix, consumers, profiler, advancedTranslucency, world);
                }

                @Override
                public void rendering_setContextFrustum(Frustum frustum) {
                    renderContext.setFrustum(frustum);
                }

                @Override
                public void rendering_prepareBlockOutlineContext(VertexConsumer vertexConsumer, Entity entity, double cameraX, double cameraY, double cameraZ, BlockPos blockPos, BlockState blockState) {
                    renderContext.prepareBlockOutline(vertexConsumer, entity, cameraX, cameraY, cameraZ, blockPos, blockState);
                }
            };
        } else {
            LOGGER.info("Sodium has detected that Fabric Rendering v1 is NOT installed.");
            FABRIC_RENDERING = new FabricRenderingHooks() {
                @Override
                public void rendering_prepareContext(WorldRenderer worldRenderer, MatrixStack matrixStack, float tickDelta, long limitTime, boolean blockOutlines, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f projectionMatrix, ClientWorld world, Profiler profiler, boolean advancedTranslucency, VertexConsumerProvider consumers) { }

                @Override
                public void rendering_setContextFrustum(Frustum frustum) { }

                @Override
                public void rendering_prepareBlockOutlineContext(VertexConsumer vertexConsumer, Entity entity, double cameraX, double cameraY, double cameraZ, BlockPos blockPos, BlockState blockState) { }
            };
        }
        if (loader.isModLoaded("fabric-lifecycle-events-v1")) {
            LOGGER.info("Sodium has detected that Fabric Lifecycle Events v1 is installed. Activating compatibility hooks...");
            FABRIC_LIFECYCLE_EVENTS = new FabricLifecycleEventsHooks() {
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
            FABRIC_LIFECYCLE_EVENTS = new FabricLifecycleEventsHooks() {
                @Override
                public void invokeOnClientChunkLoad(ClientWorld world, WorldChunk chunk) { }

                @Override
                public void invokeOnClientChunkUnload(ClientWorld world, WorldChunk chunk) { }
            };
        }
    }
}
