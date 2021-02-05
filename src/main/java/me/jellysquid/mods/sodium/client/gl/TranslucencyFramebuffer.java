package me.jellysquid.mods.sodium.client.gl;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.FramebufferInfo;
import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.sodium.client.gl.shader.GlProgram;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderConstants;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderLoader;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderType;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.texture.TextureUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.ARBTextureFloat;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;

public class TranslucencyFramebuffer extends Framebuffer {
    private static GlProgram clearProgram = null;

    private int accumAttachment;
    private int revealAttachment;
    private int depthAttachment;

    public TranslucencyFramebuffer(int width, int height, boolean useDepth, boolean getError) {
        super(width, height, useDepth, getError);
        this.accumAttachment = -1;
        this.revealAttachment = -1;
        this.depthAttachment = -1;
    }

    @Override
    public void delete() {
        super.delete();
        if (this.accumAttachment > -1) {
            TextureUtil.deleteId(this.accumAttachment);
            this.accumAttachment = -1;
        }
        if (this.revealAttachment > -1) {
            TextureUtil.deleteId(this.revealAttachment);
            this.revealAttachment = -1;
        }
        if (this.depthAttachment > -1) {
            TextureUtil.deleteId(this.depthAttachment);
            this.depthAttachment = -1;
        }
    }

    @Override
    public void initFbo(int width, int height, boolean getError) {
        RenderSystem.assertThread(RenderSystem::isOnRenderThreadOrInit);
        this.viewportWidth = width;
        this.viewportHeight = height;
        this.textureWidth = width;
        this.textureHeight = height;

        if (this.useDepthAttachment) {
            this.depthAttachment = TextureUtil.generateId();
            GlStateManager.bindTexture(this.depthAttachment);
            GlStateManager.texImage2D(GL20.GL_TEXTURE_2D, 0, GL20.GL_DEPTH_COMPONENT, width, height, 0, GL20.GL_DEPTH_COMPONENT, GL20.GL_FLOAT, null);
        }

        this.accumAttachment = TextureUtil.generateId();
        this.revealAttachment = TextureUtil.generateId();
        // This duplicates a vanilla bug, where framebuffer attachments change back to linear filtering when resized.
        // Shouldn't affect us because the translucency compositor samples texels directly, bypassing filtering altogether.
        this.setTexFilter(GL20.GL_LINEAR);

        GlStateManager.bindTexture(this.accumAttachment);
        GlStateManager.texImage2D(GL20.GL_TEXTURE_2D, 0, ARBTextureFloat.GL_RGBA16F_ARB, width, height, 0, GL20.GL_RGBA, GL20.GL_FLOAT, null);
        GlStateManager.bindTexture(this.revealAttachment);
        GlStateManager.texImage2D(GL20.GL_TEXTURE_2D, 0, GL20.GL_ALPHA8, width, height, 0, GL20.GL_ALPHA, GL20.GL_UNSIGNED_BYTE, null);

        this.fbo = GlStateManager.genFramebuffers();
        GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, this.fbo);
        GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT, GL20.GL_TEXTURE_2D, this.accumAttachment, 0);
        GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.COLOR_ATTACHMENT+1, GL20.GL_TEXTURE_2D, this.revealAttachment, 0);
        if (this.useDepthAttachment) {
            GlStateManager.framebufferTexture2D(FramebufferInfo.FRAME_BUFFER, FramebufferInfo.DEPTH_ATTACHMENT, GL20.GL_TEXTURE_2D, this.depthAttachment, 0);
        }
        this.checkFramebufferStatus();

        IntBuffer attachments = MemoryUtil.memAllocInt(2);
        attachments.put(0, FramebufferInfo.COLOR_ATTACHMENT);
        attachments.put(1, FramebufferInfo.COLOR_ATTACHMENT+1);
        GL20.glDrawBuffers(attachments);

        this.clear(getError);
        this.endRead();
    }

    @Override
    public void setTexFilter(int filter) {
        RenderSystem.assertThread(RenderSystem::isOnRenderThreadOrInit);
        this.texFilter = filter;

        GlStateManager.bindTexture(this.accumAttachment);
        GlStateManager.texParameter(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MIN_FILTER, filter);
        GlStateManager.texParameter(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAG_FILTER, filter);
        GlStateManager.texParameter(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_S, GL20.GL_CLAMP);
        GlStateManager.texParameter(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_R, GL20.GL_CLAMP);

        GlStateManager.bindTexture(this.revealAttachment);
        GlStateManager.texParameter(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MIN_FILTER, filter);
        GlStateManager.texParameter(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAG_FILTER, filter);
        GlStateManager.texParameter(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_S, GL20.GL_CLAMP);
        GlStateManager.texParameter(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_R, GL20.GL_CLAMP);

        GlStateManager.bindTexture(0);
    }

    @Override
    public void beginRead() {
        RenderSystem.assertThread(RenderSystem::isOnRenderThread);
        GlStateManager.bindTexture(this.accumAttachment);
    }

    @Override
    public void clear(boolean getError) {
        RenderSystem.assertThread(RenderSystem::isOnRenderThreadOrInit);
        this.beginWrite(true);

        if (TranslucencyFramebuffer.clearProgram == null) {
            ShaderConstants empty = ShaderConstants.builder().build();
            TranslucencyFramebuffer.clearProgram = GlProgram.builder(new Identifier("sodium", "translucency_clear"))
                .attachShader(ShaderLoader.loadShader(ShaderType.VERTEX, new Identifier("sodium", "fullscreen_gl20.v.glsl"), empty))
                .attachShader(ShaderLoader.loadShader(ShaderType.FRAGMENT, new Identifier("sodium", "translucency_clear_gl20.f.glsl"), empty))
                .build((program, name) -> new GlProgram(program, name) {});
        }
        // clearColor is ignored, but that shouldn't cause problems unless someone misuses this class
        // If we do need it in future, it can be done through a uniform
        TranslucencyFramebuffer.clearProgram.bind();
        GlStateManager.disableDepthTest();
        GlStateManager.disableBlend();
        GL20.glDrawArrays(GL20.GL_TRIANGLES, 0, 3);
        GlStateManager.enableBlend();
        GlStateManager.enableDepthTest();
        TranslucencyFramebuffer.clearProgram.unbind();

        if (this.useDepthAttachment) {
            GlStateManager.clear(GL20.GL_DEPTH_BUFFER_BIT, getError);
        }
        this.endWrite();
    }

    @Override
    public int getColorAttachment() {
        return this.accumAttachment;
    }
    @Override
    public int getDepthAttachment() {
        return this.depthAttachment;
    }

    public int getAccumAttachment() {
        return this.accumAttachment;
    }
    public int getRevealAttachment() {
        return this.revealAttachment;
    }
}
