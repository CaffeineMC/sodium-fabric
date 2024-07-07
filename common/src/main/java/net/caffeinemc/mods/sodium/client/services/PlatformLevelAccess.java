package net.caffeinemc.mods.sodium.client.services;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.client.model.color.ColorProviderRegistry;
import net.caffeinemc.mods.sodium.client.model.light.LightPipelineProvider;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.FluidRenderer;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.List;
import java.util.function.Function;

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
    void runChunkLayerEvents(RenderType renderLayer, LevelRenderer levelRenderer, Matrix4f modelMatrix, Matrix4f projectionMatrix, int ticks, Camera mainCamera, Frustum cullingFrustum);

    /**
     * Returns any NeoForge chunk renderers to run. <b>This is not thread safe.</b>
     * @param level The current level
     * @param origin The origin of the current chunk
     * @return Any NeoForge chunk renderers to run
     */
    List<?> getExtraRenderers(Level level, BlockPos origin);

    /**
     * Runs any NeoForge chunk renderers.
     * @param renderers The list of chunk renderers to run.
     * @param typeToConsumer A consumer that converts render types to vertex consumers
     * @param slice The current level slice
     */
    void renderAdditionalRenderers(List<?> renderers, Function<RenderType, VertexConsumer> typeToConsumer, LevelSlice slice);

    /**
     * Gets the current light manager for the chunk section.
     * @param chunk The current chunk.
     * @param pos The section within that chunk being drawn.
     * @return The current light manager, or null
     */
    @Nullable Object getLightManager(LevelChunk chunk, SectionPos pos);
}
