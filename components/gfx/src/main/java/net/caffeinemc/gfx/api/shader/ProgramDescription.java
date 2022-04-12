package net.caffeinemc.gfx.api.shader;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class ProgramDescription {
    public final Map<ShaderType, ShaderDescription> shaders;

    private ProgramDescription(Map<ShaderType, ShaderDescription> shaders) {
        this.shaders = shaders;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<ShaderType, ShaderDescription> shaders = new EnumMap<>(ShaderType.class);

        public Builder addShaderBinary(ShaderType type, ShaderDescription description) {
            this.shaders.put(type, description);
            return this;
        }

        public ProgramDescription build() {
            if (this.shaders.isEmpty())  {
                throw new IllegalStateException("No shader sources specified");
            }

            return new ProgramDescription(
                    Collections.unmodifiableMap(this.shaders));
        }
    }
}
