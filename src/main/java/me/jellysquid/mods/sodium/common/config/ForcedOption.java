package me.jellysquid.mods.sodium.common.config;

import me.jellysquid.mods.sodium.client.SodiumClientMod;

/**
 * An {@link Option} that's always enabled, ignoring user configuration or any mod overrides.
 */
public class ForcedOption extends Option {
    public ForcedOption(String name) {
        super(name, true, false);
    }

    @Override
    public void setEnabled(boolean enabled, boolean userDefined) {
        if (userDefined)
            SodiumClientMod.logger().warn("User tried to {} forced configuration option \"{}\", ignoring",
                    enabled ? "enable" : "disable", getName());
    }

    @Override
    public void addModOverride(boolean enabled, String modId) {
        SodiumClientMod.logger().warn("Mod {} tried to {} forced configuration option \"{}\", ignoring",
                modId, enabled ? "enable" : "disable", getName());
    }
}
