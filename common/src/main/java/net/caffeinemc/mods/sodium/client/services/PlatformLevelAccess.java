package net.caffeinemc.mods.sodium.client.services;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.client.model.color.ColorProviderRegistry;
import net.caffeinemc.mods.sodium.client.model.light.LightPipelineProvider;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.FluidRenderer;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.caffeinemc.mods.sodium.client.world.SodiumAuxiliaryLightManager;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
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
     * Gets the specialized render data for this block entity.
     * @param blockEntity The block entity to get the render data of.
     * @return The specialized render data for this block entity. If the platform does not support it or there is no data, null.
     */
    @Nullable
    Object getBlockEntityData(BlockEntity blockEntity);

    /**
     * Gets the current light manager for the chunk section.
     * @param chunk The current chunk.
     * @param pos The section within that chunk being drawn.
     * @return The current light manager, or null
     */
    @Nullable SodiumAuxiliaryLightManager getLightManager(LevelChunk chunk, SectionPos pos);
}
