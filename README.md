# Sodium
![GitHub license](https://img.shields.io/github/license/jellysquid3/Sodium.svg)
![GitHub issues](https://img.shields.io/github/issues/jellysquid3/Sodium.svg)
![GitHub tag](https://img.shields.io/github/tag/jellysquid3/Sodium.svg)

Sodium is a free and open-source optimization mod which attempts to improve frame rates,
reduce micro-stutter, and fix graphical issues in Minecraft.

:warning: This mod is currently in a very early stage of development. There are numerous
serious, game-breaking issues. The codebase is also not in the best state currently. You
should not use this in your game unless you are absolutely willing to deal with serious
issues. 

### Support development

You can help buy me food and support development while getting early access builds of my mods by [making a monthly pledge to my Patreon!](https://patreon.com/jellysquid) You'll also gain some special perks, such as prioritized support on [my Discord server](https://jellysquid.me/discord).

<a href="https://www.patreon.com/bePatron?u=824442"><img src="https://github.com/jellysquid3/Phosphor/raw/master/doc/patreon.png" width="200"></a>

### Join the Discord

You can join the official Discord for my mods by [clicking here](https://jellysquid.me/discord).

<a href="https://jellysquid.me/discord"><img src="https://i.vgy.me/YrTrsE.png"></a>

### Compiling the mod

#### Prerequisites

You will need the JDK 8 (or newer, see section below) installed in order to build Sodium. You can use [Chocolatey](https://chocolatey.org) or [SDKMAN](https://sdkman.io/) to manage Java installations. However, you can also always grab the installers or binary packages directly from [AdoptOpenJDK's website](https://adoptopenjdk.net/) if you do not wish to install a package manager.

The Oracle JDK/JRE builds should be avoided where possible due to their poor quality on Windows. JDK builds which use the OpenJ9 VM might cause issues with building the mod and will generally have much worse in-game performance.

#### A note on newer Java versions

For the best possible performance with Sodium installed, you should prefer to use a Java 14 runtime for your game client with the [Z Garbage Collector (ZGC)](https://wiki.openjdk.java.net/display/zgc/Main) enabled. However, please be sure you read this entire section before upgrading.

If you build the mod with JDK 11 or newer, you *must* upgrade your game client's runtime to at least the version you are building with. If you try to use an older runtime while building with a newer version, [your game may crash or not render anything at all](https://github.com/jellysquid3/sodium-fabric/issues/16). Additionally, if you are using the Java 11 runtime or newer for your client, you should install [Voyager mod for Fabric](https://github.com/modmuss50/Voyager) to patch a [known world generation bug](https://bugs.mojang.com/browse/MC-149777) which can cause rare crashes.

The official Minecraft launcher (and most other third-party launchers) will often use Java 8 runtime in order to work around [bugs in the Intel HD 2xxx/3xxx graphics drivers on Windows 10](https://github.com/LWJGL/lwjgl/issues/119). If you are not affected by these issues, you can usually upgrade the runtime by modifying your game profile's settings in your launcher of choice.

#### Compiling

Navigate to the directory you've cloned this repository and launch a build with Gradle using `gradlew build` on Windows or `./gradlew build` on macOS/Linux. The resulting build artifacts will be present in `build/libs`.

### License

Sodium is licensed under GNU LGPLv3, a free and open-source license. For more information, please see the [license file](https://github.com/jellysquid3/sodium-fabric/blob/master/LICENSE.txt).
