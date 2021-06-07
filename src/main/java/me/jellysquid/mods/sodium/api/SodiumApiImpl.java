package me.jellysquid.mods.sodium.api;

public class SodiumApiImpl implements SodiumApi {
    public static void install() {
        SodiumApi.Impl.instance = new SodiumApiImpl();
    }
}
