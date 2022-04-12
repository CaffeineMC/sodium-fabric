package net.caffeinemc.gradle.shaderc.compiler.options;

import net.caffeinemc.gradle.shaderc.compiler.IncludeResolver;
import org.lwjgl.util.shaderc.Shaderc;

import java.lang.ref.Cleaner;
import java.lang.ref.PhantomReference;

public class ShaderCompilerOptions {
    private static final Cleaner CLEANER = Cleaner.create();

    private final long handle;
    private final IncludeResolver resolver;

    public ShaderCompilerOptions(IncludeResolver resolver) {
        var handle = Shaderc.shaderc_compile_options_initialize();
        CLEANER.register(this, () -> Shaderc.shaderc_compile_options_release(handle));

        Shaderc.shaderc_compile_options_set_include_callbacks(handle,
                IncludeResolver.RESOLVE_CALLBACK, IncludeResolver.RELEASE_CALLBACK, resolver.pointer());

        this.handle = handle;
        this.resolver = resolver;
    }

    public void setVersion(CompilerIdentifier identifier) {
        Shaderc.shaderc_compile_options_set_forced_version_profile(this.handle, identifier.version(), identifier.profile().id());
    }

    public void setOptimizationLevel(ShaderOptimizationLevel level) {
        Shaderc.shaderc_compile_options_set_optimization_level(this.handle, level.id());
    }

    public void enablePedantry() {
        Shaderc.shaderc_compile_options_set_warnings_as_errors(this.handle);
    }

    public long handle() {
        return this.handle;
    }

}
