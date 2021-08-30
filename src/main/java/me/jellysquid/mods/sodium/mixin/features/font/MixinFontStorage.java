package me.jellysquid.mods.sodium.mixin.features.font;

import me.jellysquid.mods.sodium.client.util.SparseArray;
import net.minecraft.client.font.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(FontStorage.class)
public abstract class MixinFontStorage {
    @Shadow
    @Final
    private static Glyph SPACE;

    @Shadow
    protected abstract RenderableGlyph getRenderableGlyph(int codePoint);

    @Shadow
    @Final
    private static EmptyGlyphRenderer EMPTY_GLYPH_RENDERER;

    @Shadow
    protected abstract GlyphRenderer getGlyphRenderer(RenderableGlyph c);

    private final SparseArray<Glyph> glyphCacheSparse = new SparseArray<>(65536, 8);
    private final SparseArray<GlyphRenderer> glyphRendererSparse = new SparseArray<>(65536, 8);

    @Inject(method = "setFonts", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;clear()V", ordinal = 0))
    private void onGlyphCacheCleared(List<Font> fonts, CallbackInfo ci) {
        this.glyphCacheSparse.clear();
        this.glyphRendererSparse.clear();
    }

    /**
     * @author JellySquid
     * @reason Avoid expensive hash table
     */
    @Overwrite
    public Glyph getGlyph(int codePoint) {
        Glyph glyph = this.glyphCacheSparse.get(codePoint);

        if (glyph == null) {
            this.glyphCacheSparse.put(codePoint, glyph = this.computeGlyph(codePoint));
        }

        return glyph;
    }

    /**
     * @author JellySquid
     * @reason Avoid expensive hash table
     */
    @Overwrite
    public GlyphRenderer getGlyphRenderer(int codePoint) {
        GlyphRenderer renderer = this.glyphRendererSparse.get(codePoint);

        if (renderer == null) {
            this.glyphRendererSparse.put(codePoint, renderer = this.computeGlyphRenderer(codePoint));
        }

        return renderer;
    }

    private Glyph computeGlyph(int codePoint) {
        return codePoint == 32 ? SPACE : this.getRenderableGlyph(codePoint);
    }

    private GlyphRenderer computeGlyphRenderer(int codePoint) {
        return codePoint == 32 ? EMPTY_GLYPH_RENDERER : this.getGlyphRenderer(this.getRenderableGlyph(codePoint));
    }
}
