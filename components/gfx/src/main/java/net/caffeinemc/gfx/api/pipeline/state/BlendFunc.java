package net.caffeinemc.gfx.api.pipeline.state;

public class BlendFunc {
    public final SrcFactor srcRGB, srcAlpha;
    public final DstFactor dstRGB, dstAlpha;

    public BlendFunc(SrcFactor srcRGB, DstFactor dstRGB, SrcFactor srcAlpha, DstFactor dstAlpha) {
        this.srcRGB = srcRGB;
        this.srcAlpha = srcAlpha;
        this.dstRGB = dstRGB;
        this.dstAlpha = dstAlpha;
    }

    public static BlendFunc unified(SrcFactor srcRGBA, DstFactor dstRGBA) {
        return new BlendFunc(srcRGBA, dstRGBA, srcRGBA, dstRGBA);
    }

    public static BlendFunc separate(SrcFactor srcRGB, DstFactor dstRGB, SrcFactor srcAlpha, DstFactor dstAlpha) {
        return new BlendFunc(srcRGB, dstRGB, srcAlpha, dstAlpha);
    }

    public enum DstFactor {
        ZERO,
        ONE,
        SRC_COLOR,
        ONE_MINUS_SRC_COLOR,
        DST_COLOR,
        ONE_MINUS_DST_COLOR,
        SRC_ALPHA,
        ONE_MINUS_SRC_ALPHA,
        DST_ALPHA,
        ONE_MINUS_DST_ALPHA,
        CONSTANT_COLOR,
        ONE_MINUS_CONSTANT_COLOR,
        CONSTANT_ALPHA,
        ONE_MINUS_CONSTANT_ALPHA
    }

    public enum SrcFactor {
        ZERO,
        ONE,
        SRC_COLOR,
        ONE_MINUS_SRC_COLOR,
        DST_COLOR,
        ONE_MINUS_DST_COLOR,
        SRC_ALPHA,
        ONE_MINUS_SRC_ALPHA,
        DST_ALPHA,
        ONE_MINUS_DST_ALPHA,
        CONSTANT_COLOR,
        ONE_MINUS_CONSTANT_COLOR,
        CONSTANT_ALPHA,
        ONE_MINUS_CONSTANT_ALPHA,
        SRC_ALPHA_SATURATE
    }
}
