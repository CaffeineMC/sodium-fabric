package net.caffeinemc.sodium.render.chunk.sort;

import it.unimi.dsi.fastutil.objects.ObjectArrays;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.gfx.api.pipeline.ComputePipeline;
import net.caffeinemc.gfx.api.pipeline.RenderPipeline;
import net.caffeinemc.gfx.api.shader.Program;
import net.caffeinemc.gfx.api.shader.ShaderDescription;
import net.caffeinemc.gfx.api.shader.ShaderType;
import net.caffeinemc.gfx.api.sync.Fence;
import net.caffeinemc.gfx.api.types.ElementFormat;
import net.caffeinemc.gfx.api.types.PrimitiveType;
import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.render.chunk.draw.AbstractChunkRenderer;
import net.caffeinemc.sodium.render.chunk.draw.ChunkCameraContext;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPass;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPassManager;
import net.caffeinemc.sodium.render.chunk.shader.ChunkShaderInterface;
import net.caffeinemc.sodium.render.chunk.shader.SortShaderInterface;
import net.caffeinemc.sodium.render.chunk.state.ChunkPassModel;
import net.caffeinemc.sodium.render.shader.ShaderConstants;
import net.caffeinemc.sodium.render.shader.ShaderLoader;
import net.caffeinemc.sodium.render.shader.ShaderParser;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class ChunkGeometrySorter {
    private static final int LOCAL_SIZE_X = 1024;
    private static final int LOCAL_BMS = 0;
    private static final int LOCAL_DISPERSE = 1;
    private static final int GLOBAL_FLIP = 2;
    private static final int GLOBAL_DISPERSE = 3;
    
    private final RenderDevice device;
    private final ComputePipeline<SortShaderInterface> computePipeline;
    private final ChunkRenderPass[] translucentPasses;
    private final int passCount;
    
    private final float angleThreshold;
    
    private SortNode[] nodes;
    
    public ChunkGeometrySorter(RenderDevice device, ChunkRenderPassManager renderPassManager, TerrainVertexType vertexType, float angleThreshold) {
        this.device = device;
        this.translucentPasses = Arrays.stream(renderPassManager.getAllRenderPasses())
                                       .filter(ChunkRenderPass::isTranslucent)
                                       .toArray(ChunkRenderPass[]::new);
        this.passCount = this.translucentPasses.length;
        this.angleThreshold = (float) Math.cos(angleThreshold);
        // make an estimate for size based on inputs (render distance?)
        this.nodes = new SortNode[4096 * this.passCount];
    
        var constants = getShaderConstants(vertexType).build();

        var compShader = ShaderParser.parseSodiumShader(
                ShaderLoader.MINECRAFT_ASSETS,
                new Identifier("sodium", "terrain/terrain_sort.comp"),
                constants
        );

        var desc = ShaderDescription.builder()
                                    .addShaderSource(ShaderType.COMPUTE, compShader)
                                    .build();

        Program<SortShaderInterface> program = this.device.createProgram(desc, SortShaderInterface::new);
        this.computePipeline = this.device.createComputePipeline(program);
    }
    
    protected static ShaderConstants.Builder getShaderConstants(TerrainVertexType vertexType) {
        var constants = ShaderConstants.builder();
    
        constants.add("LOCAL_SIZE_X", String.valueOf(LOCAL_SIZE_X));
        constants.add("LOCAL_BMS", String.valueOf(LOCAL_BMS));
        constants.add("LOCAL_DISPERSE", String.valueOf(LOCAL_DISPERSE));
        constants.add("GLOBAL_FLIP", String.valueOf(GLOBAL_FLIP));
        constants.add("GLOBAL_DISPERSE", String.valueOf(GLOBAL_DISPERSE));
        
        if (!MathHelper.approximatelyEquals(vertexType.getVertexRange(), 1.0f)) {
            constants.add("VERT_SCALE", String.valueOf(vertexType.getVertexRange()));
        }
        
        return constants;
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
                    // sort is not done
                    // TODO: queue for later?
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
    
    public void delete() {
        this.device.deleteComputePipeline(this.computePipeline);
        
        Arrays.stream(this.nodes)
              .filter(Objects::nonNull)
              .forEach(SortNode::delete);
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
