package me.jellysquid.mods.sodium.render.shader;

import me.jellysquid.mods.thingl.shader.ShaderConstants;
import net.minecraft.util.Identifier;

public class ShaderParseContext {
    private final ShaderLoader loader;
    private final ShaderConstants constants;

    public ShaderParseContext(ShaderLoader loader, ShaderConstants constants) {
        this.loader = loader;
        this.constants = constants;
    }

    public String importShader(Identifier identifier) {
        return this.loader.parseShader(identifier, ShaderConstants.empty());
    }

    public ShaderConstants getConstants() {
        return this.constants;
    }
}
