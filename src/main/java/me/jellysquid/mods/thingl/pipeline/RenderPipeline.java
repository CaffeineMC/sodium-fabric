package me.jellysquid.mods.thingl.pipeline;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.thingl.pipeline.options.ColorMask;
import me.jellysquid.mods.thingl.pipeline.options.CullingMode;
import me.jellysquid.mods.thingl.pipeline.options.DepthFunc;
import me.jellysquid.mods.thingl.pipeline.options.TranslucencyMode;

public class RenderPipeline {
    private final CullingMode cullingMode;
    private final TranslucencyMode translucencyMode;
    private final DepthFunc depthFunc;
    private final boolean depthEnabled;
    private final boolean depthMask;
    private final ColorMask colorMask;

    private RenderPipeline(CullingMode cullingMode, TranslucencyMode translucencyMode, DepthFunc depthFunc, boolean depthEnabled, boolean depthMask, ColorMask colorMask) {
        this.cullingMode = cullingMode;
        this.translucencyMode = translucencyMode;
        this.depthFunc = depthFunc;
        this.depthEnabled = depthEnabled;
        this.depthMask = depthMask;
        this.colorMask = colorMask;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static RenderPipeline defaults() {
        return builder()
                .build();
    }

    public void enable() {
        if (this.cullingMode == CullingMode.ENABLE) {
            RenderSystem.enableCull();
        } else {
            RenderSystem.disableCull();
        }

        if (this.translucencyMode == TranslucencyMode.ENABLED) {
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        } else {
            RenderSystem.disableBlend();
        }

        RenderSystem.depthMask(this.depthMask);
        RenderSystem.colorMask(this.colorMask.red, this.colorMask.green, this.colorMask.blue, this.colorMask.alpha);

        if (this.depthEnabled) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(this.depthFunc.id);
        } else {
            RenderSystem.disableDepthTest();
        }
    }

    public static class Builder {
        private CullingMode cullingMode = CullingMode.ENABLE;
        private TranslucencyMode translucencyMode = TranslucencyMode.DISABLED;
        private DepthFunc depthFunc = DepthFunc.LESS_THAN_OR_EQUAL;
        private boolean depthEnabled = true;
        private boolean depthMask = true;
        private ColorMask colorMask = new ColorMask(true, true, true, true);

        public Builder setCullingMode(CullingMode mode) {
            this.cullingMode = mode;
            return this;
        }

        public Builder setTranslucencyMode(TranslucencyMode mode) {
            this.translucencyMode = mode;
            return this;
        }

        public Builder setDepthFunc(DepthFunc func) {
            this.depthFunc = func;
            return this;
        }

        public Builder setDepthEnabled(boolean enabled) {
            this.depthEnabled = enabled;
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

        public RenderPipeline build() {
            return new RenderPipeline(this.cullingMode, this.translucencyMode,
                    this.depthFunc, this.depthEnabled, this.depthMask, this.colorMask);
        }
    }
}
