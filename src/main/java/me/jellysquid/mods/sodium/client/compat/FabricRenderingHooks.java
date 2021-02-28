package me.jellysquid.mods.sodium.client.compat;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.profiler.Profiler;

public interface FabricRenderingHooks {
    void prepareContext(WorldRenderer worldRenderer, MatrixStack matrixStack, float tickDelta, long limitTime,
                        boolean blockOutlines, Camera camera, GameRenderer gameRenderer,
                        LightmapTextureManager lightmapTextureManager, Matrix4f projectionMatrix,
                        ClientWorld world, Profiler profiler, boolean advancedTranslucency,
                        VertexConsumerProvider consumers);
    void invokeStartEvent();
    void setContextFrustum(Frustum frustum);
    void invokeAfterSetupEvent();
    void invokeBeforeEntitiesEvent();
    void invokeAfterEntitiesEvent();
    void invokeBeforeBlockOutlineEvent();
    boolean shouldRenderBlockOutline();
    void prepareBlockOutlineContext(VertexConsumer vertexConsumer, Entity entity,
                                    double cameraX, double cameraY, double cameraZ,
                                    BlockPos blockPos, BlockState blockState);
    void invokeBlockOutlineEvent();
    void invokeBeforeDebugRenderEvent();
    void invokeAfterTranslucentEvent();
    void invokeLastEvent();
    void invokeEndEvent();
    void invokeInvalidateRenderStateEvent();
}
