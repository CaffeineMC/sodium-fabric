package me.jellysquid.mods.sodium.mixin.core;

import com.mojang.blaze3d.platform.GlStateManager;
import me.jellysquid.mods.sodium.interop.vanilla.mixin.*;
import me.jellysquid.mods.sodium.opengl.array.VertexArrayDescription;
import me.jellysquid.mods.sodium.opengl.array.VertexArrayResourceBinding;
import me.jellysquid.mods.sodium.opengl.attribute.VertexAttribute;
import me.jellysquid.mods.sodium.opengl.attribute.VertexAttributeBinding;
import me.jellysquid.mods.sodium.opengl.device.RenderDevice;
import me.jellysquid.mods.sodium.opengl.pipeline.Pipeline;
import me.jellysquid.mods.sodium.opengl.shader.Program;
import me.jellysquid.mods.sodium.opengl.types.*;
import me.jellysquid.mods.sodium.render.immediate.RenderImmediate;
import me.jellysquid.mods.sodium.render.immediate.VanillaShaderInterface;
import net.minecraft.client.render.*;
import org.apache.commons.lang3.Validate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;

@Mixin(RenderLayer.class)
public class MixinRenderLayer {
    @Mixin(targets = "net/minecraft/client/render/RenderLayer$MultiPhaseParameters")
    public static class MixinMultiPhaseParameters implements MultiPhaseParametersExtended {
        @Shadow
        @Final
        private RenderPhase.TextureBase texture;

        @Shadow
        @Final
        private RenderPhase.Shader shader;

        @Shadow
        @Final
        private RenderPhase.Transparency transparency;

        @Shadow
        @Final
        private RenderPhase.DepthTest depthTest;

        @Shadow
        @Final
        private RenderPhase.Cull cull;

        @Shadow
        @Final
        private RenderPhase.Lightmap lightmap;

        @Shadow
        @Final
        private RenderPhase.Overlay overlay;

        @Shadow
        @Final
        private RenderPhase.Layering layering; // TODO: implement

        @Shadow
        @Final
        private RenderPhase.Target target; // TODO: implement

        @Shadow
        @Final
        private RenderPhase.Texturing texturing; // TODO: implement

        @Shadow
        @Final
        private RenderPhase.WriteMaskState writeMaskState;

        @Shadow
        @Final
        private RenderPhase.LineWidth lineWidth; // TODO: implement

        @Override
        public RenderState createRenderState() {
            return RenderState.builder()
                    .setBlendFunction(createBlendFunction(this.transparency))
                    .setDepthFunc(createDepthFunction(this.depthTest))
                    .setCullingMode(createCullingMode(this.cull))
                    .setWriteMask(createWriteMask(this.writeMaskState))
                    .build();
        }

        @Override
        public Program<VanillaShaderInterface> createProgram() {
            if (this.shader instanceof RenderPhaseShaderAccess access) {
                var supplier = access.getShader();
                var shader = supplier.get();

                Validate.notNull(shader, "Shader program not available");

                if (shader instanceof ShaderExtended extended) {
                    return extended.sodium$getShader();
                } else {
                    throw new UnsupportedOperationException("Shader doesn't implement required interface");
                }
            }

            throw new UnsupportedOperationException("Unsupported shader value");
        }

        @Override
        public ShaderTexture[] createShaderTextures() {
            var textures = new ShaderTexture[12];

            if (this.overlay instanceof ShaderTextureProvider provider) {
                populateTextures(textures, provider.getTextures());
            }

            if (this.lightmap instanceof ShaderTextureProvider provider) {
                populateTextures(textures, provider.getTextures());
            }

            if (this.texture instanceof ShaderTextureProvider provider) {
                populateTextures(textures, provider.getTextures());
            }

            return textures;
        }

        private static void populateTextures(ShaderTexture[] outputs, List<ShaderTexture> inputs) {
            for (var input : inputs) {
                outputs[input.target()] = input;
            }
        }

        private static WriteMask createWriteMask(RenderPhase.WriteMaskState value) {
            return new WriteMask(value.color, value.depth);
        }

        private static CullingMode createCullingMode(RenderPhase.Cull value) {
            if (value == RenderPhase.ENABLE_CULLING) {
                return CullingMode.ENABLE;
            } else if (value == RenderPhase.DISABLE_CULLING) {
                return CullingMode.DISABLE;
            }

            throw new UnsupportedOperationException();
        }

        private static DepthFunc createDepthFunction(RenderPhase.DepthTest value) {
            if (value == RenderPhase.ALWAYS_DEPTH_TEST) {
                return DepthFunc.ALWAYS;
            } else if (value == RenderPhase.EQUAL_DEPTH_TEST) {
                return DepthFunc.EQUAL;
            } else if (value == RenderPhase.LEQUAL_DEPTH_TEST) {
                return DepthFunc.LESS_THAN_OR_EQUAL;
            }

            throw new UnsupportedOperationException();
        }

