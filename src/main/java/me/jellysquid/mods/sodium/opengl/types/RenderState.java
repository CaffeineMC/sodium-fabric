package me.jellysquid.mods.sodium.opengl.types;

public class RenderState {
    public final CullingMode cullingMode;
    public final BlendFunction blendFunction;
    public final DepthFunc depthFunc;
    public final boolean depthMask;
    public final ColorMask colorMask;

    private RenderState(CullingMode cullingMode, BlendFunction blendFunction, DepthFunc depthFunc, boolean depthMask, ColorMask colorMask) {
        this.cullingMode = cullingMode;
        this.blendFunction = blendFunction;
        this.depthFunc = depthFunc;
        this.depthMask = depthMask;
        this.colorMask = colorMask;
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
        private boolean depthMask = true;
        private ColorMask colorMask = new ColorMask(true, true, true, true);

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

        public Builder setDepthMask(boolean mask) {
            this.depthMask = mask;
            return this;
        }

        public Builder setColorMask(ColorMask mask) {
            this.colorMask = mask;
            return this;
        }

        public RenderState build() {
            return new RenderState(this.cullingMode, this.blendFunction,
                    this.depthFunc, this.depthMask, this.colorMask);
        }
    }
}
