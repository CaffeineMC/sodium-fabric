package me.jellysquid.mods.sodium.client.render.chunk.sorting;

import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferStorageFlags;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gl.shader.*;
import me.jellysquid.mods.sodium.client.gl.util.EnumBitField;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.SharedQuadIndexBuffer;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegionManager;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import net.minecraft.util.Identifier;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.EnumSet;

public class TranslucentSorting {
    public static final int MAX_BATCHES_PER_DISPATCH = 256;
    public static final int QUADS_PER_WORKGROUP_BITS = 8;
    public static final int QUADS_PER_WORKGROUP = 1<<QUADS_PER_WORKGROUP_BITS;
    protected final RenderDevice device;
    private final GlBuffer batchBuffer;
    private final ByteBuffer batchBuilderBuffer;
    private final GlProgram<TranslucentSortingInterface> sorter;
    private final RenderRegionManager regionManager;

    private GlProgram<TranslucentSortingInterface> generateShader() {
        var constants = ShaderConstants.builder();
        constants.addIn("""                   
                    struct QuadIndicies {
                        int a;
                        int b;
                        int c;
                        int e;
                        int f;
                        int g;
                    };
                                        
                    uint getIndexBase(QuadIndicies indicies) {
                        //Zeros out the lower bits
                        return indicies.a&0xFFFFFFFC;
                    }""");
        GlShader sortingShader = ShaderLoader.loadShader(ShaderType.COMPUTE,
                new Identifier("sodium", "sorting/bitonic.comp"),
                constants.build());

        GlProgram<TranslucentSortingInterface> res;
        try {
            res = GlProgram.builder(new Identifier("sodium", "translucency_sorter"))
                    .attachShader(sortingShader)
                    .link(TranslucentSortingInterface::new);
        } finally {
            sortingShader.delete();
        }
        return res;
    }
    public TranslucentSorting(RenderDevice device, RenderRegionManager regionManager) {
        this.device = device;
        this.regionManager = regionManager;

        this.sorter = generateShader();

        int bufferSize = MAX_BATCHES_PER_DISPATCH<<4;
        this.batchBuffer = device.createCommandList().createImmutableBuffer(bufferSize, new EnumBitField<>(EnumSet.noneOf(GlBufferStorageFlags.class)));
        this.batchBuilderBuffer = ByteBuffer.allocateDirect(bufferSize);
    }

