package net.caffeinemc.sodium.render.chunk.passes;

import java.util.Objects;
import net.caffeinemc.gfx.api.pipeline.RenderPipelineDescription;

public final class ChunkRenderPass {
    private final RenderPipelineDescription pipelineDescription;
    private final boolean mipped;
    private final float alphaCutoff;
    
    private int id;
    
    public ChunkRenderPass(RenderPipelineDescription pipelineDescription, boolean mipped, float alphaCutoff) {
        this.pipelineDescription = pipelineDescription;
        this.mipped = mipped;
        this.alphaCutoff = alphaCutoff;
    }
    
    public RenderPipelineDescription getPipelineDescription() {
        return this.pipelineDescription;
    }
    
    public boolean isMipped() {
        return this.mipped;
    }
    
    public float getAlphaCutoff() {
        return this.alphaCutoff;
    }
    
    void setId(int id) {
        this.id = id;
    }
    
    public int getId() {
        return this.id;
    }
    
    public boolean isTranslucent() {
        return this.pipelineDescription.blendFunc != null;
    }
    
    public boolean isCutout() {
        return this.alphaCutoff != 0.0f;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ChunkRenderPass) obj;
        return Objects.equals(this.pipelineDescription, that.pipelineDescription) &&
               this.mipped == that.mipped &&
               Float.floatToIntBits(this.alphaCutoff) == Float.floatToIntBits(that.alphaCutoff);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(this.pipelineDescription, this.mipped, this.alphaCutoff);
    }
    
}
