package me.jellysquid.mods.sodium.client.gl.buffer;

import me.jellysquid.mods.sodium.client.gl.GlHandle;
import net.minecraft.client.render.VertexFormat;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;

public abstract class GlBuffer extends GlHandle {
    protected static final VertexBufferFunctions bufferFuncs = VertexBufferFunctions.pickBest(GL.getCapabilities());

    protected final int target;

    protected VertexFormat vertexFormat;
    protected int vertexCount = 0;

    protected GlBuffer(int target) {
        this.target = target;
        this.setHandle(bufferFuncs.glGenBuffers());
    }

    public void unbind() {
        bufferFuncs.glBindBuffer(this.target, 0);
    }

    public void bind() {
        bufferFuncs.glBindBuffer(this.target, this.handle());
    }

    public void drawArrays(int mode) {
        GL11.glDrawArrays(mode, 0, this.vertexCount);
    }

    public abstract void upload(BufferUploadData data);

    public void delete() {
        bufferFuncs.glDeleteBuffers(this.handle());

        this.invalidateHandle();
    }

    public enum VertexBufferFunctions {
        BASE {
            @Override
            public void glBindBuffer(int target, int id) {
                GL15.glBindBuffer(target, id);
            }

            @Override
            public int glGenBuffers() {
                return GL15.glGenBuffers();
            }

            @Override
            public void glDeleteBuffers(int id) {
                GL15.glDeleteBuffers(id);
            }

            @Override
            public void glBufferData(int target, ByteBuffer data, int usage) {
                GL15.glBufferData(target, data, usage);
            }
        },
        ARB {
            @Override
            public void glBindBuffer(int target, int id) {
                ARBVertexBufferObject.glBindBufferARB(target, id);
            }

            @Override
            public int glGenBuffers() {
                return ARBVertexBufferObject.glGenBuffersARB();
            }

            @Override
            public void glDeleteBuffers(int id) {
                ARBVertexBufferObject.glDeleteBuffersARB(id);
            }

            @Override
            public void glBufferData(int target, ByteBuffer data, int usage) {
                ARBVertexBufferObject.glBufferDataARB(target, data, usage);
            }
        },
        UNSUPPORTED {
            @Override
            public void glBindBuffer(int target, int id) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int glGenBuffers() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void glDeleteBuffers(int id) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void glBufferData(int target, ByteBuffer data, int usage) {
                throw new UnsupportedOperationException();
            }
        };

        public static VertexBufferFunctions pickBest(GLCapabilities capabilities) {
            if (capabilities.OpenGL15) {
                return BASE;
            } else if (capabilities.GL_ARB_vertex_buffer_object) {
                return ARB;
            }

            return UNSUPPORTED;
        }

        public abstract void glBindBuffer(int target, int id);

        public abstract int glGenBuffers();

        public abstract void glDeleteBuffers(int id);

        public abstract void glBufferData(int target, ByteBuffer data, int usage);
    }

}