    private int setupBatches(boolean shiftByHalf, CommandList cl, RenderRegion.RenderRegionStorage storage, RenderSection... sections) {
        long addr = MemoryUtil.memAddress(batchBuilderBuffer);
        int batchCount = 0;
        boolean needsMultiPass = false;
        for (var section : sections) {
            var state = storage.getState(section);
            var range = state.getModelPart(ModelQuadFacing.UNASSIGNED);
            int quads = (range.vertexCount()>>2) - (shiftByHalf?QUADS_PER_WORKGROUP/2:0);
            if (quads > 256) {
                needsMultiPass = true;
            }

            //Dont resort if the original quad count is < QUADS_PER_WORKGROUP
            int batches = (int) Math.ceil(((float)quads)/QUADS_PER_WORKGROUP);
            if ((batchCount+batches)>(QUADS_PER_WORKGROUP*MAX_BATCHES_PER_DISPATCH)) {
                throw new IllegalStateException("Too many batches");
            }

            //Generate the batches
            for (int batch = 0; batch < batches; batch++) {
                int indexStart = (batch<<QUADS_PER_WORKGROUP_BITS);
                int count = Math.min(quads-indexStart, QUADS_PER_WORKGROUP);
                indexStart += (shiftByHalf?QUADS_PER_WORKGROUP/2:0);
                indexStart += state.getIndexStartIndex();

                MemoryUtil.memPutInt(addr, indexStart);
                MemoryUtil.memPutInt(addr + 4, count);
                MemoryUtil.memPutInt(addr + 8, range.vertexStart() + state.getVertexSegment().getOffset());
                MemoryUtil.memPutInt(addr + 12, section.getChunkId());

                addr += 16;
            }
            batchCount += batches;
        }

        regionManager.getStagingBuffer().enqueueCopy(cl, batchBuilderBuffer, batchBuffer, 0);
        regionManager.getStagingBuffer().flush(cl);
        return batchCount|(needsMultiPass?1<<31:0);
    }
    /*
    //TODO: batches can be any range of any part of any section in the entire region
    // optimize the dispatching and distribution of the batches
    private int setupBatches(boolean shiftByHalf, CommandList cl, RenderRegion.RenderRegionStorage storage, RenderSection... sections) {
        //The total number of batches cannot exceed MAX_BATCHES_PER_DISPATCH, throws an exception if this is reached
        long addr = MemoryUtil.memAddress(batchBuilderBuffer);
        int batchCount = 0;
        boolean needsMultiPass = false;
        for (var section : sections) {
            var range = storage.getState(section).getModelPart(ModelQuadFacing.UNASSIGNED);
            int quads = (range.vertexCount()>>2) - (shiftByHalf?QUADS_PER_WORKGROUP/2:0);
            if (quads > 256) {
                needsMultiPass = true;
            }

            //Dont resort if the original quad count is < QUADS_PER_WORKGROUP
            if (shiftByHalf && quads < QUADS_PER_WORKGROUP/2) {
                continue;
            }

            int batches = (quads>>QUADS_PER_WORKGROUP_BITS);
            if ((batchCount+batches)>(QUADS_PER_WORKGROUP*MAX_BATCHES_PER_DISPATCH)) {
                throw new IllegalStateException("Too many batches");
            }


            if ((quads - (batches << QUADS_PER_WORKGROUP_BITS)) > (QUADS_PER_WORKGROUP >> 1)||((quads - (batches << QUADS_PER_WORKGROUP_BITS))>0 && shiftByHalf)) {//We sadly need another batch
                batches++;
            }


            //Generate the batches
            for (int batch = 0; batch < batches; batch++) {
                int indexStart = (batch<<QUADS_PER_WORKGROUP_BITS) + (shiftByHalf?QUADS_PER_WORKGROUP/2:0);
                int count = Math.min(quads-indexStart, QUADS_PER_WORKGROUP);
                indexStart += range.indexStart()/6;

                MemoryUtil.memPutInt(addr, indexStart);
                MemoryUtil.memPutInt(addr + 4, count);
                MemoryUtil.memPutInt(addr + 8, range.vertexStart());
                MemoryUtil.memPutInt(addr + 12, section.getChunkId());

                addr += 16;
            }
            batchCount += batches;
        }

        regionManager.getStagingBuffer().enqueueCopy(cl, batchBuilderBuffer, batchBuffer, 0);
        regionManager.getStagingBuffer().flush(cl);
        return batchCount|(needsMultiPass?1<<31:0);
    }*/

    private void partiallySortRegion(CommandList cl, Vector3f origin, RenderRegion region, RenderSection section) {
        var storage = region.getStorage(DefaultTerrainRenderPasses.TRANSLUCENT);
        /*
        storage.getStates()
                .filter(ChunkGraphicsState::hasIndexSegment)
                .toList()
                .toArray(new RenderSection[0]);
         */
        int batches = setupBatches(false, cl, storage, section);
        sorter.getInterface().execute(origin, region, batches&(-1>>>1), batchBuffer);
        if ((batches&(1<<31)) != 0) {
            //Needs multi pass
            batches = setupBatches(true, cl, storage, section);
            sorter.getInterface().execute(origin, region, batches&(-1>>>1), batchBuffer);
        }
    }

    public void partiallySort(Vector3f sortOrigin, RenderSection section) {
        sorter.bind();
        partiallySortRegion(device.createCommandList(), sortOrigin, regionManager.getRegion(section.getRegionId()), section);
    }

    public void delete(CommandList commandList) {
        sorter.delete();
        commandList.deleteBuffer(batchBuffer);
    }
}
