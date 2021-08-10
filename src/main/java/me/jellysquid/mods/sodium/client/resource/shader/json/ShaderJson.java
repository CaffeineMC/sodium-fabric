package me.jellysquid.mods.sodium.client.resource.shader.json;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class ShaderJson {
    @SerializedName("source")
    private String source;

    @SerializedName("constants")
    private Map<String, String> constants;

    public String getSource() {
        return this.source;
    }

    public Map<String, String> getConstants() {
        return this.constants;
    }
}
