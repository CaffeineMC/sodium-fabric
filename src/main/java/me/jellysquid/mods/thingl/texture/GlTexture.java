package me.jellysquid.mods.thingl.texture;

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.thingl.GlObject;
import me.jellysquid.mods.thingl.device.RenderDevice;
import me.jellysquid.mods.thingl.device.RenderDeviceImpl;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL11C;

public class GlTexture extends GlObject {
    public GlTexture(RenderDeviceImpl device)  {
        super(device);

        this.setHandle(TextureUtil.generateTextureId());
    }

    public void setTextureData(TextureData data) {
        if (data.isDisposed()) throw new IllegalStateException("Texture data is invalid");

        // TODO: move to state tracker
        var prev = RenderSystem.getTextureId(GL11C.GL_TEXTURE_2D);

        RenderSystem.bindTexture(this.handle());

        // TODO: why is this necessary? what is changing this?
        GL11.glPixelStorei(GL11C.GL_UNPACK_SWAP_BYTES, 0);
        GL11.glPixelStorei(GL11C.GL_UNPACK_LSB_FIRST, 0);
        GL11.glPixelStorei(GL11C.GL_UNPACK_ROW_LENGTH, 0);
        GL11.glPixelStorei(GL11C.GL_UNPACK_SKIP_ROWS, 0);
        GL11.glPixelStorei(GL11C.GL_UNPACK_SKIP_PIXELS, 0);
        GL11.glPixelStorei(GL11C.GL_UNPACK_ALIGNMENT, 4);

        // TODO: allow formats to be swapped
        GL11C.glTexImage2D(GL11C.GL_TEXTURE_2D, 0, GL11C.GL_RGBA, data.width, data.height, 0, GL11C.GL_RGBA, GL11C.GL_UNSIGNED_BYTE, data.buffer);

        RenderSystem.bindTexture(prev);
    }

    public void delete() {
        GL11C.glDeleteTextures(this.handle());
        this.invalidateHandle();
    }
}
