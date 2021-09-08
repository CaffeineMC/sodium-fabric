package me.jellysquid.mods.sodium.render.chunk.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.render.shader.ShaderLoader;
import me.jellysquid.mods.thingl.attribute.GlVertexFormat;
import me.jellysquid.mods.thingl.device.CommandList;
import me.jellysquid.mods.thingl.device.RenderDevice;
import me.jellysquid.mods.thingl.shader.*;
import me.jellysquid.mods.thingl.texture.GlSampler;
import me.jellysquid.mods.thingl.texture.GlTexture;
import me.jellysquid.mods.thingl.texture.TextureData;
import me.jellysquid.mods.sodium.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.interop.vanilla.lightmap.LightmapTextureManagerAccessor;
import me.jellysquid.mods.sodium.render.chunk.format.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.render.chunk.shader.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GL33C;

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
        blockTexSampler.setParameter(GL33C.GL_TEXTURE_MIN_FILTER, GL33C.GL_NEAREST_MIPMAP_LINEAR);
        blockTexSampler.setParameter(GL33C.GL_TEXTURE_MAG_FILTER, GL33C.GL_NEAREST);
        blockTexSampler.setParameter(GL33C.GL_TEXTURE_LOD_BIAS, 0.0F);

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

    protected GlProgram<ChunkShaderInterface> compileProgram(CommandList commands, ChunkShaderOptions options) {
        GlProgram<ChunkShaderInterface> program = this.programs.get(options);

        if (program == null) {
            this.programs.put(options, program = this.createShader(commands, "blocks/block_layer_opaque", options));
        }

        return program;
    }

    private GlProgram<ChunkShaderInterface> createShader(CommandList commands, String path, ChunkShaderOptions options) {
        ShaderConstants constants = options.constants();

        GlShader vertShader = null;
        GlShader fragShader = null;

        var loader = new ShaderLoader(RenderDevice.INSTANCE);

        try {
            vertShader = loader.loadShader(ShaderType.VERTEX,
                    new Identifier("sodium", path + ".vsh"), constants);

            fragShader = loader.loadShader(ShaderType.FRAGMENT,
                    new Identifier("sodium", path + ".fsh"), constants);

            return commands.createProgram(new GlShader[] { vertShader, fragShader },
                    (ctx) -> new ChunkShaderInterface(ctx, options));
        } finally {
            if (vertShader != null) {
                vertShader.delete();
            }

            if (fragShader != null) {
                fragShader.delete();
            }
        }
    }

    protected void begin(CommandList commandList, BlockRenderPass pass) {
        ChunkShaderOptions options = new ChunkShaderOptions(ChunkFogMode.SMOOTH, pass);

        this.activeProgram = this.compileProgram(commandList, options);
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
