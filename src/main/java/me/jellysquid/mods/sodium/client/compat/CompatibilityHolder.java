package me.jellysquid.mods.sodium.client.compat;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
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
import org.apache.logging.log4j.Logger;

/**
 * Utility class that initializes and holds all compatibility-related interface instances.
 */
public final class CompatibilityHolder {
    private CompatibilityHolder() { }

    private static final Logger LOGGER = SodiumClientMod.logger();

    public static final FabricHookInvoker FABRIC_HOOKS;

    static {
        final FabricLoader loader = FabricLoader.getInstance();
        if (loader.isModLoaded("fabric-rendering-v1")) {
            LOGGER.info("Sodium has detected that Fabric Rendering v1 is installed. Activating compatibility hooks...");
            FABRIC_HOOKS = new FabricHookInvoker() {
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
            FABRIC_HOOKS = new FabricHookInvoker() {
                @Override
                public void rendering_prepareContext(WorldRenderer worldRenderer, MatrixStack matrixStack, float tickDelta, long limitTime, boolean blockOutlines, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f projectionMatrix, ClientWorld world, Profiler profiler, boolean advancedTranslucency, VertexConsumerProvider consumers) { }

                @Override
                public void rendering_setContextFrustum(Frustum frustum) { }

                @Override
                public void rendering_prepareBlockOutlineContext(VertexConsumer vertexConsumer, Entity entity, double cameraX, double cameraY, double cameraZ, BlockPos blockPos, BlockState blockState) { }
            };
        }
    }
}
