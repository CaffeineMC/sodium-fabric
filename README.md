![Project icon](https://git-assets.jellysquid.me/hotlink-ok/sodium/icon-rounded-128px.png)

# Sodium (for Fabric)
![GitHub license](https://img.shields.io/github/license/jellysquid3/sodium-fabric.svg)
![GitHub issues](https://img.shields.io/github/issues/jellysquid3/sodium-fabric.svg)
![GitHub tag](https://img.shields.io/github/tag/jellysquid3/sodium-fabric.svg)
[![Discord chat](https://img.shields.io/badge/chat%20on-discord-7289DA)](https://jellysquid.me/discord)

Sodium is a free and open-source optimization mod for the Minecraft client that improves frame rates, reduces
micro-stutter, and fixes graphical issues in Minecraft. 

:warning: Sodium has had a lot of time to shape up lately, but the mod is still alpha software. You may run into small
graphical issues or crashes while using it. Additionally, the
[Fabric Rendering API](https://fabricmc.net/wiki/documentation:rendering) is not yet supported, which may cause crashes
or prevent other mods from rendering correctly. Please be aware of these issues before using it in your game.

### Downloads

You can find downloads for Sodium through the
[GitHub releases page](https://github.com/jellysquid3/sodium-fabric/releases). Once Sodium matures and leaves the stage
of alpha software, builds will be published on CurseForge. 

### Community

If you'd like to get help with the mod, check out the latest developments, or be notified when there's a new release,
the Discord community might be for you! You can join the official server for my mods by clicking
[here](https://jellysquid.me).

### Building from source

If you're hacking on the code or would like to compile a custom build of Sodium from the latest sources, you'll want
to start here.

#### Prerequisites

You will need to install JDK 8 (or newer, see below) in order to build Sodium. You can either install this through
a package manager such as [Chocolatey](https://chocolatey.org/) on Windows or [SDKMAN!](https://sdkman.io/) on other
platforms. If you'd prefer to not use a package manager, you can always grab the installers or packages directly from
[AdoptOpenJDK](https://adoptopenjdk.net/).

On Windows, the Oracle JDK/JRE builds should be avoided where possible due to their poor quality. Always prefer using
the open-source builds from AdoptOpenJDK when possible.

#### A note on newer Java versions

For the best possible experience with Sodium installed, you should prefer to use a Java 14 runtime for your game client
with the [Z Garbage Collector (ZGC)](https://wiki.openjdk.java.net/display/zgc/Main) enabled. This is purely optional,
but will generally help to improve frame times by reducing garbage collection pause times. However, please be sure
you read this entire section before upgrading your Java runtime.

If you build the mod with JDK 11 or newer, you *must* upgrade your game client's runtime to at least the version you are
building with. If you try to use an older runtime while building with a newer version,
[your game may crash or not render anything at all](https://github.com/jellysquid3/sodium-fabric/issues/16).
Additionally, if you are using the Java 11 runtime or newer for your client, you should install
[Voyager mod for Fabric](https://github.com/modmuss50/Voyager) to patch a
[known world generation bug](https://bugs.mojang.com/browse/MC-149777) which can cause rare crashes.

The official Minecraft launcher (and most other third-party launchers) will often use Java 8 runtime in order to work
around [bugs in the Intel HD 2xxx/3xxx graphics drivers on Windows 10](https://github.com/LWJGL/lwjgl/issues/119). If
you are not affected by these issues, you can usually upgrade the runtime by modifying your game profile's settings in
your launcher of choice.

#### Compiling

Navigate to the directory you've cloned this repository and launch a build with Gradle using `gradlew build` (Windows)
or `./gradlew build` (macOS/Linux). If you are not using the Gradle wrapper, simply replace `gradlew` with `gradle`
or the path to it.

The initial setup may take a few minutes. After Gradle has finished building everything, you can find the resulting
artifacts in `build/libs`.

### License

Sodium is licensed under GNU LGPLv3, a free and open-source license. For more information, please see the
[license file](https://github.com/jellysquid3/sodium-fabric/blob/1.16.x/dev/LICENSE.txt).
