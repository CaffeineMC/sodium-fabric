package me.jellysquid.mods.sodium.client.gl.shader;

import java.util.*;

public class ShaderConstants {
    private final List<String> defines;

    private ShaderConstants(List<String> defines) {
        this.defines = defines;
    }

    public List<String> getDefineStrings() {
        return this.defines;
    }

    public static ShaderConstants.Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private static final String EMPTY_VALUE = "";

        private final HashMap<String, String> constants = new HashMap<>();
        private final List<String> addIns = new LinkedList<>();

        private Builder() {

        }

        public void add(String name) {
            this.add(name, EMPTY_VALUE);
        }

        public void add(String name, String value) {
            String prev = this.constants.get(name);

            if (prev != null) {
                throw new IllegalArgumentException("Constant " + name + " is already defined with value " + prev);
            }

            this.constants.put(name, value);
        }

        public void addIn(String value) {
            this.addIns.add(value);
        }

        public ShaderConstants build() {
            List<String> defines = new ArrayList<>(this.constants.size());

            for (Map.Entry<String, String> entry : this.constants.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                if (value.isEmpty()) {
                    defines.add("#define " + key);
                } else {
                    defines.add("#define " + key + " " + value);
                }
            }

            defines.addAll(this.addIns);

            return new ShaderConstants(Collections.unmodifiableList(defines));
        }

        public void addAll(List<String> defines) {
            for (String value : defines) {
                this.add(value);
            }
        }
    }
}
