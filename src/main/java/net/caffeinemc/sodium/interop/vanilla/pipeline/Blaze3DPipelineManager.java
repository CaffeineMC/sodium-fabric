package net.caffeinemc.sodium.interop.vanilla.pipeline;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.function.Consumer;
import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.api.device.RenderConfiguration;
import net.caffeinemc.gfx.api.device.RenderDeviceProperties;
import net.caffeinemc.gfx.api.pipeline.Pipeline;
import net.caffeinemc.gfx.api.pipeline.PipelineDescription;
import net.caffeinemc.gfx.api.pipeline.PipelineState;
import net.caffeinemc.gfx.api.pipeline.state.CullMode;
import net.caffeinemc.gfx.api.pipeline.state.DepthFunc;
import net.caffeinemc.gfx.api.shader.BufferBlock;
import net.caffeinemc.gfx.api.texture.Sampler;
import net.caffeinemc.gfx.api.texture.Texture;
import net.caffeinemc.gfx.opengl.GlEnum;
import net.caffeinemc.gfx.opengl.array.GlVertexArray;
import net.caffeinemc.gfx.opengl.buffer.GlBuffer;
import net.caffeinemc.gfx.opengl.pipeline.GlPipelineManager;
import net.caffeinemc.gfx.opengl.shader.GlProgram;
import net.caffeinemc.gfx.opengl.texture.GlSampler;
import net.caffeinemc.gfx.opengl.texture.GlTexture;
import net.minecraft.client.render.BufferRenderer;
import org.apache.commons.lang3.Validate;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GL45C;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public class Blaze3DPipelineManager implements GlPipelineManager {
    private final Blaze3DPipelineState state;

    public Blaze3DPipelineManager(RenderDeviceProperties properties) {
        this.state = new Blaze3DPipelineState(properties.values.maxCombinedTextureImageUnits);
    }

    @Override
    public <ARRAY extends Enum<ARRAY>, PROGRAM> void bindPipeline(Pipeline<PROGRAM, ARRAY> pipeline, Consumer<PipelineState> gate) {
        // FIXME: why is this here? used to null the current buffer renderer vertex format.
        BufferRenderer.resetCurrentVertexBuffer();

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

        private final int maxTextureUnits;

        public Blaze3DPipelineState(int maxTextureUnits) {
            this.maxTextureUnits = maxTextureUnits;
        }

        @Override
        public void bindTexture(int unit, Texture texture, Sampler sampler) {
            if (RenderConfiguration.API_CHECKS) {
                Validate.isTrue(unit >= 0 && unit < this.maxTextureUnits, "Texture unit index is invalid");
            }

            GL45C.glBindTextureUnit(unit, GlTexture.getHandle(texture));
            GL45C.glBindSampler(unit, GlSampler.getHandle(sampler));
        }

        @Override
        public void bindBufferBlock(BufferBlock block, Buffer buffer) {
            this.bindBufferBlock(block, buffer, 0, buffer.capacity());
        }

        @Override
        public void bindBufferBlock(BufferBlock block, Buffer buffer, long offset, long length) {
            if (RenderConfiguration.API_CHECKS) {
                Validate.isTrue(offset >= 0, "Offset must be greater-than or equal to zero");
//                Validate.isTrue(offset + length <= buffer.capacity(), "Range is out of buffer bounds");
            }

            GL32C.glBindBufferRange(GlEnum.from(block.type()), block.index(), GlBuffer.getHandle(buffer), offset, length);
        }

        public void restore() {
            try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                int textureCount = GlStateManager.TEXTURE_COUNT;
                long texturesPtr = memoryStack.nmalloc(Integer.BYTES * textureCount);
                for (int unit = 0; unit < textureCount; unit++) {
                    MemoryUtil.memPutInt(texturesPtr + (unit * Integer.BYTES), GlStateManager.TEXTURES[unit].boundTexture);
                }
                GL45C.nglBindTextures(0, textureCount, texturesPtr);
            }

            GL45C.nglBindSamplers(0, this.maxTextureUnits, MemoryUtil.NULL);
        }
    }
}
