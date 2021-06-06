package me.jellysquid.mods.sodium.client.gui;

import com.google.common.collect.ImmutableList;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.options.*;
import me.jellysquid.mods.sodium.client.gui.options.binding.compat.VanillaBooleanOptionBinding;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlValueFormatter;
import me.jellysquid.mods.sodium.client.gui.options.control.CyclingControl;
import me.jellysquid.mods.sodium.client.gui.options.control.SliderControl;
import me.jellysquid.mods.sodium.client.gui.options.control.TickBoxControl;
import me.jellysquid.mods.sodium.client.gui.options.storage.MinecraftOptionsStorage;
import me.jellysquid.mods.sodium.client.gui.options.storage.SodiumOptionsStorage;
import me.jellysquid.mods.sodium.client.util.UnsafeUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.options.AttackIndicator;
import net.minecraft.client.options.GraphicsMode;
import net.minecraft.client.options.Option;
import net.minecraft.client.options.ParticlesMode;
import net.minecraft.client.util.Window;

import java.util.ArrayList;
import java.util.List;

public class SodiumGameOptionPages {
    private static final SodiumOptionsStorage sodiumOpts = new SodiumOptionsStorage();
    private static final MinecraftOptionsStorage vanillaOpts = new MinecraftOptionsStorage();

