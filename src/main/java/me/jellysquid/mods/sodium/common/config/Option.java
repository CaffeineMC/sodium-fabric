package me.jellysquid.mods.sodium.common.config;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class Option {
    private boolean enabled;
    private boolean userDefined;
    private Set<String> modDefined = new LinkedHashSet<>(0);

    public Option(boolean enabled, boolean userDefined) {
        this.enabled = enabled;
        this.userDefined = userDefined;
    }

    public void setEnabled(boolean enabled, boolean userDefined) {
        this.enabled = enabled;
        this.userDefined = userDefined;
    }

    public void addModOverride(boolean enabled, String modId) {
        this.enabled = enabled;
        this.modDefined.add(modId);
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public boolean isUserDefined() {
        return this.userDefined;
    }

    public void clearModsDefiningValue() {
        this.modDefined.clear();
    }

    public Collection<String> getModsDefiningValue() {
        return this.modDefined;
    }
}
