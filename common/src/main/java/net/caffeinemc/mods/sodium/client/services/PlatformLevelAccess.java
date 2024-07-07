package net.caffeinemc.mods.sodium.client.services;

import com.mojang.blaze3d.vertex.PoseStack;
import net.caffeinemc.mods.sodium.client.model.color.ColorProviderRegistry;
import net.caffeinemc.mods.sodium.client.model.light.LightPipelineProvider;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.FluidRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;

public interface PlatformLevelAccess {
    PlatformLevelAccess INSTANCE = Services.load(PlatformLevelAccess.class);

    static PlatformLevelAccess getInstance() {
        return INSTANCE;
    }

    /**
     * Creates a new platform dependent fluid renderer.
     * @param colorRegistry The current color registry.
     * @param lightPipelineProvider The current {@code LightPipelineProvider}.
     * @return A new fluid renderer.
     */
    FluidRenderer createPlatformFluidRenderer(ColorProviderRegistry colorRegistry, LightPipelineProvider lightPipelineProvider);

    /**
     * Should be run in the top of the Vanilla fluid renderer. If true, cancel the Vanilla render.
     * @return if the vanilla rendering should be cancelled.
     */
    boolean tryRenderFluid();

    /**
     * Runs any events after drawing a chunk layer.
     * @param renderLayer The current chunk layer that was drawn
     * @param levelRenderer The level renderer
     * @param modelMatrix The current modelview matrix
     * @param projectionMatrix The current projection matrix
     * @param ticks The current tick count
     * @param mainCamera The current camera
     * @param cullingFrustum The current frustum
     */
    void runChunkLayerEvents(RenderType renderLayer, LevelRenderer levelRenderer, PoseStack modelMatrix, Matrix4f projectionMatrix, int ticks, Camera mainCamera, Frustum cullingFrustum);
}
