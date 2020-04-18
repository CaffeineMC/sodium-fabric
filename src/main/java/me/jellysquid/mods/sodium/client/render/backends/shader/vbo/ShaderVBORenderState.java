package me.jellysquid.mods.sodium.client.render.backends.shader.vbo;

import me.jellysquid.mods.sodium.client.gl.attribute.GlAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.render.backends.ChunkRenderState;
import net.minecraft.client.util.math.Vector3d;
import org.lwjgl.opengl.GL20;

public class ShaderVBORenderState implements ChunkRenderState {
    private final GlBuffer buffer;
    private final Vector3d translation;

    public ShaderVBORenderState(GlBuffer buffer, Vector3d translation) {
        this.translation = translation;
        this.buffer = buffer;
    }

    public void bind(GlAttributeBinding[] attributes) {
        this.buffer.bind();

        for (GlAttributeBinding binding : attributes) {
            GL20.glVertexAttribPointer(binding.index, binding.count, binding.format, binding.normalized, binding.stride, binding.pointer);
        }
    }

    public void draw(int mode) {
        this.buffer.drawArrays(mode);
    }

    public void unbind() {
        this.buffer.unbind();
    }

    @Override
    public void delete() {
        this.buffer.delete();
    }

    public Vector3d getTranslation() {
        return this.translation;
    }
}
