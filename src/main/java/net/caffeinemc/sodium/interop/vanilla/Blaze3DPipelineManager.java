package net.caffeinemc.sodium.interop.vanilla;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.api.shader.UniformBlock;
import net.caffeinemc.gfx.opengl.array.GlVertexArray;
import net.caffeinemc.gfx.opengl.GlEnum;
import net.caffeinemc.gfx.api.pipeline.Pipeline;
import net.caffeinemc.gfx.api.pipeline.PipelineState;
import net.caffeinemc.gfx.api.texture.Sampler;
import net.caffeinemc.gfx.api.pipeline.state.CullMode;
import net.caffeinemc.gfx.api.pipeline.state.DepthFunc;
import net.caffeinemc.gfx.api.pipeline.PipelineDescription;
import net.caffeinemc.gfx.opengl.buffer.GlBuffer;
import net.caffeinemc.gfx.opengl.pipeline.GlPipelineManager;
import net.caffeinemc.gfx.opengl.shader.GlProgram;
import net.minecraft.client.render.BufferRenderer;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GL45C;

import java.util.BitSet;
import java.util.function.Consumer;

public class Blaze3DPipelineManager implements GlPipelineManager {
    private final Blaze3DPipelineState state = new Blaze3DPipelineState();

    @Override
    public <ARRAY extends Enum<ARRAY>, PROGRAM> void bindPipeline(Pipeline<PROGRAM, ARRAY> pipeline, Consumer<PipelineState> gate) {
        BufferRenderer.vertexFormat = null;

        GL45C.glUseProgram(GlProgram.getHandle(pipeline.getProgram()));
        GL45C.glBindVertexArray(GlVertexArray.getHandle(pipeline.getVertexArray()));

        setRenderState(pipeline.getDescription());

        try {
            gate.accept(this.state);
        } finally {
            this.state.restore();
        }

        unsetRenderState(pipeline.getDescription());
    }

    private static void setRenderState(PipelineDescription desc) {
        if (desc.cullMode == CullMode.ENABLE) {
            RenderSystem.enableCull();
        } else {
            RenderSystem.disableCull();
        }

        if (desc.blendFunc != null) {
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GlEnum.from(desc.blendFunc.srcRGB), GlEnum.from(desc.blendFunc.dstRGB),
                    GlEnum.from(desc.blendFunc.srcAlpha), GlEnum.from(desc.blendFunc.dstAlpha));
        } else {
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        }

        if (!desc.writeMask.depth()) {
            RenderSystem.depthMask(false);
        }

        if (!desc.writeMask.color()) {
            RenderSystem.colorMask(false, false, false, false);
        }

        if (desc.depthFunc != DepthFunc.ALWAYS) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GlEnum.from(desc.depthFunc));
        } else {
            RenderSystem.disableDepthTest();
        }
    }


    private static void unsetRenderState(PipelineDescription pipelineDescription) {
        // VANILLA BUG: Some code paths rely on the depth function being GL_LEQUAL by default
        // Not setting this causes graphical corruption when certain effects are being rendered (i.e. items with glint)
        // If we changed the depth function, we need to be sure to restore it
        if (pipelineDescription.depthFunc != DepthFunc.ALWAYS) {
            RenderSystem.depthFunc(GL45C.GL_LEQUAL);
        }

        // VANILLA BUG: Depth and color masks must be reset to their default value of true for each
        if (!pipelineDescription.writeMask.depth()) {
            RenderSystem.depthMask(true);
        }

        if (!pipelineDescription.writeMask.color()) {
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

        @Override
        public void bindUniformBlock(UniformBlock block, Buffer buffer) {
            this.bindUniformBlock(block, buffer, 0, buffer.getCapacity());
        }

        @Override
        public void bindUniformBlock(UniformBlock block, Buffer buffer, long offset, long length) {
            GL32C.glBindBufferRange(GL32C.GL_UNIFORM_BUFFER, block.binding(), ((GlBuffer) buffer).handle(), offset, length);
        }

        public void restore() {
            // TODO: use multi-bind and just reset everything no matter what? it's probably faster
            for (int unit = this.changedTextures.nextSetBit(0); unit != -1; unit = this.changedTextures.nextSetBit(unit + 1)) {
                GL45C.glBindTextureUnit(unit, GlStateManager.TEXTURES[unit].boundTexture);
                GL45C.glBindSampler(unit, 0);
            }

            this.changedTextures.clear();
        }
    }
}
