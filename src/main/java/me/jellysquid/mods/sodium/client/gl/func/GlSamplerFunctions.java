package me.jellysquid.mods.sodium.client.gl.func;

import org.lwjgl.opengl.ARBSamplerObjects;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GLCapabilities;

public enum GlSamplerFunctions {
    CORE {
        @Override
        public int glGenSamplers() {
            return GL33.glGenSamplers();
        }

        @Override
        public void glSamplerParameteri(int handle, int param, int value) {
            GL33.glSamplerParameteri(handle, param, value);
        }

        @Override
        public void glDeleteSamplers(int handle) {
            GL33.glDeleteSamplers(handle);
        }

        @Override
        public void glBindSampler(int texture, int handle) {
            GL33.glBindSampler(texture, handle);
        }
    },
    ARB {
        @Override
        public int glGenSamplers() {
            return ARBSamplerObjects.glGenSamplers();
        }

        @Override
        public void glSamplerParameteri(int handle, int param, int value) {
            ARBSamplerObjects.glSamplerParameteri(handle, param, value);
        }

        @Override
        public void glDeleteSamplers(int handle) {
            ARBSamplerObjects.glDeleteSamplers(handle);
        }

        @Override
        public void glBindSampler(int texture, int handle) {
            ARBSamplerObjects.glBindSampler(texture, handle);
        }
    },
    UNSUPPORTED {
        @Override
        public int glGenSamplers() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void glSamplerParameteri(int handle, int param, int value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void glDeleteSamplers(int handle) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void glBindSampler(int texture, int handle) {
            throw new UnsupportedOperationException();
        }
    };

    public abstract int glGenSamplers();

    public abstract void glSamplerParameteri(int handle, int param, int value);

    public abstract void glDeleteSamplers(int handle);

    public abstract void glBindSampler(int texture, int handle);

    static GlSamplerFunctions load(GLCapabilities capabilities) {
        if (capabilities.OpenGL33) {
            return GlSamplerFunctions.CORE;
        } else if (capabilities.GL_ARB_sampler_objects) {
            return GlSamplerFunctions.ARB;
        } else {
            return GlSamplerFunctions.UNSUPPORTED;
        }
    }
}