    public static OptionPage general() {
        List<OptionGroup> groups = new ArrayList<>();

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName("View Distance")
                        .setTooltip("The view distance controls how far away terrain will be rendered. Lower distances mean that less terrain will be " +
                                "rendered, improving frame rates.")
                        .setControl(option -> new SliderControl(option, 2, 32, 1, ControlValueFormatter.quantity("Chunks")))
                        .setBinding((options, value) -> options.viewDistance = value, options -> options.viewDistance)
                        .setImpact(OptionImpact.HIGH)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName("Brightness")
                        .setTooltip("Controls the brightness (gamma) of the game.")
                        .setControl(opt -> new SliderControl(opt, 0, 100, 1, ControlValueFormatter.brightness()))
                        .setBinding((opts, value) -> opts.gamma = value * 0.01D, (opts) -> (int) (opts.gamma / 0.01D))
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName("Clouds")
                        .setTooltip("Controls whether or not clouds will be visible.")
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> {
                            opts.quality.enableClouds = value;

                            if (MinecraftClient.isFabulousGraphicsOrBetter()) {
                                Framebuffer framebuffer = MinecraftClient.getInstance().worldRenderer.getCloudsFramebuffer();
                                if (framebuffer != null) {
                                    framebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
                                }
                            }
                        }, (opts) -> opts.quality.enableClouds)
                        .setImpact(OptionImpact.LOW)
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName("GUI Scale")
                        .setTooltip("Sets the maximum scale factor to be used for the user interface. If 'auto' is used, then the largest scale factor " +
                                "will always be used.")
                        .setControl(option -> new SliderControl(option, 0, 4, 1, ControlValueFormatter.guiScale()))
                        .setBinding((opts, value) -> {
                            opts.guiScale = value;

                            MinecraftClient client = MinecraftClient.getInstance();
                            client.onResolutionChanged();
                        }, opts -> opts.guiScale)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setName("Fullscreen")
                        .setTooltip("If enabled, the game will display in full-screen (if supported).")
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> {
                            opts.fullscreen = value;

                            MinecraftClient client = MinecraftClient.getInstance();
                            Window window = client.getWindow();

                            if (window != null && window.isFullscreen() != opts.fullscreen) {
                                window.toggleFullscreen();

                                // The client might not be able to enter full-screen mode
                                opts.fullscreen = window.isFullscreen();
                            }
                        }, (opts) -> opts.fullscreen)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setName("V-Sync")
                        .setTooltip("If enabled, the game's frame rate will be synchronized to the monitor's refresh rate, making for a generally smoother experience " +
                                "at the expense of overall input latency. This setting might reduce performance if your system is too slow.")
                        .setControl(TickBoxControl::new)
                        .setBinding(new VanillaBooleanOptionBinding(Option.VSYNC))
                        .setImpact(OptionImpact.VARIES)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName("FPS Limit")
                        .setTooltip("Limits the maximum number of frames per second. In effect, this will throttle the game and can be useful when you want to conserve " +
                                "battery life or multi-task between other applications. If V-Sync is enabled, this option will be ignored unless it is lower than your " +
                                "display's refresh rate.")
                        .setControl(option -> new SliderControl(option, 5, 260, 5, ControlValueFormatter.fpsLimit()))
                        .setBinding((opts, value) -> {
                            opts.maxFps = value;
                            MinecraftClient.getInstance().getWindow().setFramerateLimit(value);
                        }, opts -> opts.maxFps)
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setName("View Bobbing")
                        .setTooltip("If enabled, the player's view will sway and bob when moving around. Players who suffer from motion sickness can benefit from disabling this.")
                        .setControl(TickBoxControl::new)
                        .setBinding(new VanillaBooleanOptionBinding(Option.VIEW_BOBBING))
                        .build())
                .add(OptionImpl.createBuilder(AttackIndicator.class, vanillaOpts)
                        .setName("Attack Indicator")
                        .setTooltip("Controls where the Attack Indicator is displayed on screen.")
                        .setControl(opts -> new CyclingControl<>(opts, AttackIndicator.class, new String[] { "Off", "Crosshair", "Hotbar" }))
                        .setBinding((opts, value) -> opts.attackIndicator = value, (opts) -> opts.attackIndicator)
                        .build())
                .build());

        return new OptionPage("General", ImmutableList.copyOf(groups));
    }

    public static OptionPage quality() {
        List<OptionGroup> groups = new ArrayList<>();

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(GraphicsMode.class, vanillaOpts)
                        .setName("Graphics Quality")
                        .setTooltip("The default graphics quality controls some legacy options and is necessary for mod compatibility. If the options below are left to " +
                                "\"Default\", they will use this setting.")
                        .setControl(option -> new CyclingControl<>(option, GraphicsMode.class, new String[] { "Fast", "Fancy", "Fabulous" }))
                        .setBinding(
                                (opts, value) -> opts.graphicsMode = value,
                                opts -> opts.graphicsMode)
                        .setImpact(OptionImpact.HIGH)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(SodiumGameOptions.GraphicsQuality.class, sodiumOpts)
                        .setName("Clouds Quality")
                        .setTooltip("Controls the quality of rendered clouds in the sky.")
                        .setControl(option -> new CyclingControl<>(option, SodiumGameOptions.GraphicsQuality.class))
                        .setBinding((opts, value) -> opts.quality.cloudQuality = value, opts -> opts.quality.cloudQuality)
                        .setImpact(OptionImpact.LOW)
                        .build())
                .add(OptionImpl.createBuilder(SodiumGameOptions.GraphicsQuality.class, sodiumOpts)
                        .setName("Weather Quality")
                        .setTooltip("Controls the quality of rain and snow effects.")
                        .setControl(option -> new CyclingControl<>(option, SodiumGameOptions.GraphicsQuality.class))
                        .setBinding((opts, value) -> opts.quality.weatherQuality = value, opts -> opts.quality.weatherQuality)
                        .setImpact(OptionImpact.MEDIUM)
                        .build())
                .add(OptionImpl.createBuilder(ParticlesMode.class, vanillaOpts)
                        .setName("Particle Quality")
                        .setTooltip("Controls the maximum number of particles which can be present on screen at any one time.")
                        .setControl(opt -> new CyclingControl<>(opt, ParticlesMode.class, new String[] { "High", "Medium", "Low" }))
                        .setBinding((opts, value) -> opts.particles = value, (opts) -> opts.particles)
                        .setImpact(OptionImpact.MEDIUM)
                        .build())
                .add(OptionImpl.createBuilder(SodiumGameOptions.LightingQuality.class, sodiumOpts)
                        .setName("Smooth Lighting")
                        .setTooltip("Controls the quality of smooth lighting effects.\n" +
                                "\nOff - No smooth lighting" +
                                "\nLow - Smooth block lighting only" +
                                "\nHigh (new!) - Smooth block and entity lighting")
                        .setControl(option -> new CyclingControl<>(option, SodiumGameOptions.LightingQuality.class))
                        .setBinding((opts, value) -> opts.quality.smoothLighting = value, opts -> opts.quality.smoothLighting)
                        .setImpact(OptionImpact.MEDIUM)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName("Biome Blend")
                        .setTooltip("Controls the range which biomes will be sampled for block colorization. " +
                                "Higher values greatly increase the amount of time it takes to build chunks for diminishing improvements in quality.")
                        .setControl(option -> new SliderControl(option, 0, 7, 1, ControlValueFormatter.quantityOrDisabled("block(s)", "None")))
                        .setBinding((opts, value) -> opts.biomeBlendRadius = value, opts -> opts.biomeBlendRadius)
                        .setImpact(OptionImpact.LOW)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName("Entity Distance")
                        .setTooltip("Controls how far away entities can render from the player. Higher values increase the render distance at the expense " +
                                "of frame rates.")
                        .setControl(option -> new SliderControl(option, 50, 500, 25, ControlValueFormatter.percentage()))
                        .setBinding((opts, value) -> opts.entityDistanceScaling = value / 100.0F, opts -> Math.round(opts.entityDistanceScaling * 100.0F))
                        .setImpact(OptionImpact.MEDIUM)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setName("Entity Shadows")
                        .setTooltip("If enabled, basic shadows will be rendered beneath mobs and other entities.")
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.entityShadows = value, opts -> opts.entityShadows)
                        .setImpact(OptionImpact.LOW)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName("Vignette")
                        .setTooltip("If enabled, a vignette effect will be rendered on the player's view. This is very unlikely to make a difference " +
                                "to frame rates unless you are fill-rate limited.")
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.quality.enableVignette = value, opts -> opts.quality.enableVignette)
                        .setImpact(OptionImpact.LOW)
                        .build())
                .build());


        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName("Mipmap Levels")
                        .setTooltip("Controls the number of mipmaps which will be used for block model textures. Higher values provide better rendering of blocks " +
                                "in the distance, but may adversely affect performance with many animated textures.")
                        .setControl(option -> new SliderControl(option, 0, 4, 1, ControlValueFormatter.multiplier()))
                        .setBinding((opts, value) -> opts.mipmapLevels = value, opts -> opts.mipmapLevels)
                        .setImpact(OptionImpact.MEDIUM)
                        .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                        .build())
                .build());


        return new OptionPage("Quality", ImmutableList.copyOf(groups));
    }

    public static OptionPage advanced() {
        List<OptionGroup> groups = new ArrayList<>();

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName("Use Chunk Multi-Draw")
                        .setTooltip("Allows for many chunks to be rendered together in larger draw call batches.")
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.advanced.useMultidraw = value, opts -> opts.advanced.useMultidraw)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName("Use Vertex Arrays")
                        .setTooltip("Minimizes CPU overhead when switching between different kinds of render tasks.")
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.advanced.useVertexArrays = value, opts -> opts.advanced.useVertexArrays)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName("Use Chunk Face Culling")
                        .setTooltip("If enabled, an additional culling pass will be performed on the CPU to determine which planes of a chunk mesh are visible. This " +
                                "can eliminate a large number of block faces very early in the rendering process, saving memory bandwidth and time on the GPU.")
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.MEDIUM)
                        .setBinding((opts, value) -> opts.advanced.useChunkFaceCulling = value, opts -> opts.advanced.useChunkFaceCulling)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName("Use Compact Vertex Format")
                        .setTooltip("If enabled, a more compact vertex format will be used for chunk meshes which limits the precision of vertex attributes. This format " +
                                "will reduce graphics memory usage and bandwidth requirements by around 40%, but could cause z-fighting/flickering texture issues in " +
                                "some edge cases.")
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.MEDIUM)
                        .setBinding((opts, value) -> opts.advanced.useCompactVertexFormat = value, opts -> opts.advanced.useCompactVertexFormat)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName("Use Fog Occlusion")
                        .setTooltip("If enabled, chunks which are determined to be fully hidden by fog effects will be skipped during rendering. This " +
                                "will generally provide a modest improvement to the number of chunks rendered each frame, especially " +
                                "where fog effects are heavier (i.e. while underwater.)")
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.advanced.useFogOcclusion = value, opts -> opts.advanced.useFogOcclusion)
                        .setImpact(OptionImpact.MEDIUM)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName("Use Entity Culling")
                        .setTooltip("If enabled, a secondary culling pass will be performed before attempting to render an entity. This additional pass " +
                                "takes into account the current set of visible chunks and removes entities which are not in any visible chunks.")
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.MEDIUM)
                        .setBinding((opts, value) -> opts.advanced.useAdvancedEntityCulling = value, opts -> opts.advanced.useAdvancedEntityCulling)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName("Use Particle Culling")
                        .setTooltip("If enabled, only particles which are determined to be visible will be rendered. This can provide a significant improvement " +
                                "to frame rates when many particles are nearby.")
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.MEDIUM)
                        .setBinding((opts, value) -> opts.advanced.useParticleCulling = value, opts -> opts.advanced.useParticleCulling)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName("Animate Only Visible Textures")
                        .setTooltip("If enabled, only animated textures determined to be visible will be updated. This can provide a significant boost to frame " +
                                "rates on some hardware. If you experience issues with some textures not being animated, disable this option.")
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.MEDIUM)
                        .setBinding((opts, value) -> opts.advanced.animateOnlyVisibleTextures = value, opts -> opts.advanced.animateOnlyVisibleTextures)
                        .build()
                )
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName("Use Memory Intrinsics")
                        .setTooltip("If enabled, special intrinsics will be used to speed up the copying of client memory in certain vertex-limited scenarios, such " +
                                "as particle and text rendering. This option only exists for debugging purposes and should be left enabled unless you know what " +
                                "you are doing.")
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.MEDIUM)
                        .setEnabled(UnsafeUtil.isSupported())
                        .setBinding((opts, value) -> opts.advanced.useMemoryIntrinsics = value, opts -> opts.advanced.useMemoryIntrinsics)
                        .build()
                )
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName("Disable Driver Blacklist")
                        .setTooltip("If selected, Sodium will ignore the built-in driver blacklist and enable options which are known to be broken " +
                                "with your system configuration. This might cause serious problems and should not be used unless you really do know better. The settings " +
                                "screen must be saved, closed, and re-opened after changing this option in order to reveal previously hidden options.")
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.advanced.disableDriverBlacklist = value, opts -> opts.advanced.disableDriverBlacklist)
                        .build()
                )
                .build());

        return new OptionPage("Advanced", ImmutableList.copyOf(groups));
    }
}
