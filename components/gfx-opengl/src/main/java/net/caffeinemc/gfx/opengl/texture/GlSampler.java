package net.caffeinemc.gfx.opengl.texture;

import net.caffeinemc.gfx.api.texture.Sampler;
import net.caffeinemc.gfx.api.texture.parameters.AddressMode;
import net.caffeinemc.gfx.api.texture.parameters.FilterMode;
import net.caffeinemc.gfx.api.texture.parameters.MipmapMode;
import net.caffeinemc.gfx.opengl.GlEnum;
import net.caffeinemc.gfx.opengl.GlObject;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.GL45C;

public class GlSampler extends GlObject implements Sampler {
    public GlSampler(
            @Nullable FilterMode minFilter,
            @Nullable MipmapMode mipmapMode,
            @Nullable FilterMode magFilter,
            @Nullable AddressMode addressModeU,
            @Nullable AddressMode addressModeV,
            @Nullable AddressMode addressModeW
    ) {
        int handle = GL33C.glGenSamplers();
        this.setHandle(handle);

        if (mipmapMode != null && minFilter == null) {
            // the initial value of minFilter in opengl is GL_NEAREST_MIPMAP_LINEAR, so lets just assume NEAREST if not
            // provided
            minFilter = FilterMode.NEAREST;
        }

        if (minFilter != null) {
            int glMinFilter;
            if (mipmapMode == null) {
                glMinFilter = GlEnum.from(minFilter);
            } else if (minFilter == FilterMode.NEAREST && mipmapMode == MipmapMode.NEAREST) {
                glMinFilter = GL45C.GL_NEAREST_MIPMAP_NEAREST;
            } else if (minFilter == FilterMode.LINEAR && mipmapMode == MipmapMode.LINEAR) {
                glMinFilter = GL45C.GL_LINEAR_MIPMAP_LINEAR;
            } else if (minFilter == FilterMode.LINEAR && mipmapMode == MipmapMode.NEAREST) {
                glMinFilter = GL45C.GL_LINEAR_MIPMAP_NEAREST;
            } else if (minFilter == FilterMode.NEAREST && mipmapMode == MipmapMode.LINEAR) {
                glMinFilter = GL45C.GL_NEAREST_MIPMAP_LINEAR;
            } else {
                throw new IllegalArgumentException("Unexpected value combination for minFilter and mipmapMode: " + minFilter + ", " + mipmapMode);
            }

            GL45C.glSamplerParameteri(handle, GL45C.GL_TEXTURE_MIN_FILTER, glMinFilter);
        }

        if (magFilter != null) {
            GL45C.glSamplerParameteri(handle, GL45C.GL_TEXTURE_MAG_FILTER, GlEnum.from(magFilter));
        }

        if (addressModeU != null) {
            GL45C.glSamplerParameteri(handle, GL45C.GL_TEXTURE_WRAP_S, GlEnum.from(addressModeU));
        }

        if (addressModeV != null) {
            GL45C.glSamplerParameteri(handle, GL45C.GL_TEXTURE_WRAP_T, GlEnum.from(addressModeV));
        }

        if (addressModeW != null) {
            GL45C.glSamplerParameteri(handle, GL45C.GL_TEXTURE_WRAP_R, GlEnum.from(addressModeW));
        }
    }

    public static int getHandle(Sampler sampler) {
        return ((GlSampler) sampler).getHandle();
    }
}
