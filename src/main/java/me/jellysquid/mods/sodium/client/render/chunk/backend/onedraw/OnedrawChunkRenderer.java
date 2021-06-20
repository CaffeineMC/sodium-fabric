package me.jellysquid.mods.sodium.client.render.chunk.backend.onedraw;

import it.unimi.dsi.fastutil.objects.ObjectList;
import me.jellysquid.mods.sodium.client.gl.arena.GlBufferArena;
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
import me.jellysquid.mods.sodium.client.render.chunk.ChunkCameraContext;
import me.jellysquid.mods.sodium.client.render.chunk.RenderChunk;
import me.jellysquid.mods.sodium.client.render.chunk.backend.RegionChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.backend.multidraw.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderBounds;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.RenderRegion;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderBindingPoints;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.Map;

public class OnedrawChunkRenderer extends RegionChunkRenderer {
    private final GlMultiDrawBatch batch = new GlMultiDrawBatch(ModelQuadFacing.COUNT);

    public OnedrawChunkRenderer(RenderDevice device, ChunkVertexType vertexType) {
        super(device, vertexType);
    }

    @Override
    public void render(MatrixStack matrixStack, CommandList commandList, ObjectList<RenderChunk> renders, BlockRenderPass pass, ChunkCameraContext camera) {
        super.begin(pass, matrixStack);

        Map<RenderRegion, List<RenderChunk>> sortedRenders = this.sortRenders(renders);

        for (Map.Entry<RenderRegion, List<RenderChunk>> entry : sortedRenders.entrySet()) {
            RenderRegion region = entry.getKey();
            List<RenderChunk> chunks = entry.getValue();

            if (region.getTessellation() == null) {
                region.setTessellation(this.createRegionTessellation(commandList, region.getVertexBufferArena(), region.getIndexBufferArena()));
            }

            DrawCommandList drawCommandList = commandList.beginTessellating(region.getTessellation());

            for (RenderChunk render : chunks) {
                ChunkGraphicsState state = render.getGraphicsState(pass);
                ChunkRenderBounds bounds = render.getBounds();

                this.batch.begin();

                // TODO: remove very expensive divisions
                int vertexOffset = state.getVertexSegment().getStart() / this.vertexFormat.getStride();
                int indexOffset = state.getIndexSegment().getStart() / 4;

                boolean visible = this.addDrawCall(state.getModelPart(ModelQuadFacing.UNASSIGNED), vertexOffset, indexOffset);

                if (camera.posY > bounds.y1) {
                    visible |= this.addDrawCall(state.getModelPart(ModelQuadFacing.UP), vertexOffset, indexOffset);
                }

                if (camera.posY < bounds.y2) {
                    visible |= this.addDrawCall(state.getModelPart(ModelQuadFacing.DOWN), vertexOffset, indexOffset);
                }

                if (camera.posX > bounds.x1) {
                    visible |= this.addDrawCall(state.getModelPart(ModelQuadFacing.EAST), vertexOffset, indexOffset);
                }

                if (camera.posX < bounds.x2) {
                    visible |= this.addDrawCall(state.getModelPart(ModelQuadFacing.WEST), vertexOffset, indexOffset);
                }

                if (camera.posZ > bounds.z1) {
                    visible |= this.addDrawCall(state.getModelPart(ModelQuadFacing.SOUTH), vertexOffset, indexOffset);
                }

                if (camera.posZ < bounds.z2) {
                    visible |= this.addDrawCall(state.getModelPart(ModelQuadFacing.NORTH), vertexOffset, indexOffset);
                }

                this.batch.end();

                if (!visible) {
                    continue;
                }

                float modelX = camera.getChunkModelOffset(render.getRenderX(), camera.blockX, camera.deltaX);
                float modelY = camera.getChunkModelOffset(render.getRenderY(), camera.blockY, camera.deltaY);
                float modelZ = camera.getChunkModelOffset(render.getRenderZ(), camera.blockZ, camera.deltaZ);

                try (MemoryStack stack = MemoryStack.stackPush()) {
                    FloatBuffer fb = stack.mallocFloat(4);
                    fb.put(0, modelX);
                    fb.put(1, modelY);
                    fb.put(2, modelZ);

                    GL20C.glVertexAttrib4fv(ChunkShaderBindingPoints.MODEL_OFFSET.getGenericAttributeIndex(), fb);
                }

                drawCommandList.multiDrawElementsBaseVertex(this.batch.getPointerBuffer(), this.batch.getCountBuffer(), this.batch.getBaseVertexBuffer());
            }

            drawCommandList.flush();
        }

        super.end();
    }

    private boolean addDrawCall(ElementRange part, int vertexOffset, int indexOffset) {
        if (part != null) {
            this.batch.addChunkRender(part, vertexOffset, indexOffset);
            return true;
        }

        return false;
    }


    @Override
    protected GlTessellation createRegionTessellation(CommandList commandList, GlBufferArena vertices, GlBufferArena indices) {
        return commandList.createTessellation(GlPrimitiveType.TRIANGLES, new TessellationBinding[] {
                new TessellationBinding(vertices.getArenaBuffer(), new GlVertexAttributeBinding[] {
                        new GlVertexAttributeBinding(ChunkShaderBindingPoints.POSITION,
                                this.vertexFormat.getAttribute(ChunkMeshAttribute.POSITION)),
                        new GlVertexAttributeBinding(ChunkShaderBindingPoints.COLOR,
                                this.vertexFormat.getAttribute(ChunkMeshAttribute.COLOR)),
                        new GlVertexAttributeBinding(ChunkShaderBindingPoints.TEX_COORD,
                                this.vertexFormat.getAttribute(ChunkMeshAttribute.TEXTURE)),
                        new GlVertexAttributeBinding(ChunkShaderBindingPoints.LIGHT_COORD,
                                this.vertexFormat.getAttribute(ChunkMeshAttribute.LIGHT))
                }, false)
        }, indices.getArenaBuffer());
    }
}
