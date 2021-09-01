package me.jellysquid.mods.sodium.client.render.chunk;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gl.shader.*;
import me.jellysquid.mods.sodium.client.gl.texture.GlSampler;
import me.jellysquid.mods.sodium.client.gl.texture.GlTexture;
import me.jellysquid.mods.sodium.client.gl.texture.TextureData;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.LightmapTextureManagerAccessor;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.shader.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.GL46C;

import java.util.EnumMap;
import java.util.Map;

public abstract class ShaderChunkRenderer implements ChunkRenderer {
    private final Map<ChunkShaderOptions, GlProgram<ChunkShaderInterface>> programs = new Object2ObjectOpenHashMap<>();

    protected final ChunkVertexType vertexType;
    protected final GlVertexFormat<ChunkMeshAttribute> vertexFormat;

    protected final RenderDevice device;

    protected GlProgram<ChunkShaderInterface> activeProgram;

    private final Map<ChunkShaderTextureUnit, GlSampler> samplers = new EnumMap<>(ChunkShaderTextureUnit.class);
    private final GlTexture stippleTexture;
    private final float detailDistance;

    public ShaderChunkRenderer(RenderDevice device, ChunkVertexType vertexType, float detailDistance) {
        this.device = device;
        this.vertexType = vertexType;
        this.vertexFormat = vertexType.getCustomVertexFormat();
        this.detailDistance = detailDistance;

        try (TextureData data = TextureData.loadInternal("/assets/sodium/textures/shader/stipple.png")) {
            this.stippleTexture = new GlTexture();
            this.stippleTexture.setTextureData(data);
        }

        for (ChunkShaderTextureUnit unit : ChunkShaderTextureUnit.values()) {
            this.samplers.put(unit, new GlSampler());
        }

        var blockTexSampler = this.samplers.get(ChunkShaderTextureUnit.BLOCK_TEXTURE);
        blockTexSampler.setParameter(GL33C.GL_TEXTURE_MIN_FILTER, GL33C.GL_NEAREST);
        blockTexSampler.setParameter(GL33C.GL_TEXTURE_MAG_FILTER, GL33C.GL_NEAREST);

        var blockTexMippedSampler = this.samplers.get(ChunkShaderTextureUnit.BLOCK_MIPPED_TEXTURE);
        blockTexMippedSampler.setParameter(GL33C.GL_TEXTURE_MIN_FILTER, GL33C.GL_NEAREST_MIPMAP_LINEAR);
        blockTexMippedSampler.setParameter(GL33C.GL_TEXTURE_MAG_FILTER, GL33C.GL_NEAREST);

        var lightTexSampler = this.samplers.get(ChunkShaderTextureUnit.LIGHT_TEXTURE);
        lightTexSampler.setParameter(GL33C.GL_TEXTURE_MIN_FILTER, GL33C.GL_LINEAR);
        lightTexSampler.setParameter(GL33C.GL_TEXTURE_MAG_FILTER, GL33C.GL_LINEAR);
        lightTexSampler.setParameter(GL33C.GL_TEXTURE_WRAP_S, GL33C.GL_CLAMP_TO_EDGE);
        lightTexSampler.setParameter(GL33C.GL_TEXTURE_WRAP_T, GL33C.GL_CLAMP_TO_EDGE);

        var stippleSampler = this.samplers.get(ChunkShaderTextureUnit.STIPPLE_TEXTURE);
        stippleSampler.setParameter(GL33C.GL_TEXTURE_MIN_FILTER, GL33C.GL_NEAREST);
        stippleSampler.setParameter(GL33C.GL_TEXTURE_MAG_FILTER, GL33C.GL_NEAREST);
        stippleSampler.setParameter(GL33C.GL_TEXTURE_WRAP_S, GL33C.GL_REPEAT);
        stippleSampler.setParameter(GL33C.GL_TEXTURE_WRAP_T, GL33C.GL_REPEAT);
    }

