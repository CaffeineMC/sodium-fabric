package net.caffeinemc.sodium.render.chunk.sort;

import it.unimi.dsi.fastutil.objects.ObjectArrays;
import java.util.Arrays;
import java.util.List;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.gfx.api.sync.Fence;
import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.render.chunk.draw.ChunkCameraContext;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPass;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPassManager;
import net.caffeinemc.sodium.render.chunk.state.ChunkPassModel;

public class ChunkGeometrySorter {
    private final RenderDevice device;
    private final ChunkRenderPass[] translucentPasses;
    private final int passCount;
    
    private final float angleThreshold;
    
    private SortNode[] nodes;
    
    public ChunkGeometrySorter(RenderDevice device, ChunkRenderPassManager renderPassManager, float angleThreshold) {
        this.device = device;
        this.translucentPasses = Arrays.stream(renderPassManager.getAllRenderPasses())
                                       .filter(ChunkRenderPass::isTranslucent)
                                       .toArray(ChunkRenderPass[]::new);
        this.passCount = this.translucentPasses.length;
        this.angleThreshold = (float) Math.cos(angleThreshold);
        // make an estimate for size based on inputs (render distance?)
        this.nodes = new SortNode[4096 * this.passCount];
    }
    
    public void sortGeometry(List<RenderSection> sortedSections, ChunkCameraContext camera) {
        for (int translucentPassId = 0; translucentPassId < this.translucentPasses.length; translucentPassId++) {
            ChunkRenderPass renderPass = this.translucentPasses[translucentPassId];
            int passId = renderPass.getId();
    
            for (RenderSection section : sortedSections) {
                ChunkPassModel model = section.getData().models[passId];
    
                if (model == null) {
                    // section doesn't have a model for this render pass
                    continue;
                }
                
                int key = this.createKey(section, translucentPassId);
                SortNode sortNode = this.computeNodeIfAbsent(key, section);
    
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
        for (int translucentPassId = 0; translucentPassId < this.translucentPasses.length; translucentPassId++) {
            int key = this.createKey(section, translucentPassId);
            if (key >= this.nodes.length) {
                // out of bounds, skip
                continue;
            }
            SortNode node = this.nodes[key];
            if (node != null) {
                node.delete();
            }
            this.nodes[key] = null;
        }
    }
    
    private int createKey(RenderSection section, int passId) {
        return (section.id() * this.passCount) + passId;
    }
    
    private SortNode computeNodeIfAbsent(int key, RenderSection section) {
        if (this.nodes.length <= key) {
            this.nodes = ObjectArrays.grow(this.nodes, key + 1);
        } else {
            SortNode existingNode = this.nodes[key];
            if(existingNode != null) {
                return existingNode;
            }
        }
        SortNode sortNode = new SortNode(section.getChunkX(), section.getChunkY(), section.getChunkZ());
        this.nodes[key] = sortNode;
        return sortNode;
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
        
        public void delete() {
            this.fence.delete();
        }
        
        public SectionSortVectors getSortVectors() {
            return this.sortVectors;
        }
        
        public void setFence(Fence fence) {
            this.fence = fence;
        }
    }
}
