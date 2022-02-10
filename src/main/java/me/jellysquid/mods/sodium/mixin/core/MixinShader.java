package me.jellysquid.mods.sodium.mixin.core;

import me.jellysquid.mods.sodium.interop.vanilla.mixin.ShaderExtended;
import me.jellysquid.mods.sodium.opengl.device.RenderDevice;
import me.jellysquid.mods.sodium.opengl.shader.ShaderDescription;
import me.jellysquid.mods.sodium.opengl.shader.ShaderType;
import me.jellysquid.mods.sodium.render.immediate.VanillaShaderInterface;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL45C;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.List;

@Mixin(ShaderInstance.class)
public class MixinShader implements ShaderExtended {
    @Shadow
    @Final
    private Program vertexProgram;

    @Shadow
    @Final
    private Program fragmentProgram;

    @Shadow
    @Final
    private List<String> samplerNames;

    @Shadow
    @Final
    private VertexFormat vertexFormat;

    private final RenderDevice device = RenderDevice.INSTANCE;
    private me.jellysquid.mods.sodium.opengl.shader.Program<VanillaShaderInterface> sodium$shader;

    private ShaderDescription createShaderDescription() {
        var desc = ShaderDescription.builder();

        // TODO: Fetch shader binary program instead of source code to speed this up
        if (this.vertexProgram != null) {
            desc.addShaderSource(ShaderType.VERTEX, getShaderSource(this.vertexProgram));
        }

        if (this.fragmentProgram != null) {
            desc.addShaderSource(ShaderType.FRAGMENT, getShaderSource(this.fragmentProgram));
        }

        int nextAttributeIndex = 0;

        for (String attributeName : this.vertexFormat.getElementAttributeNames()) {
            desc.addAttributeBinding(attributeName, nextAttributeIndex++);
        }

        return desc.build();
    }

    @Override
    public me.jellysquid.mods.sodium.opengl.shader.Program<VanillaShaderInterface> sodium$getShader() {
        if (this.sodium$shader == null) {
            this.sodium$shader = this.createShader();
        }

        return this.sodium$shader;
    }

    private me.jellysquid.mods.sodium.opengl.shader.Program<VanillaShaderInterface> createShader() {
        return this.device.createProgram(this.createShaderDescription(),
                (context) -> new VanillaShaderInterface(context, this.samplerNames));
    }

    private static String getShaderSource(Program program) {
        return GL45C.glGetShaderSource(program.getId());
    }

    @Inject(method = "close", at = @At("RETURN"))
    private void postClose(CallbackInfo ci) {
        if (this.sodium$shader != null) {
            this.device.deleteProgram(this.sodium$shader);
            this.sodium$shader = null;
        }
    }
}
