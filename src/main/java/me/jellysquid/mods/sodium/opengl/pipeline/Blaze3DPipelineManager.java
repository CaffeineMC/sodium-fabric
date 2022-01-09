package me.jellysquid.mods.sodium.opengl.pipeline;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.sodium.opengl.sampler.Sampler;
import me.jellysquid.mods.sodium.opengl.types.CullingMode;
import me.jellysquid.mods.sodium.opengl.types.DepthFunc;
import me.jellysquid.mods.sodium.opengl.types.RenderState;
import net.minecraft.client.render.BufferRenderer;
import org.lwjgl.opengl.GL45C;

import java.util.BitSet;
import java.util.function.Consumer;

public class Blaze3DPipelineManager implements PipelineManager {
    private final Blaze3DPipelineState state = new Blaze3DPipelineState();

    @Override
    public <ARRAY extends Enum<ARRAY>, PROGRAM> void bindPipeline(Pipeline<PROGRAM, ARRAY> pipeline, Consumer<PipelineState> gate) {
        BufferRenderer.vertexFormat = null;

        GlStateManager._glUseProgram(pipeline.getProgram().handle());
        GlStateManager._glBindVertexArray(pipeline.getVertexArray().handle());

        setRenderState(pipeline.getState());

        try {
            gate.accept(this.state);
        } finally {
            this.state.restore();
        }

        unsetRenderState(pipeline.getState());
    }

    private static void setRenderState(RenderState renderState) {
        if (renderState.cullingMode == CullingMode.ENABLE) {
            RenderSystem.enableCull();
        } else {
            RenderSystem.disableCull();
        }

        if (renderState.blendFunction != null) {
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(renderState.blendFunction.srcRGB, renderState.blendFunction.dstRGB,
                    renderState.blendFunction.srcAlpha, renderState.blendFunction.dstAlpha);
        } else {
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        }

        if (!renderState.writeMask.depth()) {
            RenderSystem.depthMask(false);
        }

        if (!renderState.writeMask.color()) {
            RenderSystem.colorMask(false, false, false, false);
        }

        if (renderState.depthFunc != DepthFunc.ALWAYS) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(renderState.depthFunc.id);
        } else {
            RenderSystem.disableDepthTest();
        }
    }


    private static void unsetRenderState(RenderState renderState) {
        // VANILLA BUG: Some code paths rely on the depth function being GL_LEQUAL by default
        // Not setting this causes graphical corruption when certain effects are being rendered (i.e. items with glint)
        // If we changed the depth function, we need to be sure to restore it
        if (renderState.depthFunc != DepthFunc.ALWAYS) {
            RenderSystem.depthFunc(GL45C.GL_LEQUAL);
        }

        // VANILLA BUG: Depth and color masks must be reset to their default value of true for each
        if (!renderState.writeMask.depth()) {
            RenderSystem.depthMask(true);
        }

        if (!renderState.writeMask.color()) {
            RenderSystem.colorMask(true, true, true, true);
        }
    }

    private static class Blaze3DPipelineState implements PipelineState {
        private final BitSet changedTextures = new BitSet(GL45C.glGetInteger(GL45C.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS));

        @Override
        public void bindTexture(int unit, int texture, Sampler sampler) {
            this.changedTextures.set(unit);

            GL45C.glBindTextureUnit(unit, texture);
            GL45C.glBindSampler(unit, sampler.handle());
        }

        public void restore() {
            for (int unit = this.changedTextures.nextSetBit(0); unit != -1; unit = this.changedTextures.nextSetBit(unit + 1)) {
                GL45C.glBindTextureUnit(unit, GlStateManager.TEXTURES[unit].boundTexture);
                GL45C.glBindSampler(unit, 0);
            }

            this.changedTextures.clear();
        }
    }
}
