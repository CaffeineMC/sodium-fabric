package me.jellysquid.mods.sodium.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.util.Identifier;

public class InGameGuiBatch extends ItemRenderBatch {
    private static final Identifier WIDGETS_TEXTURE = new Identifier("textures/gui/widgets.png");
    private static final Identifier GUI_ICONS_TEXTURE = new Identifier("textures/gui/icons.png");

    public final BufferBuilder widgets = new BufferBuilder(16384);
    public final BufferBuilder icons = new BufferBuilder(16384);

    @Override
    public void startBatches() {
        super.startBatches();

        this.widgets.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        this.icons.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
    }

    @Override
    protected void drawBatches() {
        this.drawWidgets();

        super.drawBatches();

        this.drawIcons();
    }

    private void drawWidgets() {
        RenderSystem.enableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.setShaderTexture(0, WIDGETS_TEXTURE);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        this.widgets.end();
        BufferRenderer.draw(this.widgets);
    }

    private void drawIcons() {
        RenderSystem.enableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.setShaderTexture(0, GUI_ICONS_TEXTURE);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        this.icons.end();
        BufferRenderer.draw(this.icons);
    }
}
