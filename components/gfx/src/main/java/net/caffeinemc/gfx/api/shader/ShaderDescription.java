package net.caffeinemc.gfx.api.shader;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class ShaderDescription {
    public final Map<ShaderType, String> shaderSources;
    public final Object2IntMap<String> attributeBindings;
    public final Object2IntMap<String> fragmentBindings;

    private ShaderDescription(Map<ShaderType, String> shaderSources, Object2IntMap<String> attributeBindings, Object2IntMap<String> fragmentBindings) {
        this.shaderSources = shaderSources;
        this.attributeBindings = attributeBindings;
        this.fragmentBindings = fragmentBindings;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<ShaderType, String> shaderSources = new EnumMap<>(ShaderType.class);

        private final Object2IntMap<String> attributeBindings = new Object2IntOpenHashMap<>();
        private final Object2IntMap<String> fragmentBindings = new Object2IntOpenHashMap<>();

        public Builder addShaderSource(ShaderType type, String source) {
            this.shaderSources.put(type, source);
            return this;
        }

        public Builder addAttributeBinding(String name, int binding) {
            this.attributeBindings.put(name, binding);
            return this;
        }

        public Builder addFragmentBinding(String name, int binding) {
            this.fragmentBindings.put(name, binding);
            return this;
        }

        public ShaderDescription build() {
            if (this.shaderSources.isEmpty())  {
                throw new IllegalStateException("No shader sources specified");
            }

            return new ShaderDescription(
                    Collections.unmodifiableMap(this.shaderSources),
                    Object2IntMaps.unmodifiable(this.attributeBindings),
                    Object2IntMaps.unmodifiable(this.fragmentBindings));
        }
    }
}
