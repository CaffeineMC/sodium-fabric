package me.jellysquid.mods.sodium.client.gui.api;

import com.google.common.collect.ImmutableList;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;

import java.util.List;

public interface SodiumGameOptionApi {

    default OptionPage getOptionPage() {
        return null;
    }

    default List<OptionPage> getProvidedOptionPages() {
        return ImmutableList.of();
    }
}