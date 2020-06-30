package me.jellysquid.mods.sodium.client.gl.func;

import org.lwjgl.opengl.ARBMultiDrawIndirect;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLCapabilities;

import java.nio.ByteBuffer;

public enum GlIndirectMultiDrawFunctions {
    CORE {
        @Override
        public void glMultiDrawArraysIndirect(int mode, ByteBuffer indirect, int primcount, int stride) {
            GL43.glMultiDrawArraysIndirect(mode, indirect, primcount, stride);
        }
    },
    ARB {
        @Override
        public void glMultiDrawArraysIndirect(int mode, ByteBuffer indirect, int primcount, int stride) {
            ARBMultiDrawIndirect.glMultiDrawArraysIndirect(mode, indirect, primcount, stride);
        }
    },
    UNSUPPORTED {
        @Override
        public void glMultiDrawArraysIndirect(int mode, ByteBuffer indirect, int primcount, int stride) {
            throw new UnsupportedOperationException();
        }
    };

    public static GlIndirectMultiDrawFunctions load(GLCapabilities capabilities) {
        if (capabilities.OpenGL43) {
            return CORE;
        } else if (capabilities.GL_ARB_multi_draw_indirect) {
            return ARB;
        } else {
            return UNSUPPORTED;
        }
    }

    public abstract void glMultiDrawArraysIndirect(int mode, ByteBuffer indirect, int primcount, int stride);
}
