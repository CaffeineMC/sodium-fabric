package me.jellysquid.mods.sodium.client.render.chunk.shader.texture;

import com.mojang.blaze3d.platform.GlStateManager;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ShaderComponent;
import net.minecraft.client.texture.AbstractTexture;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

public abstract class ChunkProgramTextureComponent implements ShaderComponent {
    protected void bindTexture(AbstractTexture texture, ChunkProgramTextureUnit unit) {
        if (texture == null) {
            throw new IllegalStateException("Texture is absent for unit: " + unit);
        }

        GlStateManager.activeTexture(GL15.GL_TEXTURE0 + unit.ordinal());
        GlStateManager.bindTexture(texture.getGlId());
    }

    protected void bindUniform(int location, ChunkProgramTextureUnit unit) {
        GL20.glUniform1i(location, unit.ordinal());
    }

    public abstract void setMipmapping(boolean mipped);
}
