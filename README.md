![Project icon](https://git-assets.jellysquid.me/hotlink-ok/sodium/icon-rounded-128px.png)

# Sodium (for Fabric)
![GitHub license](https://img.shields.io/github/license/jellysquid3/sodium-fabric.svg)
![GitHub issues](https://img.shields.io/github/issues/jellysquid3/sodium-fabric.svg)
![GitHub tag](https://img.shields.io/github/tag/jellysquid3/sodium-fabric.svg)

Sodium is a free and open-source optimization mod for the Minecraft client that improves frame rates, reduces
micro-stutter, and fixes graphical issues in Minecraft. 

:warning: Sodium has had a lot of time to shape up lately, but the mod is still alpha software. You may run into small
graphical issues or crashes while using it. Additionally, the
[Fabric Rendering API](https://fabricmc.net/wiki/documentation:rendering) is not yet supported, which may cause crashes
and other issues with a number of mods.

## Installation

### Stable releases

#### Manual Installation (recommended)

The latest releases of Sodium are published to our [official Modrinth page](https://modrinth.com/mod/sodium) and [GitHub releases page](https://github.com/jellysquid3/sodium-fabric/releases). Usually, builds will be
made available on GitHub slightly sooner than other locations.

You will need Fabric Loader 0.10.x or newer installed in your game in order to load Sodium. If you haven't installed Fabric
mods before, you can find a variety of community guides for doing so [here](https://fabricmc.net/wiki/install).

#### CurseForge

If you are using the new CurseForge client, you can continue to find downloads through our
[official CurseForge page](https://www.curseforge.com/minecraft/mc-mods/sodium). Please note
that the CurseForge launcher does not natively support Fabric modding, so you will also need to install
[Jumploader](https://www.curseforge.com/minecraft/mc-mods/jumploader) in order to create a Fabric environment. As such,
we generally do not recommend this option, and are looking to phase out support for it in the near future. 


### Bleeding-edge builds

If you are a player who is looking to get your hands on the latest **bleeding-edge builds for testing**, consider
taking a look at the builds produced through our [GitHub Actions workflow](actions/workflows/gradle.yml). This
workflow automatically runs every time a change is pushed to the repository, and as such, they will reflect the latest
state of development.

Bleeding edge builds will often include unfinished code that hasn't been extensively tested. That code may introduce
incomplete features, bugs, crashes, and all other kinds of weird issues. You **should not use these bleeding edge builds**
unless you know what you are doing and are comfortable with software debugging. If you report issues using these builds,
we will expect that this is the case. Caveat emptor.

### Reporting Issues

You can report bugs and crashes by opening an issue on our [issue tracker](https://github.com/jellysquid3/sodium-fabric/issues).
Before opening a new issue, please check using the search tool that your issue has not already been created, and that if
there is a suitable template for the issue you are opening, that it is filled out entirely. Issues which are duplicates
or do not contain the necessary information to triage and debug may be closed. 

Please note that while the issue tracker is open to feature and mod compatibility requests, development
is primarily focused on improving hardware compatibility and performance, along with finishing any unimplemented features
necessary for parity with the vanilla renderer.

### Community
[![Discord chat](https://img.shields.io/badge/chat%20on-discord-7289DA)](https://jellysquid.me/discord)

We have an [official Discord community](https://jellysquid.me/discord) for all of our projects. By joining, you can:
- Get installation help and technical support with all of our mods 
- Be notified of the latest developments as they happen
- Get involved and collaborate with the rest of our team
- ... and just hang out with the rest of our community.

### Building from sources

#### Requirements

- JRE 8 or newer (for running Gradle)
- JDK 8 (optional)
  - If you neither have JDK 8 available on your shell's path or installed through a supported package manager (such as
[SDKMAN](https://sdkman.io)), Gradle will automatically download a suitable toolchain from the [AdoptOpenJDK project](https://adoptopenjdk.net/)
and use it to compile the project. For more information on what package managers are supported and how you can
customize this behavior on a system-wide level, please see [Gradle's Toolchain user guide](https://docs.gradle.org/current/userguide/toolchains.html).
- Gradle 6.7 or newer (optional)
  - The [Gradle wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html#sec:using_wrapper) is provided in
    this repository can be used instead of installing a suitable version of Gradle yourself. However, if you are building
    many projects, you may prefer to install it yourself through a suitable package manager as to save disk space and to
    avoid many different Gradle daemons sitting around in memory.

#### Building with Gradle

Sodium uses a typical Gradle project structure and can be built by simply running the default `build` task.

**Tip:** If this is a one-off build, and you would prefer the Gradle daemon does not stick around in memory afterwards 
(often consuming upwards of 1 GiB), then you can use the [`--no-daemon` argument](https://docs.gradle.org/current/userguide/gradle_daemon.html#sec:disabling_the_daemon)
to ensure that the daemon is torn down after the build is complete. However, subsequent Gradle builds will
[start more slowly](https://docs.gradle.org/current/userguide/gradle_daemon.html#sec:why_the_daemon) if the Gradle
daemon is not sitting warm and loaded in memory.

After Gradle finishes building the project, the resulting build artifacts (your usual mod binaries, and
their sources) can be found in `build/libs`.

Build artifacts classified with `dev` are outputs containing the sources and compiled classes
before they are remapped into stable intermediary names. If you are working in a developer environment and would
like to add the mod to your game, you should prefer to use the `modRuntime` or `modCompile` configurations provided by
Loom instead of these outputs.

Please note that support is not provided for setting up build environments or compiling the mod. We ask that
users who are looking to get their hands dirty with the code have a basic understanding of compiling Java/Gradle
projects.

### License

Sodium is licensed under GNU LGPLv3, a free and open-source license. For more information, please see the
[license file](https://github.com/jellysquid3/sodium-fabric/blob/1.16.x/dev/LICENSE.txt).
