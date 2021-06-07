package me.jellysquid.mods.sodium.api;

public interface SodiumApi {
    static SodiumApi get() {
        return Impl.instance;
    }

    final class Impl {
        static SodiumApi instance;
        private Impl() {}
    }
}