        private static BlendFunction createBlendFunction(RenderPhase.Transparency value) {
            if (value == RenderPhase.NO_TRANSPARENCY) {
                return null;
            } else if (value == RenderPhase.ADDITIVE_TRANSPARENCY) {
                return BlendFunction.of(GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE);
            } else if (value == RenderPhase.LIGHTNING_TRANSPARENCY) {
                return BlendFunction.of(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            } else if (value == RenderPhase.GLINT_TRANSPARENCY) {
                return BlendFunction.of(GlStateManager.SrcFactor.SRC_COLOR, GlStateManager.DstFactor.ONE, GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE);
            } else if (value == RenderPhase.CRUMBLING_TRANSPARENCY) {
                return BlendFunction.of(GlStateManager.SrcFactor.DST_COLOR, GlStateManager.DstFactor.SRC_COLOR, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
            } else if (value == RenderPhase.TRANSLUCENT_TRANSPARENCY) {
                return BlendFunction.of(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
            }

            throw new UnsupportedOperationException();
        }
    }

    @Mixin(targets = "net/minecraft/client/render/RenderLayer$MultiPhase")
    public static class MixinMultiPhase extends RenderLayer {
        @Shadow
        @Final
        private MultiPhaseParameters phases;

        private Pipeline<VanillaShaderInterface, VanillaShaderInterface.BufferTarget> pipeline;
        private ShaderTexture[] textures;

        public MixinMultiPhase(String name, VertexFormat vertexFormat, VertexFormat.DrawMode drawMode, int expectedBufferSize, boolean hasCrumbling, boolean translucent, Runnable startAction, Runnable endAction) {
            super(name, vertexFormat, drawMode, expectedBufferSize, hasCrumbling, translucent, startAction, endAction);
        }

        @Override
        public void draw(BufferBuilder buffer, int cameraX, int cameraY, int cameraZ) {
            if (this.pipeline != null && !this.pipeline.getProgram().isHandleValid()) {
                this.destroy();
            }

            if (this.pipeline == null) {
                this.init();
            }

            if (buffer.isBuilding()) {
                if (this.pipeline.getState().blendFunction != null) {
                    buffer.setCameraPosition((float) cameraX, (float) cameraY, (float) cameraZ);
                }

                buffer.end();

                var data = buffer.popData();
                var params = data.getFirst();

                RenderImmediate.getInstance()
                        .draw(this.pipeline, this.textures, data.getSecond(), params.getMode(), params.getVertexFormat(), params.getCount(), params.getElementFormat(), params.getVertexCount(), params.isTextured());
            }
        }

        private void destroy() {
            RenderDevice.INSTANCE.deletePipeline(this.pipeline);
            this.pipeline = null;
            this.textures = null;
        }

        private void init() {
            if ((Object) this.phases instanceof MultiPhaseParametersExtended params) {
                this.pipeline = this.createPipeline(params.createRenderState(), params.createProgram());
                this.textures = params.createShaderTextures();
            } else {
                throw new UnsupportedOperationException("Not able to convert multi-phase parameters into pipeline");
            }
        }

        private Pipeline<VanillaShaderInterface, VanillaShaderInterface.BufferTarget> createPipeline(RenderState renderState, Program<VanillaShaderInterface> program) {
            var vertexArray = new VertexArrayDescription<>(VanillaShaderInterface.BufferTarget.values(),
                    List.of(new VertexArrayResourceBinding<>(VanillaShaderInterface.BufferTarget.VERTICES, createVanillaVertexBindings(this.getVertexFormat()))));

            return RenderDevice.INSTANCE.createPipeline(renderState, program, vertexArray);
        }

        private static VertexAttributeBinding[] createVanillaVertexBindings(VertexFormat vertexFormat) {
            var elements = vertexFormat.getElements();
            var bindings = new ArrayList<VertexAttributeBinding>();

            for (int i = 0; i < elements.size(); i++) {
                VertexFormatElement element = elements.get(i);

                if (element.getType() == VertexFormatElement.Type.PADDING) {
                    continue;
                }

                var format = element.getDataType().getId();
                var count = element.getLength();
                var size = element.getByteLength();
                var normalized = isVanillaAttributeNormalized(element.getType());
                var intType = isVanillaIntType(element.getType(), element.getDataType());

                var attribute = new VertexAttribute(format, size, count, normalized, vertexFormat.offsets.getInt(i), intType);

                bindings.add(new VertexAttributeBinding(i, attribute));
            }

            return bindings.toArray(VertexAttributeBinding[]::new);
        }

        private static boolean isVanillaIntType(VertexFormatElement.Type type, VertexFormatElement.DataType dataType) {
            return type == VertexFormatElement.Type.UV && dataType != VertexFormatElement.DataType.FLOAT;
        }

        private static boolean isVanillaAttributeNormalized(VertexFormatElement.Type type) {
            return type == VertexFormatElement.Type.NORMAL || type == VertexFormatElement.Type.COLOR;
        }
    }
}
