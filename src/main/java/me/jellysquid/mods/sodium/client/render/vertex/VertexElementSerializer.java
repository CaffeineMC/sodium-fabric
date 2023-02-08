package me.jellysquid.mods.sodium.client.render.vertex;

import org.lwjgl.system.MemoryUtil;

public class VertexElementSerializer {
    public static void setPositionXYZ(long ptr, float x, float y, float z) {
        MemoryUtil.memPutFloat(ptr + 0, x);
        MemoryUtil.memPutFloat(ptr + 4, y);
        MemoryUtil.memPutFloat(ptr + 8, z);
    }
    
    public static float getPositionX(long ptr) {
        return MemoryUtil.memGetFloat(ptr + 0);
    }

    public static float getPositionY(long ptr) {
        return MemoryUtil.memGetFloat(ptr + 4);
    }

    public static float getPositionZ(long ptr) {
        return MemoryUtil.memGetFloat(ptr + 8);
    }

    public static void setTextureUV(long ptr, float u, float v) {
        MemoryUtil.memPutFloat(ptr + 0, u);
        MemoryUtil.memPutFloat(ptr + 4, v);
    }

    public static float getTextureU(long ptr) {
        return MemoryUtil.memGetFloat(ptr + 0);
    }

    public static float getTextureV(long ptr) {
        return MemoryUtil.memGetFloat(ptr + 4);
    }
    
    public static void setColorABGR(long ptr, int color) {
        MemoryUtil.memPutInt(ptr + 0, color);
    }
    
    public static int getColorABGR(long ptr) {
        return MemoryUtil.memGetInt(ptr);
    }

    public static void setLightUV(long ptr, int light) {
        MemoryUtil.memPutInt(ptr + 0, light);
    }

    public static int getLightUV(long ptr) {
        return MemoryUtil.memGetInt(ptr);
    }

    public static void setOverlayUV(long ptr, int overlay) {
        MemoryUtil.memPutInt(ptr + 0, overlay);
    }

    public static int getOverlayUV(long ptr) {
        return MemoryUtil.memGetInt(ptr);
    }

    public static void setNormalXYZ(long ptr, int normal) {
        MemoryUtil.memPutInt(ptr + 0, normal);
    }

    public static int getNormalXYZ(long ptr) {
        return MemoryUtil.memGetInt(ptr);
    }
}
