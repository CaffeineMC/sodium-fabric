package me.jellysquid.mods.sodium.render.chunk.draw;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.opengl.attribute.VertexFormat;
import me.jellysquid.mods.sodium.opengl.device.RenderDevice;
import me.jellysquid.mods.sodium.opengl.shader.Program;
import me.jellysquid.mods.sodium.opengl.shader.ProgramCommandList;
import me.jellysquid.mods.sodium.opengl.shader.ShaderDescription;
import me.jellysquid.mods.sodium.opengl.shader.ShaderType;
import me.jellysquid.mods.sodium.render.chunk.passes.ChunkRenderPass;
import me.jellysquid.mods.sodium.render.chunk.shader.ChunkFogMode;
import me.jellysquid.mods.sodium.render.chunk.shader.ChunkShaderBindingPoints;
import me.jellysquid.mods.sodium.render.chunk.shader.ChunkShaderInterface;
import me.jellysquid.mods.sodium.render.chunk.shader.ChunkShaderOptions;
import me.jellysquid.mods.sodium.render.shader.ShaderConstants;
import me.jellysquid.mods.sodium.render.shader.ShaderLoader;
import me.jellysquid.mods.sodium.render.shader.ShaderParser;
import me.jellysquid.mods.sodium.render.terrain.format.TerrainMeshAttribute;
import me.jellysquid.mods.sodium.render.terrain.format.TerrainVertexType;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.function.Consumer;

public abstract class ShaderChunkRenderer implements ChunkRenderer {
    private final Map<ChunkShaderOptions, Program<ChunkShaderInterface>> programs = new Object2ObjectOpenHashMap<>();

    protected final TerrainVertexType vertexType;
    protected final VertexFormat<TerrainMeshAttribute> vertexFormat;

    protected final RenderDevice device;

    public ShaderChunkRenderer(RenderDevice device, TerrainVertexType vertexType) {
        this.device = device;
        this.vertexType = vertexType;
        this.vertexFormat = vertexType.getCustomVertexFormat();
    }

    protected Program<ChunkShaderInterface> compileProgram(ChunkShaderOptions options) {
        Program<ChunkShaderInterface> program = this.programs.get(options);

        if (program == null) {
            this.programs.put(options, program = this.createShader("blocks/block_layer_opaque", options));
        }

        return program;
    }

    private Program<ChunkShaderInterface> createShader(String path, ChunkShaderOptions options) {
        ShaderConstants constants = options.constants();

        var vertShader = ShaderParser.parseShader(ShaderLoader.MINECRAFT_ASSETS, new Identifier("sodium", path + ".vsh"), constants);
        var fragShader = ShaderParser.parseShader(ShaderLoader.MINECRAFT_ASSETS, new Identifier("sodium", path + ".fsh"), constants);

        var desc = ShaderDescription.builder()
                .addShaderSource(ShaderType.VERTEX, vertShader)
                .addShaderSource(ShaderType.FRAGMENT, fragShader)
                .addAttributeBinding("a_PosId", ChunkShaderBindingPoints.ATTRIBUTE_POSITION_ID)
                .addAttributeBinding("a_Color", ChunkShaderBindingPoints.ATTRIBUTE_COLOR)
                .addAttributeBinding("a_TexCoord", ChunkShaderBindingPoints.ATTRIBUTE_BLOCK_TEXTURE)
                .addAttributeBinding("a_LightCoord", ChunkShaderBindingPoints.ATTRIBUTE_LIGHT_TEXTURE)
                .addFragmentBinding("fragColor", ChunkShaderBindingPoints.FRAG_COLOR)
                .build();

        return this.device.createProgram(desc, (binder) -> new ChunkShaderInterface(binder, options));
    }

    protected void beginRendering(ChunkRenderPass pass, RenderDevice.ProgramGate<ChunkShaderInterface> gate) {
        var options = new ChunkShaderOptions(ChunkFogMode.SMOOTH, pass, this.vertexType);
        var program = this.compileProgram(options);

        this.device.useProgram(program, (programCommandList, programInterface) -> {
            programInterface.setup();
            gate.run(programCommandList, programInterface);
        });
    }

    @Override
    public void delete() {
        for (var program : this.programs.values()) {
            this.device.deleteProgram(program);
        }

        this.programs.clear();
    }

    @Override
    public TerrainVertexType getVertexType() {
        return this.vertexType;
    }
}
