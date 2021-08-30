package me.jellysquid.mods.sodium.client.util.font;

import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.Glyph;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.CharacterVisitor;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;

public class OutlineCharacterVisitor implements CharacterVisitor {
    private final TextRenderer.Drawer drawer;
    private final TextRenderer renderer;

    private float x;
    private float y;

    private final float offsetX;
    private final float offsetY;

    private Identifier prevFontStorageId;
    private FontStorage prevFontStorage;

    private final int color;

    public OutlineCharacterVisitor(TextRenderer.Drawer parent, TextRenderer renderer, float x, float y, float offsetX, float offsetY, int color) {
        this.drawer = parent;
        this.renderer = renderer;
        this.x = x;
        this.y = y;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.color = color;
    }

    @Override
    public boolean accept(int l, Style style, int m) {
        boolean bl = style.isBold();

        FontStorage fontStorage = this.getFontStorage(style.getFont());
        Glyph glyph = fontStorage.getGlyph(m);

        drawer.x = this.x + (this.offsetX * glyph.getShadowOffset());
        drawer.y = this.y + (this.offsetY * glyph.getShadowOffset());

        this.x += glyph.getAdvance(bl);

        return drawer.accept(l, style.withColor(this.color), m);
    }

    protected FontStorage getFontStorage(Identifier id) {
        if (this.prevFontStorageId == id) {
            return this.prevFontStorage;
        }

        this.prevFontStorageId = id;
        this.prevFontStorage = this.renderer.getFontStorage(id);

        return this.prevFontStorage;
    }
}
