package me.jellysquid.mods.sodium.client.render.chunk.backend.multidraw;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import me.jellysquid.mods.sodium.client.gl.arena.GlBufferArena;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttribute;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeFormat;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferTarget;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferUsage;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.DrawCommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gl.func.GlFunctions;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlPrimitiveType;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlTessellation;
import me.jellysquid.mods.sodium.client.gl.tessellation.TessellationBinding;
import me.jellysquid.mods.sodium.client.gl.util.ElementRange;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkCameraContext;
import me.jellysquid.mods.sodium.client.render.chunk.RenderChunk;
import me.jellysquid.mods.sodium.client.render.chunk.backend.RegionChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderBounds;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.RenderRegion;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderBindingPoints;
import net.minecraft.client.util.math.MatrixStack;

import java.util.List;
import java.util.Map;

public class MultidrawChunkRenderer extends RegionChunkRenderer {
    private final GlMutableBuffer uploadBuffer;
    private final GlMutableBuffer uniformBuffer;
    private final GlMutableBuffer commandBuffer;

    private final ChunkDrawParamsBufferBuilder uniformBufferBuilder;
    private final IndirectCommandBufferBuilder commandBufferBuilder;

    public MultidrawChunkRenderer(RenderDevice device, ChunkVertexType vertexType) {
        super(device, vertexType);

        try (CommandList commands = device.createCommandList()) {
            this.uploadBuffer = commands.createMutableBuffer(GlBufferUsage.GL_STREAM_DRAW);
            this.uniformBuffer = commands.createMutableBuffer(GlBufferUsage.GL_STATIC_DRAW);
            this.commandBuffer = commands.createMutableBuffer(GlBufferUsage.GL_STREAM_DRAW);
        }

        this.uniformBufferBuilder = ChunkDrawParamsBufferBuilder.create(2048);
        this.commandBufferBuilder = IndirectCommandBufferBuilder.create(2048);
    }

    @Override
    public void render(MatrixStack matrixStack, CommandList commandList, ObjectList<RenderChunk> renders, BlockRenderPass pass, ChunkCameraContext camera) {
        super.begin(pass, matrixStack);

        Map<RenderRegion, List<RenderChunk>> sortedRenders = this.sortRenders(renders);
        List<ChunkRegionDrawInfo> sortedDraws = this.setupDrawBatches(commandList, sortedRenders, pass, camera);

        commandList.bindBuffer(GlBufferTarget.DRAW_INDIRECT_BUFFER, this.commandBuffer);

        for (ChunkRegionDrawInfo drawInfo : sortedDraws) {
            RenderRegion region = drawInfo.region;

            if (region.getTessellation() == null) {
                region.setTessellation(this.createRegionTessellation(commandList, region.getVertexBufferArena(), region.getIndexBufferArena()));
            }

            try (DrawCommandList drawCommandList = commandList.beginTessellating(region.getTessellation())) {
                drawCommandList.multiDrawElementsIndirect(drawInfo.commandOffset, drawInfo.commandLength, 0 /* tightly packed */);
            }
        }

        super.end();
    }

