package me.jellysquid.mods.sodium.client.render.backends.shader.cr;

import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.sodium.client.gl.SodiumVertexFormats;
import me.jellysquid.mods.sodium.client.gl.array.GlVertexArray;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.buffer.BufferUploadData;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.gl.memory.BufferBlock;
import me.jellysquid.mods.sodium.client.gl.memory.BufferSegment;
import me.jellysquid.mods.sodium.client.render.backends.shader.AbstractShaderChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.backends.shader.lcb.ChunkRegionManager;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkMesh;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRender;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.layer.BlockRenderPass;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import org.apache.logging.log4j.util.TriConsumer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Consumer;

public class CRChunkRenderBackend extends AbstractShaderChunkRenderBackend<CRRenderState> {

    private static int discriminator = 0;

    private final ChunkRegionManager<CRChunkRegion> regionManager;
    private final GlMutableBuffer uploadBuffer;

    private ArrayList<CRChunkRegion> nearRegions = new ArrayList<>();
    private ArrayList<CRChunkRegion> farRegions = new ArrayList<>();

    private VertexBuffer boundingBoxBuf;
    private BoundingBoxShader boundingBoxShader;

    public CRChunkRenderBackend(GlVertexFormat<SodiumVertexFormats.ChunkMeshAttribute> format) {
        super(format);

        this.regionManager = new ChunkRegionManager<CRChunkRegion>(
                CRChunkRegion::new
        );
        this.uploadBuffer = new GlMutableBuffer(GL15.GL_STREAM_COPY);

        initBoundingBoxVertexBuffer();

        boundingBoxShader = BoundingBoxShader.createBoundingBoxShader();
    }

    private void initBoundingBoxVertexBuffer() {
        double len = 4 * 16;

        BufferBuilder boundingBoxBuffer;

        boundingBoxBuffer = new BufferBuilder(6 * 4 * 3);

        boundingBoxBuffer.begin(GL11.GL_QUADS, VertexFormats.POSITION);

        Consumer<Vec3d> putVertex = (vec) -> {
            boundingBoxBuffer.vertex(vec.x, vec.y, vec.z)
                    //.color(255, 0, 255, 255)
                    .next();
        };

        TriConsumer<Vec3d, Vec3d, Vec3d> putQuad = (base, vec1, vec2) -> {
            putVertex.accept(base);
            putVertex.accept(base.add(vec1));
            putVertex.accept(base.add(vec1).add(vec2));
            putVertex.accept(base.add(vec2));
        };

        Vec3d xAxis = new Vec3d(len, 0, 0);
        Vec3d yAxis = new Vec3d(0, len, 0);
        Vec3d zAxis = new Vec3d(0, 0, len);

        putQuad.accept(Vec3d.ZERO, xAxis, zAxis);//bottom
        putQuad.accept(Vec3d.ZERO, zAxis, yAxis);//left
        putQuad.accept(Vec3d.ZERO, yAxis, xAxis);//back

        putQuad.accept(yAxis, zAxis, xAxis);//top
        putQuad.accept(xAxis, yAxis, zAxis);//right
        putQuad.accept(zAxis, xAxis, yAxis);//front

        boundingBoxBuffer.end();

        boundingBoxBuf = new VertexBuffer(VertexFormats.POSITION);
        boundingBoxBuf.upload(boundingBoxBuffer);
    }

    @Override
    public void upload(Iterator<ChunkBuildResult<CRRenderState>> queue) {
        GlMutableBuffer uploadBuffer = this.uploadBuffer;
        uploadBuffer.bind(GL15.GL_ARRAY_BUFFER);

        BufferBlock prevBlock = null;

        while (queue.hasNext()) {
            ChunkBuildResult<CRRenderState> result = queue.next();

            ChunkRender<CRRenderState> render = result.render;
            ChunkRenderData data = result.data;

            render.resetRenderStates();
            render.setData(data);

            for (ChunkMesh mesh : data.getMeshes()) {
                ChunkSectionPos pos = render.getChunkPos();

                CRChunkRegion region = this.regionManager.createRegion(pos);
                BufferBlock block = region.getBuffer();

                if (prevBlock != block) {
                    if (prevBlock != null) {
                        prevBlock.endUploads();
                    }

                    block.beginUpload();

                    prevBlock = block;
                }

                BufferUploadData upload = mesh.takePendingUpload();
                uploadBuffer.upload(GL15.GL_ARRAY_BUFFER, upload);

                BufferSegment segment = block.upload(GL15.GL_ARRAY_BUFFER, 0, upload.buffer.capacity());

                render.setRenderState(
                        mesh.getRenderPass(),
                        new CRRenderState(region, segment, this.vertexFormat)
                );
            }
        }

        if (prevBlock != null) {
            prevBlock.endUploads();
        }

        uploadBuffer.invalidate(GL15.GL_ARRAY_BUFFER);
        uploadBuffer.unbind(GL15.GL_ARRAY_BUFFER);
    }

