package me.jellysquid.mods.sodium.client.render.chunk.shader.texture;

import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkProgram;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.SpriteAtlasTexture;
import org.lwjgl.opengl.GL20;

public class ChunkProgramSingleTexture extends ChunkProgramTextureComponent {
    private final int uBlockTex;
    private final int uLightTex;

    public ChunkProgramSingleTexture(ChunkProgram program) {
        this.uBlockTex = program.getUniformLocation("u_BlockTex");
        this.uLightTex = program.getUniformLocation("u_LightTex");
    }

    @Override
    public void bind() {
        GL20.glUniform1i(this.uBlockTex, ChunkProgramTextureUnit.BLOCK_ATLAS.ordinal());
        GL20.glUniform1i(this.uLightTex, ChunkProgramTextureUnit.LIGHT_TEX.ordinal());
    }

    @Override
    public void unbind() {

    }

    @Override
    public void delete() {

    }

    @Override
    public void setMipmapping(boolean mipped) {
        MinecraftClient.getInstance().getTextureManager()
                .getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX)
                .setFilter(false, mipped);
    }
}
