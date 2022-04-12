package net.caffeinemc.gradle.shaderc.compiler.options;

import org.lwjgl.util.shaderc.Shaderc;

public enum ShaderOptimizationLevel {
    None(Shaderc.shaderc_optimization_level_zero),
    Size(Shaderc.shaderc_optimization_level_size),
    Performance(Shaderc.shaderc_optimization_level_performance);

    private final int id;

    ShaderOptimizationLevel(int id) {
        this.id = id;
    }

    public int id() {
        return this.id;
    }
}
