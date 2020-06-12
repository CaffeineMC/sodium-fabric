package me.jellysquid.mods.sodium.client.gl.func;

public enum GlShaderDrawParameters {
    UNSUPPORTED {
        @Override
        public String getDrawIdVariable() {
            throw new UnsupportedOperationException();
        }
    },
    CORE {
        @Override
        public String getDrawIdVariable() {
            return "gl_DrawID";
        }
    },
    ARB {
        @Override
        public String getDrawIdVariable() {
            return "gl_DrawIDARB";
        }
    };

    public abstract String getDrawIdVariable();
}
