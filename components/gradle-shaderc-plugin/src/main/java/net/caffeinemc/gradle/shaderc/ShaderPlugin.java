package net.caffeinemc.gradle.shaderc;

import net.caffeinemc.gradle.shaderc.compiler.ShaderCompiler;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class ShaderPlugin implements Plugin<Project> {
    public static final ShaderCompiler COMPILER = new ShaderCompiler();

    @Override
    public void apply(Project project) {
        project.getTasks()
                .register("compileShaders", CompileTask.class);
    }
}

