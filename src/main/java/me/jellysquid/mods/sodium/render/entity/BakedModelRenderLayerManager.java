package me.jellysquid.mods.sodium.render.entity;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.interop.vanilla.layer.MultiPhaseAccessor;
import me.jellysquid.mods.sodium.interop.vanilla.layer.MultiPhaseParametersAccessor;
import me.jellysquid.mods.sodium.interop.vanilla.layer.RenderLayerAccessor;
import me.jellysquid.mods.sodium.interop.vanilla.layer.RenderPhaseShaderAccessor;
import net.minecraft.client.render.*;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class BakedModelRenderLayerManager {
    private static final Map<RenderLayer, RenderLayer> dumbToSmart = new Object2ObjectOpenHashMap<>();
    private static Map<Shader, Shader> SHADER_CONVERSION_MAP;

    @SuppressWarnings("ConstantConditions")
    public static RenderLayer tryDeriveSmartRenderLayer(@Nullable RenderLayer dumbRenderLayer) {
        if (dumbRenderLayer == null) {
            return null;
        }

        RenderLayer convertedRenderLayer = null;
        // we check if it is contained here because null may be the intentional output for incompatible layers.
        if (dumbToSmart.containsKey(dumbRenderLayer)) {
            return dumbToSmart.get(dumbRenderLayer);
        } else if (dumbRenderLayer instanceof MultiPhaseAccessor dumbMultiPhaseRenderPass) {
            MultiPhaseParametersAccessor dumbMultiPhaseParameters = ((MultiPhaseParametersAccessor) (Object) dumbMultiPhaseRenderPass.getPhases());
            Optional<Supplier<Shader>> possibleSupplier = ((RenderPhaseShaderAccessor) dumbMultiPhaseParameters.getShader()).getSupplier();
            if (possibleSupplier.isPresent()) {
                Shader dumbShader = possibleSupplier.get().get();
                if (dumbShader != null) {
                    if (SHADER_CONVERSION_MAP == null) {
                        SHADER_CONVERSION_MAP = Map.of(
                                GameRenderer.getRenderTypeEntityCutoutNoNullShader(), BakedModelShaderManager.ENTITY_CUTOUT_NO_CULL_INSTANCED,
                                GameRenderer.getRenderTypeEntityTranslucentShader(), BakedModelShaderManager.ENTITY_TRANSLUCENT_BATCHED
                        );
                    }

                    Shader convertedShader = SHADER_CONVERSION_MAP.get(dumbShader);
                    if (convertedShader != null && dumbRenderLayer instanceof RenderLayerAccessor dumbRenderLayerAccessor) {
                        RenderLayer.MultiPhaseParameters phaseParameters = RenderLayer.MultiPhaseParameters.builder()
                                .cull(dumbMultiPhaseParameters.getCull())
                                .depthTest(dumbMultiPhaseParameters.getDepthTest())
                                .layering(dumbMultiPhaseParameters.getLayering())
                                .lightmap(dumbMultiPhaseParameters.getLightmap())
                                .lineWidth(dumbMultiPhaseParameters.getLineWidth())
                                .overlay(dumbMultiPhaseParameters.getOverlay())
                                .shader(new RenderPhase.Shader(() -> convertedShader))
                                .target(dumbMultiPhaseParameters.getTarget())
                                .texture(dumbMultiPhaseParameters.getTexture())
                                .texturing(dumbMultiPhaseParameters.getTexturing())
                                .transparency(dumbMultiPhaseParameters.getTransparency())
                                .writeMaskState(dumbMultiPhaseParameters.getWriteMaskState())
                                .build(dumbMultiPhaseParameters.getOutlineMode());

                        convertedRenderLayer = new RenderLayer.MultiPhase(
                                dumbRenderLayerAccessor.getName(),
                                convertedShader.getFormat(),
                                dumbRenderLayer.getDrawMode(),
                                dumbRenderLayer.getExpectedBufferSize(),
                                dumbRenderLayer.hasCrumbling(),
                                dumbRenderLayerAccessor.getTranslucent(),
                                phaseParameters
                        ) {
                            /**
                             * Minecraft spends a long time with the startDrawing and endDrawing setting opengl variables and such.
                             * We don't want that because we know the vertex count will always be 0.
                             */
                            @Override
                            public void draw(BufferBuilder buffer, int cameraX, int cameraY, int cameraZ) {
                                if (buffer.isBuilding()) {
                                    // deliberately don't sort geometry, we do it ourselves when needed

                                    buffer.end();
                                }
                            }
                        };
                    }
                }
            }
        }

        dumbToSmart.put(dumbRenderLayer, convertedRenderLayer);
        return convertedRenderLayer;
    }
}