    @Override
    public void render(Iterator<CRRenderState> renders, MatrixStack matrixStack, double x, double y, double z) {

        throw new UnsupportedOperationException();

    }

    private static Vec3d getCameraPos() {
        return MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
    }

    private boolean isNearRegion(CRChunkRegion region) {
        //TODO change it
        return nearRegions.size() < 8;
    }

    public void onBlockRenderingStarted(
            ArrayList<ChunkRender<CRRenderState>> sectionsToRender,
            MatrixStack matrixStack,
            BlockRenderPass[] solidRenderPasses
    ) {
        prepareRendering(sectionsToRender);

        renderNearSolidBlocks(matrixStack, solidRenderPasses);

        performQueries(matrixStack);

//        renderFarSolidBlocks(matrixStack, solidRenderPasses);
    }

    private void prepareRendering(ArrayList<ChunkRender<CRRenderState>> sectionsToRender) {
        discriminator++;

        purge();

        nearRegions.clear();
        farRegions.clear();

        for (ChunkRender<CRRenderState> section : sectionsToRender) {
            CRChunkRegion region = regionManager.createRegion(section.getChunkPos());

            // check whether it's already added
            if (region.discriminator != discriminator) {
                region.discriminator = discriminator;


                if (isNearRegion(region)) {
                    nearRegions.add(region);
                } else {
                    farRegions.add(region);
                }

                // This should not be really needed
                region.resetBatches();
            }

            CRRenderState[] renderStates = section.getRenderStates();
            for (int passIndex = 0; passIndex < renderStates.length; passIndex++) {
                CRRenderState state = renderStates[passIndex];
                if (state != null) {
                    region.addToBatch(
                            BlockRenderPass.values()[passIndex],
                            state.getStart(),
                            state.getLength()
                    );
                }
            }
        }
    }

    private void drawRegions(
            MatrixStack matrixStack,
            BlockRenderPass pass,
            Iterator<CRChunkRegion> regions,
            Consumer<CRChunkRegion> drawCall
    ) {
        pass.startDrawing();

        Vec3d cameraPos = getCameraPos();

        double x = cameraPos.x;
        double y = cameraPos.y;
        double z = cameraPos.z;

        int chunkX = (int) (x / 16.0D);
        int chunkY = (int) (y / 16.0D);
        int chunkZ = (int) (z / 16.0D);

        this.begin(matrixStack);

        this.activeProgram.setModelMatrix(matrixStack, x % 16.0D, y % 16.0D, z % 16.0D);

        while (regions.hasNext()) {
            CRChunkRegion region = regions.next();

            this.activeProgram.setModelOffset(region.getOrigin(), chunkX, chunkY, chunkZ);

            drawCall.accept(region);
        }

        GlVertexArray.unbindVertexArray();

        this.end(matrixStack);

        pass.endDrawing();
    }

    private void renderNearSolidBlocks(MatrixStack matrixStack, BlockRenderPass[] passes) {
        for (BlockRenderPass pass : passes) {
            drawRegions(
                    matrixStack, pass, this.nearRegions.iterator(),
                    region -> {
                        region.drawBatch(pass, this.activeProgram.attributes);
                    }
            );
        }
    }

