package net.caffeinemc.mods.sodium.neoforge.level;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.client.services.PlatformLevelRenderHooks;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import org.joml.Matrix4f;

import java.util.List;
import java.util.function.Function;

public class NeoForgeLevelRenderHooks implements PlatformLevelRenderHooks {
    @Override
    public void runChunkLayerEvents(RenderType renderType, LevelRenderer levelRenderer, Matrix4f modelMatrix, Matrix4f projectionMatrix, int renderTick, Camera camera, Frustum frustum) {
        ClientHooks.dispatchRenderStage(renderType, levelRenderer, modelMatrix, projectionMatrix, renderTick, camera, frustum);
    }

    @Override
    public List<?> retrieveChunkMeshAppenders(Level level, BlockPos origin) {
        return ClientHooks.gatherAdditionalRenderers(origin, level);
    }

    @Override
    public void runChunkMeshAppenders(List<?> renderers, Function<RenderType, VertexConsumer> typeToConsumer, LevelSlice slice) {
        AddSectionGeometryEvent.SectionRenderingContext context = new AddSectionGeometryEvent.SectionRenderingContext(typeToConsumer, slice, new PoseStack());
        for (Object o : renderers) {
            ((AddSectionGeometryEvent.AdditionalSectionRenderer) o).render(context);
        }
    }
}
