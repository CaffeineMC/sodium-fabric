package me.jellysquid.mods.sodium.client.gl.buffer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL31;

import java.nio.ByteBuffer;

public class GlBufferTexture {
    private final GlBuffer buffer;

    private final int glTexHandle;

    private final int textureNum;

    public GlBufferTexture(GlBuffer buffer, int textureNum) {
        this.buffer = buffer;
        this.glTexHandle = GlStateManager._genTexture();
        this.textureNum = textureNum;
    }

    public int getTextureNum() {
        return textureNum;
    }

    public void bind() {
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, this.glTexHandle);
        GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, GL31.GL_R32UI, this.buffer.handle());
        RenderSystem.setShaderTexture(this.textureNum, this.glTexHandle);
    }
}