    private void performQueries(MatrixStack matrixStack) {
        Vec3d cameraPos = getCameraPos();

        double x = cameraPos.x;
        double y = cameraPos.y;
        double z = cameraPos.z;

        int chunkX = (int) (x / 16.0D);
        int chunkY = (int) (y / 16.0D);
        int chunkZ = (int) (z / 16.0D);

        RenderSystem.enableCull();
        RenderSystem.disableAlphaTest();
        RenderSystem.disableBlend();
        RenderSystem.disableLighting();
        RenderSystem.disableColorMaterial();
        RenderSystem.disableFog();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.color4f(1, 1, 1, 1);

        if (shouldUseBoundingBoxShader()) {
            boundingBoxShader.bind();

            boundingBoxShader.setModelMatrix(matrixStack, x % 16.0D, y % 16.0D, z % 16.0D);

            boundingBoxBuf.bind();

            VertexFormats.POSITION.startDrawing(0);

            for (CRChunkRegion region : farRegions) {

                boundingBoxShader.setModelOffset(region.getOrigin(), chunkX, chunkY, chunkZ);

                region.getQueryObject().performQueryAnySamplePassedConservative(() -> {
                    matrixStack.push();
                    matrixStack.translate(
                            region.getOrigin().getMinX() - x,
                            region.getOrigin().getMinY() - y,
                            region.getOrigin().getMinZ() - z
                    );

                    boundingBoxBuf.draw(matrixStack.peek().getModel(), GL11.GL_QUADS);
                    matrixStack.pop();
                });
            }

            boundingBoxShader.unbind();
        } else {
            GL20.glUseProgram(0);

            boundingBoxBuf.bind();
            VertexFormats.POSITION_COLOR.startDrawing(0);

            for (CRChunkRegion region : farRegions) {
                region.getQueryObject().performQueryAnySamplePassedConservative(() -> {
                    matrixStack.push();
                    matrixStack.translate(
                            region.getOrigin().getMinX() - x,
                            region.getOrigin().getMinY() - y,
                            region.getOrigin().getMinZ() - z
                    );

                    boundingBoxBuf.draw(matrixStack.peek().getModel(), GL11.GL_QUADS);
                    matrixStack.pop();
                });
            }


        }

        RenderSystem.glBindBuffer(34962, () -> 0);

        RenderSystem.depthMask(true);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.enableCull();

    }

    public void renderFarSolidBlocks(MatrixStack matrixStack, BlockRenderPass[] passes) {
        for (BlockRenderPass pass : passes) {
            drawRegions(
                    matrixStack, pass, this.farRegions.iterator(),
                    region -> {
                        region.drawBatch(
                                pass, this.activeProgram.attributes,
                                (r) -> {
                                    region.getQueryObject().performConditionalRendering(
                                            shouldWait(),
                                            r
                                    );
                                }
                        );
                    }
            );
        }
    }

    public void renderTranslucentBlocks(MatrixStack matrixStack) {
        BlockRenderPass pass = BlockRenderPass.TRANSLUCENT;

        drawRegions(
                matrixStack, pass, this.nearRegions.iterator(),
                region -> {
                    region.drawBatch(pass, this.activeProgram.attributes);
                }
        );

        drawRegions(
                matrixStack, pass, this.farRegions.iterator(),
                region -> {
                    region.drawBatch(
                            pass, this.activeProgram.attributes,
                            (r) -> {
                                region.getQueryObject().performConditionalRendering(
                                        shouldWait(),
                                        r
                                );
                            }
                    );
                }
        );

        //debugStat();
    }

    @Override
    public Class<CRRenderState> getRenderStateType() {
        return CRRenderState.class;
    }

    @Override
    protected CRRenderState createRenderState(GlBuffer buffer, ChunkRender<CRRenderState> render) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete() {
        super.delete();

        this.regionManager.delete();
        this.uploadBuffer.delete();
    }

    @Override
    public BlockPos getRenderOffset(ChunkSectionPos pos) {
        return this.regionManager.getRenderOffset(pos);
    }

    public static boolean isSupported() {
        return GlVertexArray.isSupported() && GlBuffer.isBufferCopySupported();
    }

    void purge() {
        regionManager.cleanup();
    }

    boolean shouldWait() {
        return false;
    }

    boolean shouldUseBoundingBoxShader() {
        return true;
    }

    void debugStat() {
        int visible = 0;
        int invisible = 0;

        for (CRChunkRegion region : farRegions) {
            boolean result = region.getQueryObject().fetchQueryResultBooleanSynced();
            if (result) {
                visible++;
            } else {
                invisible++;
            }
        }

        System.out.println(" " + nearRegions.size() + " " + visible + " " + invisible);
    }
}
