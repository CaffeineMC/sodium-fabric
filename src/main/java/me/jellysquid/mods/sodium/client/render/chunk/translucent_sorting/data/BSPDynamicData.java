package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.data;

import org.joml.Vector3fc;

import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.TQuad;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree.BSPNode;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree.BSPResult;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import net.minecraft.util.math.ChunkSectionPos;

public class BSPDynamicData extends DynamicData {
    private static final int NODE_REUSE_MIN_GENERATION = 1;

    private final BSPNode rootNode;
    private final int generation;

    private BSPDynamicData(ChunkSectionPos sectionPos,
            NativeBuffer buffer, VertexRange range, BSPResult result, int generation) {
        super(sectionPos, buffer, range, result);
        this.rootNode = result.getRootNode();
        this.generation = generation;
    }

    @Override
    public void sortOnTrigger(Vector3fc cameraPos) {
        this.sort(cameraPos);
    }

    private void sort(Vector3fc cameraPos) {
        this.unsetReuseUploadedData();

        this.rootNode.collectSortedQuads(getBuffer(), cameraPos);
    }

    public static BSPDynamicData fromMesh(BuiltSectionMeshParts translucentMesh,
            Vector3fc cameraPos, TQuad[] quads, ChunkSectionPos sectionPos,
            NativeBuffer buffer, TranslucentData oldData) {
        BSPNode oldRoot = null;
        int generation = 0;
        boolean prepareNodeReuse = false;
        if (oldData instanceof BSPDynamicData oldBSPData) {
            generation = oldBSPData.generation + 1;
            oldRoot = oldBSPData.rootNode;

            // only enable partial updates after a certain number of generations
            // (times the section has been built)
            prepareNodeReuse = generation >= NODE_REUSE_MIN_GENERATION;
        }
        var result = BSPNode.buildBSP(quads, sectionPos, oldRoot, prepareNodeReuse);

        VertexRange range = TranslucentData.getUnassignedVertexRange(translucentMesh);
        buffer = PresentTranslucentData.nativeBufferForQuads(buffer, quads);

        var dynamicData = new BSPDynamicData(sectionPos, buffer, range, result, generation);
        dynamicData.sort(cameraPos);

        // prepare accumulation groups for integration into GFNI triggering
        // TODO: combine this and the similar code in TopoSortDynamicData
        var aligned = result.getAlignedDistances();
        if (aligned != null) {
            for (var accGroup : aligned) {
                if (accGroup != null) {
                    accGroup.prepareIntegration();
                }
            }
        }
        var unaligned = result.getUnalignedDistances();
        if (unaligned != null) {
            for (var accGroup : unaligned) {
                if (accGroup != null) {
                    accGroup.prepareIntegration();
                }
            }
        }

        return dynamicData;
    }
}
