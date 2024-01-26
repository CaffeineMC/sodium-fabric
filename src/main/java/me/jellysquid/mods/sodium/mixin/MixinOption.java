package me.jellysquid.mods.sodium.mixin;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class MixinOption {
    private final String name;

    private Set<String> modDefined = null;
    private boolean enabled;
    private boolean userDefined;
    private boolean overrideable;

    public MixinOption(String name, boolean enabled, boolean userDefined, boolean overrideable) {
        this.name = name;
        this.enabled = enabled;
        this.userDefined = userDefined;
        this.overrideable = overrideable;
    }

    public void setOverrideable(boolean overrideable) {
        this.overrideable = overrideable;
    }

    public void setEnabled(boolean enabled, boolean userDefined) {
        this.enabled = enabled;
        this.userDefined = userDefined;
    }

    public void addModOverride(boolean enabled, String modId) {
        this.enabled = enabled;

        if (this.modDefined == null) {
            this.modDefined = new LinkedHashSet<>();
        }

        this.modDefined.add(modId);
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public boolean isOverridden() {
        return this.isUserDefined() || this.isModDefined();
    }

    public boolean isUserDefined() {
        return this.userDefined;
    }

    public boolean isModDefined() {
        return this.modDefined != null;
    }

    public boolean isOverrideable() {
        return overrideable;
    }

    public String getName() {
        return this.name;
    }

    public void clearModsDefiningValue() {
        this.modDefined = null;
    }

    public Collection<String> getDefiningMods() {
        return this.modDefined != null ? Collections.unmodifiableCollection(this.modDefined) : Collections.emptyList();
    }
}
