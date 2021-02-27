package me.jellysquid.mods.sodium.client.compat;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.NotNull;

public interface FabricRenderingHooks {
    void rendering_prepareContext(WorldRenderer worldRenderer, MatrixStack matrixStack, float tickDelta, long limitTime,
                                  boolean blockOutlines, Camera camera, GameRenderer gameRenderer,
                                  LightmapTextureManager lightmapTextureManager, Matrix4f projectionMatrix,
                                  ClientWorld world, Profiler profiler, boolean advancedTranslucency,
                                  VertexConsumerProvider consumers);
    void rendering_setContextFrustum(Frustum frustum);
    void rendering_prepareBlockOutlineContext(VertexConsumer vertexConsumer, Entity entity,
                                              double cameraX, double cameraY, double cameraZ,
                                              BlockPos blockPos, BlockState blockState);
    // TODO "invoke event" methods
}
