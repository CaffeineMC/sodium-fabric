package me.jellysquid.mods.sodium.opengl.types;

import com.mojang.blaze3d.platform.GlStateManager;

public class BlendFunction {
    public final GlStateManager.SrcFactor srcRGB, srcAlpha;
    public final GlStateManager.DstFactor dstRGB, dstAlpha;

    public BlendFunction(GlStateManager.SrcFactor srcRGB, GlStateManager.DstFactor dstRGB, GlStateManager.SrcFactor srcAlpha, GlStateManager.DstFactor dstAlpha) {
        this.srcRGB = srcRGB;
        this.srcAlpha = srcAlpha;
        this.dstRGB = dstRGB;
        this.dstAlpha = dstAlpha;
    }

    public static BlendFunction of(GlStateManager.SrcFactor srcRGBA, GlStateManager.DstFactor dstRGBA) {
        return new BlendFunction(srcRGBA, dstRGBA, srcRGBA, dstRGBA);
    }

    public static BlendFunction of(GlStateManager.SrcFactor srcRGB, GlStateManager.DstFactor dstRGB, GlStateManager.SrcFactor srcAlpha, GlStateManager.DstFactor dstAlpha) {
        return new BlendFunction(srcRGB, dstRGB, srcAlpha, dstAlpha);
    }
}
