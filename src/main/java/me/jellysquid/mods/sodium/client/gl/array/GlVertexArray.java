package me.jellysquid.mods.sodium.client.gl.array;

import me.jellysquid.mods.sodium.client.gl.GlHandle;
import org.lwjgl.opengl.ARBVertexArrayObject;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLCapabilities;

public class GlVertexArray extends GlHandle {
    private static final VertexArrayFunctions func = VertexArrayFunctions.pickBest(GL.getCapabilities());

    public GlVertexArray() {
        this.setHandle(func.glGenVertexArrays());
    }

    public void unbind() {
        func.glBindVertexArray(0);
    }

    public static void unbindVertexArray(){
        func.glBindVertexArray(0);
    }

    public void bind() {
        func.glBindVertexArray(this.handle());
    }

    public void delete() {
        func.glDeleteVertexArrays(this.handle());

        this.invalidateHandle();
    }

    public static boolean isSupported() {
        return func != VertexArrayFunctions.UNSUPPORTED;
    }

    private enum VertexArrayFunctions {
        BASE {
            @Override
            public void glBindVertexArray(int id) {
                GL30.glBindVertexArray(id);
            }

            @Override
            public int glGenVertexArrays() {
                return GL30.glGenVertexArrays();
            }

            @Override
            public void glDeleteVertexArrays(int id) {
                GL30.glDeleteVertexArrays(id);
            }
        },
        ARB {
            @Override
            public void glBindVertexArray(int id) {
                ARBVertexArrayObject.glBindVertexArray(id);
            }

            @Override
            public int glGenVertexArrays() {
                return ARBVertexArrayObject.glGenVertexArrays();
            }

            @Override
            public void glDeleteVertexArrays(int id) {
                ARBVertexArrayObject.glDeleteVertexArrays(id);
            }
        },
        UNSUPPORTED {
            @Override
            public void glBindVertexArray(int id) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int glGenVertexArrays() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void glDeleteVertexArrays(int id) {
                throw new UnsupportedOperationException();
            }
        };

        public static VertexArrayFunctions pickBest(GLCapabilities capabilities) {
            if (capabilities.OpenGL30) {
                return BASE;
            } else if (capabilities.GL_ARB_vertex_array_object) {
                return ARB;
            }

            return UNSUPPORTED;
        }

        public abstract void glBindVertexArray(int id);

        public abstract int glGenVertexArrays();

        public abstract void glDeleteVertexArrays(int id);
    }
}
