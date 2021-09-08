package me.jellysquid.mods.thingl.functions;

import me.jellysquid.mods.thingl.buffer.GlBufferStorageFlags;
import me.jellysquid.mods.thingl.buffer.GlBufferTarget;
import me.jellysquid.mods.thingl.device.RenderDevice;
import me.jellysquid.mods.thingl.device.RenderDeviceImpl;
import me.jellysquid.mods.thingl.util.EnumBitField;
import org.lwjgl.opengl.ARBBufferStorage;
import org.lwjgl.opengl.GL44C;
import org.lwjgl.opengl.GLCapabilities;

public enum BufferStorageFunctions {
    NONE {
        @Override
        public void createBufferStorage(GlBufferTarget target, long length, EnumBitField<GlBufferStorageFlags> flags) {
            throw new UnsupportedOperationException();
        }
    },
    CORE {
        @Override
        public void createBufferStorage(GlBufferTarget target, long length, EnumBitField<GlBufferStorageFlags> flags) {
            GL44C.glBufferStorage(target.getTargetParameter(), length, flags.getBitField());
        }
    },
    ARB {
        @Override
        public void createBufferStorage(GlBufferTarget target, long length, EnumBitField<GlBufferStorageFlags> flags) {
            ARBBufferStorage.glBufferStorage(target.getTargetParameter(), length, flags.getBitField());
        }
    };

    public static BufferStorageFunctions pickBest(RenderDeviceImpl device) {
        GLCapabilities capabilities = device.getCapabilities();

        if (capabilities.OpenGL44) {
            return CORE;
        } else if (capabilities.GL_ARB_buffer_storage) {
            return ARB;
        } else {
            return NONE;
        }
    }


    public abstract void createBufferStorage(GlBufferTarget target, long length, EnumBitField<GlBufferStorageFlags> flags);
}
