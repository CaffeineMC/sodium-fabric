package net.caffeinemc.gradle.shaderc.compiler.options;

import org.lwjgl.util.shaderc.Shaderc;

public enum ShaderProfile {
    None(Shaderc.shaderc_profile_none),
    Core(Shaderc.shaderc_profile_core),
    Compatibility(Shaderc.shaderc_profile_compatibility),
    ES(Shaderc.shaderc_profile_es);

    private final int id;

    ShaderProfile(int id) {
        this.id = id;
    }

    public int id() {
        return this.id;
    }
}
