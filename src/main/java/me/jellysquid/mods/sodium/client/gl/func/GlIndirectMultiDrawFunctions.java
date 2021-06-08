package me.jellysquid.mods.sodium.client.gl.func;

import me.jellysquid.mods.sodium.client.render.chunk.backends.multidraw.MultidrawChunkRenderBackend;
import org.lwjgl.opengl.ARBMultiDrawIndirect;
import org.lwjgl.opengl.GL43C;
import org.lwjgl.opengl.GLCapabilities;

public enum GlIndirectMultiDrawFunctions {
    CORE {
        @Override
        public void glMultiDrawArraysIndirect(int mode, long indirect, int primcount, int stride) {
            GL43C.glMultiDrawArraysIndirect(mode, indirect, primcount, stride);
        }

        @Override
        public void glMultiDrawElementArraysIndirect(int mode, int type, long indirect, int primcount, int stride) {
            GL43C.glMultiDrawElementsIndirect(mode, type, indirect, primcount, stride);
        }
    },
    ARB {
        @Override
        public void glMultiDrawArraysIndirect(int mode, long indirect, int primcount, int stride) {
            ARBMultiDrawIndirect.glMultiDrawArraysIndirect(mode, indirect, primcount, stride);
        }

        @Override
        public void glMultiDrawElementArraysIndirect(int mode, int type, long indirect, int primcount, int stride) {
            ARBMultiDrawIndirect.glMultiDrawElementsIndirect(mode, type, indirect, primcount, stride);
        }
    },
    UNSUPPORTED {
        @Override
        public void glMultiDrawArraysIndirect(int mode, long indirect, int primcount, int stride) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void glMultiDrawElementArraysIndirect(int mode, int type, long indirect, int primcount, int stride) {
            throw new UnsupportedOperationException();
        }
    };

    public static GlIndirectMultiDrawFunctions load(GLCapabilities capabilities) {
        if (MultidrawChunkRenderBackend.isWindowsIntelDriver()) {
            return UNSUPPORTED;
        } else if (capabilities.OpenGL43) {
            return CORE;
        } else if (capabilities.GL_ARB_multi_draw_indirect && capabilities.GL_ARB_draw_indirect) {
            return ARB;
        } else {
            return UNSUPPORTED;
        }
    }

    public abstract void glMultiDrawArraysIndirect(int mode, long indirect, int primcount, int stride);

    public abstract void glMultiDrawElementArraysIndirect(int mode, int type, long indirect, int primcount, int stride);
}
