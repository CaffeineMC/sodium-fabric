package me.jellysquid.mods.sodium.opengl.types;

import com.mojang.blaze3d.platform.GlStateManager;

public class BlendFunction {
    public final GlStateManager.SourceFactor srcRGB, srcAlpha;
    public final GlStateManager.DestFactor dstRGB, dstAlpha;

    public BlendFunction(GlStateManager.SourceFactor srcRGB, GlStateManager.DestFactor dstRGB, GlStateManager.SourceFactor srcAlpha, GlStateManager.DestFactor dstAlpha) {
        this.srcRGB = srcRGB;
        this.srcAlpha = srcAlpha;
        this.dstRGB = dstRGB;
        this.dstAlpha = dstAlpha;
    }

    public static BlendFunction of(GlStateManager.SourceFactor srcRGBA, GlStateManager.DestFactor dstRGBA) {
        return new BlendFunction(srcRGBA, dstRGBA, srcRGBA, dstRGBA);
    }

    public static BlendFunction of(GlStateManager.SourceFactor srcRGB, GlStateManager.DestFactor dstRGB, GlStateManager.SourceFactor srcAlpha, GlStateManager.DestFactor dstAlpha) {
        return new BlendFunction(srcRGB, dstRGB, srcAlpha, dstAlpha);
    }
}
