package net.caffeinemc.mods.sodium.neoforge.level;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.client.model.color.ColorProviderRegistry;
import net.caffeinemc.mods.sodium.client.model.light.LightPipelineProvider;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.FluidRenderer;
import net.caffeinemc.mods.sodium.client.services.PlatformLevelAccess;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.caffeinemc.mods.sodium.neoforge.render.FluidRendererImpl;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.List;
import java.util.function.Function;

public class NeoForgeLevelAccess implements PlatformLevelAccess {
    @Override
    public FluidRenderer createPlatformFluidRenderer(ColorProviderRegistry colorRegistry, LightPipelineProvider lightPipelineProvider) {
        return new FluidRendererImpl(colorRegistry, lightPipelineProvider);
    }

    @Override
    public boolean tryRenderFluid() {
        return false;
    }

    @Override
    public void runChunkLayerEvents(RenderType renderType, LevelRenderer levelRenderer, Matrix4f modelMatrix, Matrix4f projectionMatrix, int renderTick, Camera camera, Frustum frustum) {
        ClientHooks.dispatchRenderStage(renderType, levelRenderer, modelMatrix, projectionMatrix, renderTick, camera, frustum);
    }

    private static final ThreadLocal<PoseStack> emptyStack = ThreadLocal.withInitial(PoseStack::new);

    @Override
    public List<?> getExtraRenderers(Level level, BlockPos origin) {
        return ClientHooks.gatherAdditionalRenderers(origin, level);
    }

    @Override
    public void renderAdditionalRenderers(List<?> renderers, Function<RenderType, VertexConsumer> typeToConsumer, LevelSlice slice) {
        AddSectionGeometryEvent.SectionRenderingContext context = new AddSectionGeometryEvent.SectionRenderingContext(typeToConsumer, slice, emptyStack.get());
        for (int i = 0, renderersSize = renderers.size(); i < renderersSize; i++) {
            AddSectionGeometryEvent.AdditionalSectionRenderer renderer = (AddSectionGeometryEvent.AdditionalSectionRenderer) renderers.get(i);
            renderer.render(context);
        }
    }

    @Override
    public @Nullable Object getLightManager(LevelChunk chunk, SectionPos pos) {
        return chunk.getAuxLightManager(pos.origin());
    }
}
