package me.jellysquid.mods.sodium.client.render.chunk.shader.texture;

import me.jellysquid.mods.sodium.client.gl.sampler.GlSampler;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkProgram;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureManager;
import org.lwjgl.opengl.GL11;

public class ChunkProgramMultiTexture extends ChunkProgramTextureComponent {
    private final int uBlockTex, uBlockMippedTex;
    private final int uLightTex;

    private final GlSampler blockTexSampler, blockTexMippedSampler, lightTexSampler;

    public ChunkProgramMultiTexture(ChunkProgram program) {
        this.uBlockTex = program.getUniformLocation("u_BlockTex");
        this.uBlockMippedTex = program.getUniformLocation("u_BlockTexMipped");
        this.uLightTex = program.getUniformLocation("u_LightTex");

        this.blockTexSampler = new GlSampler();
        this.blockTexSampler.setParameter(GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        this.blockTexSampler.setParameter(GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

        this.blockTexMippedSampler = new GlSampler();
        this.blockTexMippedSampler.setParameter(GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST_MIPMAP_LINEAR);
        this.blockTexMippedSampler.setParameter(GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

        this.lightTexSampler = new GlSampler();
        this.lightTexSampler.setParameter(GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        this.lightTexSampler.setParameter(GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        this.lightTexSampler.setParameter(GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        this.lightTexSampler.setParameter(GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
    }

    @Override
    public void bind() {
        MinecraftClient client = MinecraftClient.getInstance();

        TextureManager textureManager = client.getTextureManager();

        LightmapTextureManagerAccessor lightmapTextureManager =
                ((LightmapTextureManagerAccessor) client.gameRenderer.getLightmapTextureManager());

        AbstractTexture blockAtlasTex = textureManager.getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
        AbstractTexture lightTex = lightmapTextureManager.getTexture();

        this.bindTexture(blockAtlasTex, ChunkProgramTextureUnit.BLOCK_ATLAS);
        this.bindTexture(blockAtlasTex, ChunkProgramTextureUnit.BLOCK_ATLAS_MIPPED);
        this.bindTexture(lightTex, ChunkProgramTextureUnit.LIGHT_TEX);

        this.blockTexSampler.bindToTextureUnit(ChunkProgramTextureUnit.BLOCK_ATLAS.ordinal());
        this.blockTexMippedSampler.bindToTextureUnit(ChunkProgramTextureUnit.BLOCK_ATLAS_MIPPED.ordinal());
        this.lightTexSampler.bindToTextureUnit(ChunkProgramTextureUnit.LIGHT_TEX.ordinal());

        this.bindUniform(this.uBlockTex, ChunkProgramTextureUnit.BLOCK_ATLAS);
        this.bindUniform(this.uBlockMippedTex, ChunkProgramTextureUnit.BLOCK_ATLAS_MIPPED);
        this.bindUniform(this.uLightTex, ChunkProgramTextureUnit.LIGHT_TEX);
    }

    public void unbind() {
        this.blockTexSampler.unbindFromTextureUnit(ChunkProgramTextureUnit.BLOCK_ATLAS.ordinal());
        this.blockTexMippedSampler.unbindFromTextureUnit(ChunkProgramTextureUnit.BLOCK_ATLAS.ordinal());
        this.blockTexSampler.unbindFromTextureUnit(ChunkProgramTextureUnit.LIGHT_TEX.ordinal());
    }

    public void delete() {
        this.blockTexSampler.delete();
        this.blockTexMippedSampler.delete();
    }

    @Override
    public void setMipmapping(boolean mipped) {

    }
}
