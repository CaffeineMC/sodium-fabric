package me.jellysquid.mods.sodium.client.util.workarounds;

import java.util.Map;

public class DriverProbeResult {
    public final String vendor, renderer, version;

    public DriverProbeResult(Map<String, String> fields) {
        this.vendor = fields.get("vendor");
        this.renderer = fields.get("renderer");
        this.version = fields.get("version");
    }
}
