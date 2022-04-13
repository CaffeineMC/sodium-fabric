package net.caffeinemc.gfx.api.shader;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class ShaderDescription {
    public final Map<ShaderType, String> shaderSources;

    private ShaderDescription(Map<ShaderType, String> shaderSources) {
        this.shaderSources = shaderSources;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<ShaderType, String> shaderSources = new EnumMap<>(ShaderType.class);

        public Builder addShaderSource(ShaderType type, String source) {
            this.shaderSources.put(type, source);
            return this;
        }

        public ShaderDescription build() {
            if (this.shaderSources.isEmpty())  {
                throw new IllegalStateException("No shader sources specified");
            }

            return new ShaderDescription(
                    Collections.unmodifiableMap(this.shaderSources));
        }
    }
}
