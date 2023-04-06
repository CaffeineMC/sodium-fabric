package me.jellysquid.mods.sodium.client.render.chunk;

import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gl.shader.*;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegionManager;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.minecraft.util.Identifier;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL43C.glDispatchCompute;

public class TranslucentSorting {
    protected final RenderDevice device;
    private final GlProgram<TranslucentSortingInterface> sorter;
    public TranslucentSorting(RenderDevice device) {
        this.device = device;
        GlShader sortingShader = ShaderLoader.loadShader(ShaderType.COMPUTE,
                new Identifier("sodium",  "sorting/bitonic.comp"),
                ShaderConstants.builder()
                        .build());
        try {
            this.sorter = GlProgram.builder(new Identifier("sodium", "translucency_sorter"))
                    .attachShader(sortingShader)
                    .link(TranslucentSortingInterface::new);
        } finally {
            sortingShader.delete();
        }
    }

    private record TranslucentRegion() {

    }
    private final PriorityQueue<TranslucentRegion> translucentRegions = new ObjectHeapPriorityQueue<>();

    public void partiallySort(Vector3f sortOrigin, RenderRegionManager manager, RenderSection section) {
        sorter.bind();
        var region = manager.getRegion(section.getRegionId());
        sorter.getInterface().setupBase(region);
        var store = region.storage.get(DefaultTerrainRenderPasses.TRANSLUCENT);
        var state = store.getState(section);
        if (state == null) return;
        if (!state.hasIndexSegment()) return;
        for (var dir : ModelQuadFacing.values()) {
            if (state.getModelPart(dir) == null) continue;
            sorter.getInterface().setupSection(sortOrigin, new Vector3f(section.getChunkX()<<4, section.getChunkY()<<4, section.getChunkZ()<<4), state, dir);
            sorter.getInterface().dispatch();
        }

    }

    public void delete(CommandList commandList) {
        sorter.delete();
    }
}
