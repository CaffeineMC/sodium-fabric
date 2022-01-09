package me.jellysquid.mods.sodium.opengl.types;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;

public class RenderState {
    public final CullingMode cullingMode;
    @Nullable public final BlendFunction blendFunction;
    public final DepthFunc depthFunc;
    public final WriteMask writeMask;

    private RenderState(CullingMode cullingMode, @Nullable BlendFunction blendFunction, DepthFunc depthFunc, WriteMask writeMask) {
        this.cullingMode = Validate.notNull(cullingMode);
        this.blendFunction = blendFunction;
        this.depthFunc = Validate.notNull(depthFunc);
        this.writeMask = Validate.notNull(writeMask);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static RenderState defaults() {
        return builder().build();
    }

    public static class Builder {
        private CullingMode cullingMode = CullingMode.ENABLE;
        private BlendFunction blendFunction = null;
        private DepthFunc depthFunc = DepthFunc.LESS_THAN_OR_EQUAL;
        private WriteMask writeMask = new WriteMask(true, true);

        public Builder setCullingMode(CullingMode mode) {
            this.cullingMode = mode;
            return this;
        }

        public Builder setBlendFunction(BlendFunction mode) {
            this.blendFunction = mode;
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

        public RenderState build() {
            return new RenderState(this.cullingMode, this.blendFunction,
                    this.depthFunc, this.writeMask);
        }
    }
}
