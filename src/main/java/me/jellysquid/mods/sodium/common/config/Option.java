package me.jellysquid.mods.sodium.common.config;

public class Option {
    private boolean enabled;
    private boolean userDefined;

    public Option(boolean enabled, boolean userDefined) {
        this.enabled = enabled;
        this.userDefined = userDefined;
    }

    public void setEnabled(boolean enabled, boolean userDefined) {
        this.enabled = enabled;
        this.userDefined = userDefined;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public boolean isUserDefined() {
        return this.userDefined;
    }
}
