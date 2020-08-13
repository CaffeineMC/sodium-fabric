package me.jellysquid.mods.sodium.client.gui;

import com.google.common.collect.ImmutableList;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.options.OptionFlag;
import me.jellysquid.mods.sodium.client.gui.options.OptionGroup;
import me.jellysquid.mods.sodium.client.gui.options.OptionImpl;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
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
import net.minecraft.client.resource.language.I18n;
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
                        .setName(I18n.translate("options.renderDistance"))
                        .setTooltip(I18n.translate("sodium.options.view_distance.tooltip"))
                        .setControl(option -> new SliderControl(option, 2, 32, 1, ControlValueFormatter.quantity("Chunks")))
                        .setBinding((options, value) -> options.viewDistance = value, options -> options.viewDistance)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName(I18n.translate("options.gamma"))
                        .setTooltip(I18n.translate("sodium.options.brightness.tooltip"))
                        .setControl(opt -> new SliderControl(opt, 0, 100, 1, ControlValueFormatter.brightness()))
                        .setBinding((opts, value) -> opts.gamma = value * 0.01D, (opts) -> (int) (opts.gamma / 0.01D))
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName(I18n.translate("options.renderClouds"))
                        .setTooltip(I18n.translate("sodium.options.clouds.tooltip"))
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
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName(I18n.translate("sodium.options.fog.name"))
                        .setTooltip(I18n.translate("sodium.options.fog.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.quality.enableFog = value, opts -> opts.quality.enableFog)
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName(I18n.translate("options.guiScale"))
                        .setTooltip(I18n.translate("sodium.options.gui_scale.tooltip"))
                        .setControl(option -> new SliderControl(option, 0, 4, 1, ControlValueFormatter.guiScale()))
                        .setBinding((opts, value) -> {
                            opts.guiScale = value;

                            MinecraftClient client = MinecraftClient.getInstance();
                            client.onResolutionChanged();
                        }, opts -> opts.guiScale)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setName(I18n.translate("options.fullscreen"))
                        .setTooltip(I18n.translate("sodium.options.fullscreen.tooltip"))
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
                        .setName(I18n.translate("options.vsync"))
                        .setTooltip(I18n.translate("sodium.options.v_sync.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding(new VanillaBooleanOptionBinding(Option.VSYNC))
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName(I18n.translate("options.framerateLimit"))
                        .setTooltip(I18n.translate("sodium.options.fps_limit.tooltip"))
                        .setControl(option -> new SliderControl(option, 5, 260, 5, ControlValueFormatter.fpsLimit()))
                        .setBinding((opts, value) -> {
                            opts.maxFps = value;
                            MinecraftClient.getInstance().getWindow().setFramerateLimit(value);
                        }, opts -> opts.maxFps)
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setName(I18n.translate("options.viewBobbing"))
                        .setTooltip(I18n.translate("sodium.options.view_bobbing.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding(new VanillaBooleanOptionBinding(Option.VIEW_BOBBING))
                        .build())
                .add(OptionImpl.createBuilder(AttackIndicator.class, vanillaOpts)
                        .setName(I18n.translate("options.attackIndicator"))
                        .setTooltip(I18n.translate("sodium.options.attack_indicator.tooltip"))
                        .setControl(opts -> new CyclingControl<>(opts, AttackIndicator.class, new String[] { "Off", "Crosshair", "Hotbar" }))
                        .setBinding((opts, value) -> opts.attackIndicator = value, (opts) -> opts.attackIndicator)
                        .build())
                .build());

        return new OptionPage(I18n.translate("stat.generalButton"), ImmutableList.copyOf(groups));
    }

    public static OptionPage quality() {
        List<OptionGroup> groups = new ArrayList<>();

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(GraphicsMode.class, vanillaOpts)
                        .setName(I18n.translate("options.graphics"))
                        .setTooltip(I18n.translate("sodium.options.graphics_quality.tooltip"))
                        .setControl(option -> new CyclingControl<>(option, GraphicsMode.class, new String[] { I18n.translate("options.graphics.fast"), I18n.translate("options.graphics.fancy"), I18n.translate("options.graphics.fabulous") }))
                        .setBinding(
                                (opts, value) -> opts.graphicsMode = value,
                                opts -> opts.graphicsMode)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(SodiumGameOptions.GraphicsQuality.class, sodiumOpts)
                        .setName(I18n.translate("sodium.options.clouds_quality.name"))
                        .setTooltip(I18n.translate("sodium.options.clouds_quality.tooltip"))
                        .setControl(option -> new CyclingControl<>(option, SodiumGameOptions.GraphicsQuality.class))
                        .setBinding((opts, value) -> opts.quality.cloudQuality = value, opts -> opts.quality.cloudQuality)
                        .build())
                .add(OptionImpl.createBuilder(SodiumGameOptions.GraphicsQuality.class, sodiumOpts)
                        .setName(I18n.translate("sodium.options.weather_quality.name"))
                        .setTooltip(I18n.translate("sodium.options.weather_quality.tooltip"))
                        .setControl(option -> new CyclingControl<>(option, SodiumGameOptions.GraphicsQuality.class))
                        .setBinding((opts, value) -> opts.quality.weatherQuality = value, opts -> opts.quality.weatherQuality)
                        .build())
                .add(OptionImpl.createBuilder(ParticlesMode.class, vanillaOpts)
                        .setName(I18n.translate("sodium.options.particle_quality.name"))
                        .setTooltip(I18n.translate("sodium.options.particle_quality.tooltip"))
                        .setControl(opt -> new CyclingControl<>(opt, ParticlesMode.class, new String[] { "High", "Medium", "Low" }))
                        .setBinding((opts, value) -> opts.particles = value, (opts) -> opts.particles)
                        .build())
                .add(OptionImpl.createBuilder(SodiumGameOptions.LightingQuality.class, sodiumOpts)
                        .setName(I18n.translate("options.ao"))
                        .setTooltip(I18n.translate("sodium.options.smooth_lighting.tooltip"))
                        .setControl(option -> new CyclingControl<>(option, SodiumGameOptions.LightingQuality.class))
                        .setBinding((opts, value) -> opts.quality.smoothLighting = value, opts -> opts.quality.smoothLighting)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName(I18n.translate("options.biomeBlendRadius"))
                        .setTooltip(I18n.translate("sodium.options.biome_blend.tooltip"))
                        .setControl(option -> new SliderControl(option, 0, 7, 1, ControlValueFormatter.quantityOrDisabled("block(s)", "None")))
                        .setBinding((opts, value) -> opts.biomeBlendRadius = value, opts -> opts.biomeBlendRadius)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName(I18n.translate("options.entityDistanceScaling"))
                        .setTooltip(I18n.translate("sodium.options.entity_distance.tooltip"))
                        .setControl(option -> new SliderControl(option, 50, 500, 25, ControlValueFormatter.percentage()))
                        .setBinding((opts, value) -> opts.entityDistanceScaling = value / 100.0F, opts -> Math.round(opts.entityDistanceScaling * 100.0F))
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setName(I18n.translate("options.entityShadows"))
                        .setTooltip(I18n.translate("sodium.options.entity_shadows.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.entityShadows = value, opts -> opts.entityShadows)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName(I18n.translate("sodium.options.vignette.name"))
                        .setTooltip(I18n.translate("sodium.options.vignette.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.quality.enableVignette = value, opts -> opts.quality.enableVignette)
                        .build())
                .build());


        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName(I18n.translate("options.mipmapLevels"))
                        .setTooltip(I18n.translate("sodium.options.mipmap_levels.tooltip"))
                        .setControl(option -> new SliderControl(option, 0, 4, 1, ControlValueFormatter.multiplier()))
                        .setBinding((opts, value) -> opts.mipmapLevels = value, opts -> opts.mipmapLevels)
                        .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                        .build())
                .build());


        return new OptionPage(I18n.translate("sodium.options.pages.quality"), ImmutableList.copyOf(groups));
    }

    public static OptionPage advanced() {
        boolean disableBlacklist = SodiumClientMod.options().advanced.disableDriverBlacklist;

        List<OptionGroup> groups = new ArrayList<>();

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(SodiumGameOptions.ChunkRendererBackendOption.class, sodiumOpts)
                        .setName(I18n.translate("sodium.options.chunk_renderer.name"))
                        .setTooltip(I18n.translate("sodium.options.chunk_renderer.tooltip"))
                        .setControl((opt) -> new CyclingControl<>(opt, SodiumGameOptions.ChunkRendererBackendOption.class,
                                SodiumGameOptions.ChunkRendererBackendOption.getAvailableOptions(disableBlacklist)))
                        .setBinding((opts, value) -> opts.advanced.chunkRendererBackend = value, opts -> opts.advanced.chunkRendererBackend)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName(I18n.translate("sodium.options.use_chunk_face_culling.name"))
                        .setTooltip(I18n.translate("sodium.options.use_chunk_face_culling.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.advanced.useChunkFaceCulling = value, opts -> opts.advanced.useChunkFaceCulling)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName(I18n.translate("sodium.options.use_compact_vertex_format.name"))
                        .setTooltip(I18n.translate("sodium.options.use_compact_vertex_format.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.advanced.useCompactVertexFormat = value, opts -> opts.advanced.useCompactVertexFormat)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName(I18n.translate("sodium.options.use_fog_occlusion.name"))
                        .setTooltip(I18n.translate("sodium.options.use_fog_occlusion.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.advanced.useFogOcclusion = value, opts -> opts.advanced.useFogOcclusion)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName(I18n.translate("sodium.options.use_entity_culling.name"))
                        .setTooltip(I18n.translate("sodium.options.use_entity_culling.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.advanced.useAdvancedEntityCulling = value, opts -> opts.advanced.useAdvancedEntityCulling)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName(I18n.translate("sodium.options.use_particle_culling.name"))
                        .setTooltip(I18n.translate("sodium.options.use_particle_culling.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.advanced.useParticleCulling = value, opts -> opts.advanced.useParticleCulling)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName(I18n.translate("sodium.options.animate_only_visible_textures.name"))
                        .setTooltip(I18n.translate("sodium.options.animate_only_visible_textures.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.advanced.animateOnlyVisibleTextures = value, opts -> opts.advanced.animateOnlyVisibleTextures)
                        .build()
                )
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName(I18n.translate("sodium.options.use_memory_intrinsics.name"))
                        .setTooltip(I18n.translate("sodium.options.use_memory_intrinsics.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setEnabled(UnsafeUtil.isSupported())
                        .setBinding((opts, value) -> opts.advanced.useMemoryIntrinsics = value, opts -> opts.advanced.useMemoryIntrinsics)
                        .build()
                )
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName(I18n.translate("sodium.options.disable_driver_blacklist.name"))
                        .setTooltip(I18n.translate("sodium.options.disable_driver_blacklist.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.advanced.disableDriverBlacklist = value, opts -> opts.advanced.disableDriverBlacklist)
                        .build()

                )
                .build());
        return new OptionPage(I18n.translate("sodium.options.pages.advanced"), ImmutableList.copyOf(groups));
    }
}
