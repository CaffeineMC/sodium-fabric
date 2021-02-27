package me.jellysquid.mods.sodium.client.compat.impl.fabric.rendering;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.Nullable;

public class WorldRenderContextImpl implements WorldRenderContext {
    public WorldRenderer worldRenderer;
    public MatrixStack matrixStack;
    public float tickDelta;
    public long limitTime;
    public boolean blockOutlines;
    public Camera camera;
    public GameRenderer gameRenderer;
    public LightmapTextureManager lightmapTextureManager;
    public Matrix4f projectionMatrix;
    public ClientWorld world;
    public Profiler profiler;
    public boolean advancedTranslucency;
    public VertexConsumerProvider consumers;
    public Frustum frustum;

    @Override
    public WorldRenderer worldRenderer() {
        return worldRenderer;
    }

    @Override
    public MatrixStack matrixStack() {
        return matrixStack;
    }

    @Override
    public float tickDelta() {
        return tickDelta;
    }

    @Override
    public long limitTime() {
        return limitTime;
    }

    @Override
    public boolean blockOutlines() {
        return blockOutlines;
    }

    @Override
    public Camera camera() {
        return camera;
    }

    @Override
    public GameRenderer gameRenderer() {
        return gameRenderer;
    }

    @Override
    public LightmapTextureManager lightmapTextureManager() {
        return lightmapTextureManager;
    }

    @Override
    public Matrix4f projectionMatrix() {
        return projectionMatrix;
    }

    @Override
    public ClientWorld world() {
        return world;
    }

    @Override
    public Profiler profiler() {
        return profiler;
    }

    @Override
    public boolean advancedTranslucency() {
        return advancedTranslucency;
    }

    @Override
    public @Nullable VertexConsumerProvider consumers() {
        return consumers;
    }

    @Override
    public @Nullable Frustum frustum() {
        return frustum;
    }
}
