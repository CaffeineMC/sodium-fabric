package me.jellysquid.mods.sodium.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;

import java.util.Set;

public class ContainerBatch extends ItemRenderBatch {
    private final Reference2ObjectMap<Identifier, BufferBuilder> spriteBuffers = new Reference2ObjectOpenHashMap<>();
    private final Set<Identifier> activeSpriteBuffers = new ObjectOpenHashSet<>();

    private final BufferBuilder slotOverlayBuffer = new BufferBuilder(256);

    @Override
    public void startBatches() {
        super.startBatches();

        this.slotOverlayBuffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
    }

    @Override
    protected void drawBatches() {
        this.drawSprites();

        super.drawBatches();

        this.drawSlotOverlays();
    }

    private void drawSprites() {
        for (Identifier textureId : this.activeSpriteBuffers) {
            this.drawSprite(textureId, this.spriteBuffers.get(textureId));
        }

        this.activeSpriteBuffers.clear();
    }

    public BufferBuilder getSpriteBufferBuilder(Identifier textureId) {
        if (!this.isBuilding()) {
            throw new IllegalStateException("Not building!");
        }

        BufferBuilder bufferBuilder = this.spriteBuffers.computeIfAbsent(textureId, i -> new BufferBuilder(256));

        if (this.activeSpriteBuffers.add(textureId)) {
            bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        }

        return bufferBuilder;
    }

    private void drawSprite(Identifier texture, BufferBuilder bufferBuilder) {
        RenderSystem.enableTexture();
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        bufferBuilder.end();
        BufferRenderer.draw(bufferBuilder);
    }

    private void drawSlotOverlays() {
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        RenderSystem.disableDepthTest();
        RenderSystem.colorMask(true, true, true, false);

        this.slotOverlayBuffer.end();
        BufferRenderer.draw(this.slotOverlayBuffer);
        RenderSystem.colorMask(true, true, true, true);

        RenderSystem.enableDepthTest();
    }

    public BufferBuilder getSlotOverlayBuffer() {
        return this.slotOverlayBuffer;
    }
}
