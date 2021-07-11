package me.jellysquid.mods.sodium.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.util.math.Matrix4f;

public class ItemRenderBatch extends RenderBatch {
    private static final Matrix4f ITEM_LIGHTING_MATRIX = new Matrix4f();

    static {
        ITEM_LIGHTING_MATRIX.loadIdentity();
        ITEM_LIGHTING_MATRIX.multiply(Matrix4f.scale(-1.0f, 1.0f, 1.0f));
    }

    private final BufferBuilder itemOverlay = new BufferBuilder(16384);

    private final BatchingVertexConsumer itemModelUnlit = new BatchingVertexConsumer();
    private final BatchingVertexConsumer itemModelLit = new BatchingVertexConsumer();
    private final BatchingVertexConsumer itemLabel = new BatchingVertexConsumer();

    @Override
    public void startBatches() {
        this.itemOverlay.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
    }

    @Override
    protected void drawBatches() {
        this.drawItemModels();
        this.drawItemOverlays();
        this.drawItemLabels();
    }

    protected void drawItemLabels() {
        this.itemLabel.draw();
    }

    protected void drawItemModels() {
        RenderSystem.enableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        DiffuseLighting.enableForLevel(ITEM_LIGHTING_MATRIX);
        this.itemModelLit.draw();

        RenderSystem.enableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        DiffuseLighting.disableGuiDepthLighting();
        this.itemModelUnlit.draw();
        DiffuseLighting.enableGuiDepthLighting();
    }

    protected void drawItemOverlays() {
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        this.itemOverlay.end();
        BufferRenderer.draw(this.itemOverlay);
    }

    public VertexConsumerProvider getItemRendererVertexConsumer(boolean sideLit) {
        return sideLit ? this.itemModelLit : this.itemModelUnlit;
    }

    public VertexConsumerProvider getItemFontBuffer() {
        return this.itemLabel;
    }

    public BufferBuilder getItemOverlayBuffer() {
        return this.itemOverlay;
    }
}
