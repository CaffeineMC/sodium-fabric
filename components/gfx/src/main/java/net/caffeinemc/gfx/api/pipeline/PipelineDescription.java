package net.caffeinemc.gfx.api.pipeline;

import net.caffeinemc.gfx.api.pipeline.state.BlendFunc;
import net.caffeinemc.gfx.api.pipeline.state.CullMode;
import net.caffeinemc.gfx.api.pipeline.state.DepthFunc;
import net.caffeinemc.gfx.api.pipeline.state.WriteMask;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;

public class PipelineDescription {
    public final CullMode cullMode;
    @Nullable public final BlendFunc blendFunc;
    public final DepthFunc depthFunc;
    public final WriteMask writeMask;

    private PipelineDescription(CullMode cullMode, @Nullable BlendFunc blendFunc, DepthFunc depthFunc, WriteMask writeMask) {
        this.cullMode = Validate.notNull(cullMode);
        this.blendFunc = blendFunc;
        this.depthFunc = Validate.notNull(depthFunc);
        this.writeMask = Validate.notNull(writeMask);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static PipelineDescription defaults() {
        return builder().build();
    }

    public static class Builder {
        private CullMode cullMode = CullMode.ENABLE;
        private BlendFunc blendFunc = null;
        private DepthFunc depthFunc = DepthFunc.LESS_THAN_OR_EQUAL;
        private WriteMask writeMask = new WriteMask(true, true);

        public Builder setCullingMode(CullMode mode) {
            this.cullMode = mode;
            return this;
        }

        public Builder setBlendFunction(BlendFunc mode) {
            this.blendFunc = mode;
            return this;
        }

        public Builder setDepthFunc(DepthFunc func) {
            this.depthFunc = func;
            return this;
        }

        public Builder setWriteMask(WriteMask mask) {
            this.writeMask = mask;
            return this;
        }

        public PipelineDescription build() {
            return new PipelineDescription(this.cullMode, this.blendFunc,
                    this.depthFunc, this.writeMask);
        }
    }
}
