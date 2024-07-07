package net.caffeinemc.mods.sodium.fabric.level;

import com.mojang.blaze3d.vertex.PoseStack;
import net.caffeinemc.mods.sodium.client.model.color.ColorProviderRegistry;
import net.caffeinemc.mods.sodium.client.model.light.LightPipelineProvider;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.FluidRenderer;
import net.caffeinemc.mods.sodium.client.services.PlatformLevelAccess;
import net.caffeinemc.mods.sodium.fabric.render.FluidRendererImpl;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;

public class FabricLevelAccess implements PlatformLevelAccess {
    @Override
    public FluidRenderer createPlatformFluidRenderer(ColorProviderRegistry colorRegistry, LightPipelineProvider lightPipelineProvider) {
        return new FluidRendererImpl(colorRegistry, lightPipelineProvider);
    }

    @Override
    public boolean tryRenderFluid() {
        return FluidRendererImpl.tryRenderFluid();
    }

    @Override
    public void runChunkLayerEvents(RenderType renderLayer, LevelRenderer levelRenderer, PoseStack modelMatrix, Matrix4f projectionMatrix, int ticks, Camera mainCamera, Frustum cullingFrustum) {

    }
}
