package me.jellysquid.mods.sodium.client.gl.shader;

import java.util.*;

public class ShaderConstants {
    public static final ShaderConstants EMPTY = new ShaderConstants(Collections.emptyList());

    private final List<String> defines;

    private ShaderConstants(List<String> defines) {
        this.defines = defines;
    }

    public List<String> getDefineStrings() {
        return this.defines;
    }

    public static ShaderConstants fromStringList(List<String> defines) {
        ShaderConstants.Builder builder = new ShaderConstants.Builder();

        for (String define : defines) {
            builder.define(define);
        }

        return builder.build();
    }

    public static ShaderConstants.Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private static final String EMPTY_VALUE = "";

        private final HashMap<String, String> constants = new HashMap<>();

        private Builder() {

        }

        public void define(String name) {
            this.define(name, EMPTY_VALUE);
        }

        public void define(String name, String value) {
            String prev = this.constants.get(name);

            if (prev != null) {
                throw new IllegalArgumentException("Constant " + name + " is already defined with value " + prev);
            }

            this.constants.put(name, value);
        }

        public ShaderConstants build() {
            List<String> defines = new ArrayList<>(this.constants.size());

            for (Map.Entry<String, String> entry : this.constants.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                if (value.length() <= 0) {
                    defines.add("#define " + key);
                } else {
                    defines.add("#define " + key + " " + value);
                }
            }

            return new ShaderConstants(Collections.unmodifiableList(defines));
        }
    }
}