    protected GlProgram<ChunkShaderInterface> compileProgram(ChunkShaderOptions options) {
        GlProgram<ChunkShaderInterface> program = this.programs.get(options);

        if (program == null) {
            this.programs.put(options, program = this.createShader("blocks/block_layer_opaque", options));
        }

        return program;
    }

    private GlProgram<ChunkShaderInterface> createShader(String path, ChunkShaderOptions options) {
        ShaderConstants constants = options.constants();

        GlShader vertShader = ShaderLoader.loadShader(ShaderType.VERTEX,
                new Identifier("sodium", path + ".vsh"), constants);
        
        GlShader fragShader = ShaderLoader.loadShader(ShaderType.FRAGMENT,
                new Identifier("sodium", path + ".fsh"), constants);

        try {
            return GlProgram.builder(new Identifier("sodium", "chunk_shader"))
                    .attachShader(vertShader)
                    .attachShader(fragShader)
                    .bindAttribute("a_Pos", ChunkShaderBindingPoints.ATTRIBUTE_POSITION_ID)
                    .bindAttribute("a_Color", ChunkShaderBindingPoints.ATTRIBUTE_COLOR)
                    .bindAttribute("a_TexCoord", ChunkShaderBindingPoints.ATTRIBUTE_BLOCK_TEXTURE)
                    .bindAttribute("a_LightCoord", ChunkShaderBindingPoints.ATTRIBUTE_LIGHT_TEXTURE)
                    .bindAttribute("a_Options", ChunkShaderBindingPoints.ATTRIBUTE_BLOCK_FLAGS)
                    .bindFragmentData("fragColor", ChunkShaderBindingPoints.FRAG_COLOR)
                    .link((shader) -> new ChunkShaderInterface(shader, options));
        } finally {
            vertShader.delete();
            fragShader.delete();
        }
    }

    protected void begin(BlockRenderPass pass) {
        ChunkShaderOptions options = new ChunkShaderOptions(ChunkFogMode.SMOOTH, pass);

        this.activeProgram = this.compileProgram(options);
        this.activeProgram.bind();

        var shader = this.activeProgram.getInterface();
        shader.setup(this.vertexType);
        shader.setDetailParameters(this.detailDistance);

        MinecraftClient client = MinecraftClient.getInstance();
        TextureManager textureManager = client.getTextureManager();

        LightmapTextureManagerAccessor lightmapTextureManager =
                ((LightmapTextureManagerAccessor) client.gameRenderer.getLightmapTextureManager());

        AbstractTexture blockAtlasTex = textureManager.getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
        AbstractTexture lightTex = lightmapTextureManager.getTexture();

        this.bindTexture(ChunkShaderTextureUnit.BLOCK_TEXTURE, blockAtlasTex.getGlId());
        this.bindTexture(ChunkShaderTextureUnit.BLOCK_MIPPED_TEXTURE, blockAtlasTex.getGlId());
        this.bindTexture(ChunkShaderTextureUnit.LIGHT_TEXTURE, lightTex.getGlId());
        this.bindTexture(ChunkShaderTextureUnit.STIPPLE_TEXTURE, this.stippleTexture.handle());
    }

    private void bindTexture(ChunkShaderTextureUnit unit, int texture) {
        RenderSystem.activeTexture(GL32C.GL_TEXTURE0 + unit.id());
        RenderSystem.bindTexture(texture);

        GlSampler sampler = this.samplers.get(unit);
        sampler.bindTextureUnit(unit.id());
    }

    protected void end() {
        this.activeProgram.unbind();
        this.activeProgram = null;

        for (Map.Entry<ChunkShaderTextureUnit, GlSampler> entry : this.samplers.entrySet()) {
            entry.getValue().unbindTextureUnit(entry.getKey().id());
        }
    }

    @Override
    public void delete() {
        this.programs.values()
                .forEach(GlProgram::delete);
        this.programs.clear();

        this.stippleTexture.delete();

        for (GlSampler sampler : this.samplers.values()) {
            sampler.delete();
        }
    }

    @Override
    public ChunkVertexType getVertexType() {
        return this.vertexType;
    }
}
