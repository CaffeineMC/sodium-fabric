package me.jellysquid.mods.thingl.pipeline;

import me.jellysquid.mods.thingl.pipeline.options.ColorMask;
import me.jellysquid.mods.thingl.pipeline.options.CullingMode;
import me.jellysquid.mods.thingl.pipeline.options.DepthFunc;
import me.jellysquid.mods.thingl.pipeline.options.TranslucencyMode;

public class RenderPipelineParameters {
    private CullingMode cullingMode = CullingMode.ENABLE;
    private TranslucencyMode translucencyMode = TranslucencyMode.DISABLED;
    private DepthFunc depthFunc = DepthFunc.LESS_THAN_OR_EQUAL;
    private boolean depthEnabled = true;
    private boolean depthMask = true;
    private ColorMask colorMask = new ColorMask(true, true, true, true);

    public RenderPipelineParameters setCullingMode(CullingMode mode) {
        this.cullingMode = mode;
        return this;
    }

    public RenderPipelineParameters setTranslucencyMode(TranslucencyMode mode) {
        this.translucencyMode = mode;
        return this;
    }

    public RenderPipelineParameters setDepthFunc(DepthFunc func) {
        this.depthFunc = func;
        return this;
    }

    public RenderPipelineParameters setDepthEnabled(boolean enabled) {
        this.depthEnabled = enabled;
        return this;
    }

    public RenderPipelineParameters setDepthMask(boolean mask) {
        this.depthMask = mask;
        return this;
    }

    public RenderPipelineParameters setColorMask(ColorMask mask) {
        this.colorMask = mask;
        return this;
    }

    public RenderPipeline build() {
        return new RenderPipeline(this.cullingMode, this.translucencyMode,
                this.depthFunc, this.depthEnabled, this.depthMask, this.colorMask);
    }
}
