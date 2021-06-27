package me.jellysquid.mods.sodium.client.gl.device;

import me.jellysquid.mods.sodium.client.gl.array.GlVertexArray;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferTarget;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferUsage;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.gl.state.GlStateTracker;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlPrimitiveType;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlTessellation;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlVertexArrayTessellation;
import me.jellysquid.mods.sodium.client.gl.tessellation.TessellationBinding;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL31C;
import org.lwjgl.opengl.GL32C;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class GLRenderDevice implements RenderDevice {
    private final GlStateTracker stateTracker = new GlStateTracker();
    private final CommandList commandList = new ImmediateCommandList(this.stateTracker);
    private final DrawCommandList drawCommandList = new ImmediateDrawCommandList();

    private boolean isActive;
    private GlTessellation activeTessellation;

    @Override
    public CommandList createCommandList() {
        GLRenderDevice.this.checkDeviceActive();

        return this.commandList;
    }

    @Override
    public void makeActive() {
        if (this.isActive) {
            return;
        }

        this.stateTracker.clearRestoreState();
        this.isActive = true;
    }

    @Override
    public void makeInactive() {
        if (!this.isActive) {
            return;
        }

        this.stateTracker.applyRestoreState();
        this.isActive = false;
    }

    private void checkDeviceActive() {
        if (!this.isActive) {
            throw new IllegalStateException("Tried to access device from unmanaged context");
        }
    }

    private class ImmediateCommandList implements CommandList {
        private final GlStateTracker stateTracker;

        private ImmediateCommandList(GlStateTracker stateTracker) {
            this.stateTracker = stateTracker;
        }

        @Override
        public void bindVertexArray(GlVertexArray array) {
            if (this.stateTracker.makeVertexArrayActive(array)) {
                GL30C.glBindVertexArray(array.handle());
            }
        }

        @Override
        public void uploadData(GlMutableBuffer glBuffer, ByteBuffer byteBuffer) {
            this.bindBuffer(GlBufferTarget.ARRAY_BUFFER, glBuffer);

            GL20C.glBufferData(GlBufferTarget.ARRAY_BUFFER.getTargetParameter(), byteBuffer, glBuffer.getUsageHint().getId());

            glBuffer.setSize(byteBuffer.limit());
        }

        @Override
        public void copyBufferSubData(GlBuffer src, GlMutableBuffer dst, long readOffset, long writeOffset, long bytes) {
            if (writeOffset + bytes > dst.getSize()) {
                throw new IllegalArgumentException("Not enough space in destination buffer (writeOffset + bytes > bufferSize)");
            }

            this.bindBuffer(GlBufferTarget.COPY_READ_BUFFER, src);
            this.bindBuffer(GlBufferTarget.COPY_WRITE_BUFFER, dst);

            GL31C.glCopyBufferSubData(GL31C.GL_COPY_READ_BUFFER, GL31C.GL_COPY_WRITE_BUFFER, readOffset, writeOffset, bytes);
        }

        @Override
        public void bindBuffer(GlBufferTarget target, GlBuffer buffer) {
            if (this.stateTracker.makeBufferActive(target, buffer)) {
                GL20C.glBindBuffer(target.getTargetParameter(), buffer.handle());
            }
        }

        @Override
        public void unbindVertexArray() {
            if (this.stateTracker.makeVertexArrayActive(null)) {
                GL30C.glBindVertexArray(GlVertexArray.NULL_ARRAY_ID);
            }
        }

        @Override
        public void invalidateBuffer(GlMutableBuffer glBuffer) {
            this.allocateBuffer(GlBufferTarget.ARRAY_BUFFER, glBuffer, 0L);
        }

        @Override
        public void allocateBuffer(GlBufferTarget target, GlMutableBuffer buffer, long bufferSize) {
            this.bindBuffer(target, buffer);

            GL20C.glBufferData(target.getTargetParameter(), bufferSize, buffer.getUsageHint().getId());
            buffer.setSize(bufferSize);
        }

        @Override
        public void deleteBuffer(GlBuffer buffer) {
            int handle = buffer.handle();
            buffer.invalidateHandle();

            GL20C.glDeleteBuffers(handle);
        }

        @Override
        public void deleteVertexArray(GlVertexArray array) {
            int handle = array.handle();
            array.invalidateHandle();

            GL30C.glDeleteVertexArrays(handle);
        }

        @Override
        public void flush() {
            // NO-OP
        }

        @Override
        public DrawCommandList beginTessellating(GlTessellation tessellation) {
            GLRenderDevice.this.activeTessellation = tessellation;
            GLRenderDevice.this.activeTessellation.bind(GLRenderDevice.this.commandList);

            return GLRenderDevice.this.drawCommandList;
        }

        @Override
        public void deleteTessellation(GlTessellation tessellation) {
            tessellation.delete(this);
        }

        @Override
        public GlMutableBuffer createMutableBuffer(GlBufferUsage usage) {
            return new GlMutableBuffer(GLRenderDevice.this, usage);
        }

        @Override
        public GlTessellation createTessellation(GlPrimitiveType primitiveType, TessellationBinding[] bindings, GlBuffer indexBuffer) {
            GlVertexArrayTessellation tessellation = new GlVertexArrayTessellation(new GlVertexArray(GLRenderDevice.this), primitiveType, bindings, indexBuffer);
            tessellation.init(this);

            return tessellation;
        }
    }

    private class ImmediateDrawCommandList implements DrawCommandList {
        public ImmediateDrawCommandList() {

        }

        @Override
        public void multiDrawElementsBaseVertex(PointerBuffer pointer, IntBuffer count, IntBuffer baseVertex) {
            GlPrimitiveType primitiveType = GLRenderDevice.this.activeTessellation.getPrimitiveType();
            GL32C.glMultiDrawElementsBaseVertex(primitiveType.getId(), count, GL20C.GL_UNSIGNED_INT, pointer, baseVertex);
        }

        @Override
        public void multiDrawElements(PointerBuffer pointer, IntBuffer count) {
            GlPrimitiveType primitiveType = GLRenderDevice.this.activeTessellation.getPrimitiveType();
            GL32C.glMultiDrawElements(primitiveType.getId(), count, GL20C.GL_UNSIGNED_INT, pointer);
        }

        @Override
        public void endTessellating() {
            GLRenderDevice.this.activeTessellation.unbind(GLRenderDevice.this.commandList);
            GLRenderDevice.this.activeTessellation = null;
        }

        @Override
        public void flush() {
            if (GLRenderDevice.this.activeTessellation != null) {
                this.endTessellating();
            }
        }
    }
}
