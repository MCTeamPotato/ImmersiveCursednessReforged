package nl.theepicblock.immersive_cursedness.mixin.rubidium;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import me.jellysquid.mods.sodium.client.gui.SodiumOptionsGUI;
import me.jellysquid.mods.sodium.client.gui.options.*;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlValueFormatter;
import me.jellysquid.mods.sodium.client.gui.options.control.SliderControl;
import me.jellysquid.mods.sodium.client.gui.options.control.TickBoxControl;
import me.jellysquid.mods.sodium.client.gui.options.storage.SodiumOptionsStorage;
import net.minecraft.text.Text;
import nl.theepicblock.immersive_cursedness.ImmersiveCursedness;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Pseudo
@Mixin(value = SodiumOptionsGUI.class, remap = false)
public abstract class SodiumSettingsMixin {

    @Shadow @Final private List<OptionPage> pages;
    @Unique
    private static final SodiumOptionsStorage ic$sodiumOpts = new SodiumOptionsStorage();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        List<OptionGroup> groups = new ObjectArrayList<>();
        OptionImpl<SodiumGameOptions, Boolean> debugParticles = OptionImpl.createBuilder(Boolean.class, ic$sodiumOpts)
                .setName(Text.translatable("immersive.debugParticles"))
                .setTooltip(Text.translatable("immersive.debugParticles.tooltip"))
                .setControl(TickBoxControl::new)
                .setBinding(
                        (opts, value) -> ImmersiveCursedness.Config.debugParticles.set(value),
                        (opts) -> ImmersiveCursedness.Config.debugParticles.get()
                )
                .setImpact(OptionImpact.LOW)
                .build();

        groups.add(OptionGroup.createBuilder().add(debugParticles).build());

        OptionImpl<SodiumGameOptions, Boolean> defaultEnabled = OptionImpl.createBuilder(Boolean.class, ic$sodiumOpts)
                .setName(Text.translatable("immersive.defaultEnabled"))
                .setTooltip(Text.translatable("immersive.defaultEnabled.tooltip"))
                .setControl(TickBoxControl::new)
                .setBinding(
                        (opts, value) -> ImmersiveCursedness.Config.defaultEnabled.set(value),
                        (opts) -> ImmersiveCursedness.Config.defaultEnabled.get()
                )
                .setImpact(OptionImpact.LOW)
                .build();

        groups.add(OptionGroup.createBuilder().add(defaultEnabled).build());

        Option<Integer> horizontalSendLimit = OptionImpl.createBuilder(Integer.TYPE, ic$sodiumOpts)
                .setName(Text.translatable("immersive.horizontalSendLimit"))
                .setTooltip(Text.translatable("immersive.horizontalSendLimit.tooltip"))
                .setControl(sodiumGameOptionsIntegerOption -> new SliderControl(sodiumGameOptionsIntegerOption, 0, 210, 10, ControlValueFormatter.number()))
                .setImpact(OptionImpact.MEDIUM)
                .setBinding(
                        (opts, value) -> ImmersiveCursedness.Config.horizontalSendLimit.set(value),
                        (opts) -> ImmersiveCursedness.Config.horizontalSendLimit.get()
                )
                .build();

        Option<Integer> atmosphereRadius = OptionImpl.createBuilder(Integer.TYPE, ic$sodiumOpts)
                .setName(Text.translatable("immersive.atmosphereRadius"))
                .setTooltip(Text.translatable("immersive.atmosphereRadius.tooltip"))
                .setControl(sodiumGameOptionsIntegerOption -> new SliderControl(sodiumGameOptionsIntegerOption, 0, 60, 6, ControlValueFormatter.number()))
                .setImpact(OptionImpact.MEDIUM)
                .setBinding(
                        (opts, value) -> ImmersiveCursedness.Config.atmosphereRadius.set(value),
                        (opts) -> ImmersiveCursedness.Config.atmosphereRadius.get()
                )
                .build();

        Option<Integer> renderDistance = OptionImpl.createBuilder(Integer.TYPE, ic$sodiumOpts)
                .setName(Text.translatable("immersive.renderDistance"))
                .setTooltip(Text.translatable("immersive.renderDistance.tooltip"))
                .setControl(sodiumGameOptionsIntegerOption -> new SliderControl(sodiumGameOptionsIntegerOption, 2, 32, 1, ControlValueFormatter.number()))
                .setImpact(OptionImpact.HIGH)
                .setBinding(
                        (opts, value) -> ImmersiveCursedness.Config.renderDistance.set(value),
                        (opts) -> ImmersiveCursedness.Config.renderDistance.get()
                )
                .build();

        groups.add(OptionGroup.createBuilder().add(renderDistance).add(horizontalSendLimit).add(atmosphereRadius).build());

        pages.add(new OptionPage(Text.translatable("immersive.page"), ImmutableList.copyOf(groups)));
    }
}
