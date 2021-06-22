package me.jellysquid.mods.sodium.client.render.chunk;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.DrawCommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlPrimitiveType;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlTessellation;
import me.jellysquid.mods.sodium.client.gl.tessellation.TessellationBinding;
import me.jellysquid.mods.sodium.client.gl.util.ElementRange;
import me.jellysquid.mods.sodium.client.gl.util.GlMultiDrawBatch;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderBounds;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderBindingPoints;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.Map;

public class RegionChunkRenderer extends ShaderChunkRenderer {
    private final GlMultiDrawBatch batch = new GlMultiDrawBatch(ModelQuadFacing.COUNT * RenderRegion.REGION_SIZE);

    private final GlVertexAttributeBinding[] vertexAttributeBindings;

    public RegionChunkRenderer(RenderDevice device, ChunkVertexType vertexType) {
        super(device, vertexType);

        this.vertexAttributeBindings = new GlVertexAttributeBinding[] {
                new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_POSITION,
                        this.vertexFormat.getAttribute(ChunkMeshAttribute.POSITION)),
                new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_COLOR,
                        this.vertexFormat.getAttribute(ChunkMeshAttribute.COLOR)),
                new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_TEX_COORD,
                        this.vertexFormat.getAttribute(ChunkMeshAttribute.TEXTURE)),
                new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_LIGHT_COORD,
                        this.vertexFormat.getAttribute(ChunkMeshAttribute.LIGHT)),
                new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_TRANSLATION,
                        this.vertexFormat.getAttribute(ChunkMeshAttribute.CHUNK_OFFSET))
        };
    }

    @Override
    public void render(MatrixStack matrixStack, CommandList commandList, Map<RenderRegion, List<RenderChunk>> renders, BlockRenderPass pass, ChunkCameraContext camera) {
        super.begin(pass, matrixStack);

        for (Map.Entry<RenderRegion, List<RenderChunk>> entry : renders.entrySet()) {
            RenderRegion region = entry.getKey();
            RenderRegion.RenderRegionArenas arenas = region.getArenas(pass);

            List<RenderChunk> chunks = entry.getValue();

            this.batch.begin();

            for (RenderChunk render : chunks) {
                ChunkGraphicsState state = render.getGraphicsState(pass);

                if (state == null) {
                    continue;
                }

                ChunkRenderBounds bounds = render.getBounds();

                int vertexOffset = state.getVertexSegment().getElementOffset();
                int indexOffset = state.getIndexSegment().getElementOffset();

                this.addDrawCall(state.getModelPart(ModelQuadFacing.UNASSIGNED), vertexOffset, indexOffset);

                if (camera.posY > bounds.y1) {
                    this.addDrawCall(state.getModelPart(ModelQuadFacing.UP), vertexOffset, indexOffset);
                }

                if (camera.posY < bounds.y2) {
                    this.addDrawCall(state.getModelPart(ModelQuadFacing.DOWN), vertexOffset, indexOffset);
                }

                if (camera.posX > bounds.x1) {
                    this.addDrawCall(state.getModelPart(ModelQuadFacing.EAST), vertexOffset, indexOffset);
                }

                if (camera.posX < bounds.x2) {
                    this.addDrawCall(state.getModelPart(ModelQuadFacing.WEST), vertexOffset, indexOffset);
                }

                if (camera.posZ > bounds.z1) {
                    this.addDrawCall(state.getModelPart(ModelQuadFacing.SOUTH), vertexOffset, indexOffset);
                }

                if (camera.posZ < bounds.z2) {
                    this.addDrawCall(state.getModelPart(ModelQuadFacing.NORTH), vertexOffset, indexOffset);
                }
            }

            if (this.batch.isEmpty()) {
                continue;
            }

            this.batch.end();
            
            if (arenas.getTessellation() == null) {
                arenas.setTessellation(this.createRegionTessellation(commandList, arenas));
            }

            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer fb = stack.mallocFloat(3);
                fb.put(0, camera.getChunkModelOffset(region.getRenderX(), camera.blockX, camera.deltaX));
                fb.put(1, camera.getChunkModelOffset(region.getRenderY(), camera.blockY, camera.deltaY));
                fb.put(2, camera.getChunkModelOffset(region.getRenderZ(), camera.blockZ, camera.deltaZ));

                GL20C.glUniform3fv(this.activeProgram.uRegionTranslation, fb);
            }

            try (DrawCommandList drawCommandList = commandList.beginTessellating(arenas.getTessellation())) {
                drawCommandList.multiDrawElementsBaseVertex(this.batch.getPointerBuffer(), this.batch.getCountBuffer(), this.batch.getBaseVertexBuffer());
            }
        }

        super.end();
    }

    private void addDrawCall(ElementRange part, int vertexBase, int indexOffset) {
        if (part != null) {
            this.batch.add((indexOffset * 4) + part.elementOffset, part.elementCount, vertexBase + part.baseVertex);
        }
    }

    private GlTessellation createRegionTessellation(CommandList commandList, RenderRegion.RenderRegionArenas arenas) {
        return commandList.createTessellation(GlPrimitiveType.TRIANGLES, new TessellationBinding[] {
                new TessellationBinding(arenas.vertexBuffers.getBufferObject(), this.vertexAttributeBindings, false)
        }, arenas.indexBuffers.getBufferObject());
    }
}
