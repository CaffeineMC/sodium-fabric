package me.jellysquid.mods.sodium.client.gl.util;

import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

public class GlFogHelper {
    private static final float FAR_PLANE_THRESHOLD_EXP = (float) Math.log(1.0f / 0.0019f);
    private static final float FAR_PLANE_THRESHOLD_EXP2 = MathHelper.sqrt(FAR_PLANE_THRESHOLD_EXP);

    public static float getFogEnd() {
        return GL11.glGetFloat(GL11.GL_FOG_END);
    }

    public static float getFogStart() {
        return GL11.glGetFloat(GL11.GL_FOG_START);
    }

    public static float getFogDensity() {
        return GL11.glGetFloat(GL11.GL_FOG_DENSITY);
    }

    public static float getFogCutoff() {
        int mode = GL11.glGetInteger(GL11.GL_FOG_MODE);

        switch (mode) {
            case GL11.GL_LINEAR:
                return getFogEnd();
            case GL11.GL_EXP:
                return FAR_PLANE_THRESHOLD_EXP / getFogDensity();
            case GL11.GL_EXP2:
                return FAR_PLANE_THRESHOLD_EXP2 / getFogDensity();
            default:
                return 0.0f;
        }
    }
}
