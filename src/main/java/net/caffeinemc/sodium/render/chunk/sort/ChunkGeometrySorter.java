package net.caffeinemc.sodium.render.chunk.sort;

import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import java.util.Arrays;
import java.util.List;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.gfx.api.sync.Fence;
import net.caffeinemc.sodium.SodiumClientMod;
import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.render.chunk.draw.ChunkCameraContext;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPass;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPassManager;
import net.caffeinemc.sodium.render.chunk.state.ChunkPassModel;

public class ChunkGeometrySorter {
    private final RenderDevice device;
    private final ChunkRenderPass[] translucentPasses;
    
    private final float angleThreshold;
    
    private final Long2ReferenceMap<SortNode> sortNodes;
    
    public ChunkGeometrySorter(RenderDevice device, ChunkRenderPassManager renderPassManager, float angleThreshold) {
        this.device = device;
        this.translucentPasses = Arrays.stream(renderPassManager.getAllRenderPasses())
                                       .filter(ChunkRenderPass::isTranslucent)
                                       .toArray(ChunkRenderPass[]::new);
        this.angleThreshold = (float) Math.cos(angleThreshold);
        // make an estimate for size based on inputs (render distance?)
        this.sortNodes = new Long2ReferenceOpenHashMap<>();
    }
    
    public void sortGeometry(List<RenderSection> sortedSections, ChunkCameraContext camera) {
        for (ChunkRenderPass renderPass : this.translucentPasses) {
            int passId = renderPass.getId();
    
            for (RenderSection section : sortedSections) {
                ChunkPassModel model = section.getData().models[passId];
    
                if (model == null) {
                    // section doesn't have a model for this render pass
                    continue;
                }
                
                long key = createKey(section, passId);
                SortNode sortNode = this.sortNodes.computeIfAbsent(key, unused -> new SortNode(
                        section.getChunkX(),
                        section.getChunkY(),
                        section.getChunkZ()
                ));
    
                if (sortNode.isProcessing()) {
                    // sort is not done, queue for later?
                    continue;
                }
    
                boolean requiresSort = sortNode.getSortVectors().compareAndSet(camera, this.angleThreshold);
    
                if (requiresSort) {
                    sortNode.setFence(this.device.createFence());
        
//                    SodiumClientMod.logger()
//                                   .info(
//                                           "Sort initiated for {}, {}, {}",
//                                           section.getChunkX(),
//                                           section.getChunkY(),
//                                           section.getChunkZ()
//                                   );
                }
            }
        }
    }
    
    public void removeSection(RenderSection section) {
        for (ChunkRenderPass pass : this.translucentPasses) {
            long key = createKey(section, pass.getId());
            this.sortNodes.remove(key);
        }
    }
    
    private static long createKey(RenderSection section, int passId) {
        return ((long) section.id() << 32) | passId;
    }
    
    public static final class SortNode {
        private final SectionSortVectors sortVectors;
        private Fence fence;
        
        public SortNode(int sectionX, int sectionY, int sectionZ) {
            this.sortVectors = new SectionSortVectors(sectionX, sectionY, sectionZ);
        }
        
        public boolean isProcessing() {
            if (this.fence == null) {
                return false;
            }
            return !this.fence.poll();
        }
        
        public SectionSortVectors getSortVectors() {
            return this.sortVectors;
        }
        
        public void setFence(Fence fence) {
            this.fence = fence;
        }
    }
}