    private List<ChunkRegionDrawInfo> setupDrawBatches(CommandList commandList, Map<RenderRegion, List<RenderChunk>> lists, BlockRenderPass pass, ChunkCameraContext camera) {
        List<ChunkRegionDrawInfo> draws = new ObjectArrayList<>(lists.size());

        this.uniformBufferBuilder.reset();
        this.commandBufferBuilder.reset();

        int drawIndex = 0;

        for (Map.Entry<RenderRegion, List<RenderChunk>> entry : lists.entrySet()) {
            int commandOffset = this.commandBufferBuilder.getCount();

            for (RenderChunk render : entry.getValue()) {
                ChunkGraphicsState state = render.getGraphicsState(pass);

                // TODO: remove very expensive divisions
                int vertexOffset = state.getVertexSegment().getStart() / this.vertexFormat.getStride();
                int indexOffset = state.getIndexSegment().getStart() / 4;

                ChunkRenderBounds bounds = render.getBounds();

                boolean chunkRendered;
                chunkRendered = this.addDrawCall(state.getModelPart(ModelQuadFacing.UNASSIGNED), vertexOffset, indexOffset, drawIndex);;

                if (camera.posY > bounds.y1) {
                    chunkRendered |= this.addDrawCall(state.getModelPart(ModelQuadFacing.UP), vertexOffset, indexOffset, drawIndex);
                }

                if (camera.posY < bounds.y2) {
                    chunkRendered |= this.addDrawCall(state.getModelPart(ModelQuadFacing.DOWN), vertexOffset, indexOffset, drawIndex);
                }

                if (camera.posX > bounds.x1) {
                    chunkRendered |= this.addDrawCall(state.getModelPart(ModelQuadFacing.EAST), vertexOffset, indexOffset, drawIndex);
                }

                if (camera.posX < bounds.x2) {
                    chunkRendered |= this.addDrawCall(state.getModelPart(ModelQuadFacing.WEST), vertexOffset, indexOffset, drawIndex);
                }

                if (camera.posZ > bounds.z1) {
                    chunkRendered |= this.addDrawCall(state.getModelPart(ModelQuadFacing.SOUTH), vertexOffset, indexOffset, drawIndex);
                }

                if (camera.posZ < bounds.z2) {
                    chunkRendered |= this.addDrawCall(state.getModelPart(ModelQuadFacing.NORTH), vertexOffset, indexOffset, drawIndex);
                }

                if (chunkRendered) {
                    drawIndex++;

                    float x = camera.getChunkModelOffset(render.getRenderX(), camera.blockX, camera.deltaX);
                    float y = camera.getChunkModelOffset(render.getRenderY(), camera.blockY, camera.deltaY);
                    float z = camera.getChunkModelOffset(render.getRenderZ(), camera.blockZ, camera.deltaZ);

                    this.uniformBufferBuilder.pushChunkDrawParams(x, y, z);
                }
            }

            int commandCount = this.commandBufferBuilder.getCount() - commandOffset;

            if (commandCount != 0) {
                draws.add(new ChunkRegionDrawInfo(entry.getKey(),
                        commandOffset * IndirectCommandBufferBuilder.STRIDE, commandCount));
            }
        }


        commandList.uploadData(this.uniformBuffer, this.uniformBufferBuilder.getBuffer());
        commandList.uploadData(this.commandBuffer, this.commandBufferBuilder.getBuffer());

        return draws;
    }

    private boolean addDrawCall(ElementRange part, int vertexOffset, int indexOffset, int index) {
        if (part != null) {
            this.commandBufferBuilder.addIndirectDrawCall(part.elementCount, 1, indexOffset + part.elementOffset, vertexOffset + part.baseVertex, index);
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
                }, false),
                new TessellationBinding(this.uniformBuffer, new GlVertexAttributeBinding[] {
                        new GlVertexAttributeBinding(ChunkShaderBindingPoints.MODEL_OFFSET,
                                new GlVertexAttribute(GlVertexAttributeFormat.FLOAT, 4, false, 0, 0))
                }, true)
        }, indices.getArenaBuffer());
    }

    @Override
    public void delete() {
        super.delete();

        try (CommandList commands = RenderDevice.INSTANCE.createCommandList()) {
            commands.deleteBuffer(this.uploadBuffer);
            commands.deleteBuffer(this.uniformBuffer);
            commands.deleteBuffer(this.commandBuffer);
        }

        this.commandBufferBuilder.delete();
        this.uniformBufferBuilder.delete();
    }

    public static boolean isSupported(boolean disableDriverBlacklist) {
        return GlFunctions.isIndirectMultiDrawSupported() &&
                GlFunctions.isInstancedArraySupported();
    }
}
