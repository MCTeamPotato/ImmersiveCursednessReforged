package nl.theepicblock.immersive_cursedness;

import com.mojang.brigadier.Command;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("immersive_cursedness")
public class ImmersiveCursedness {
    public static final Logger LOGGER = LogManager.getLogger("ImmersiveCursedness");
    public static Thread cursednessThread;
    public static CursednessServer cursednessServer;

    public ImmersiveCursedness() {
        ServerLifecycleEvents.SERVER_STARTED.register(minecraftServer -> {
            cursednessThread = new Thread(() -> {
                cursednessServer = new CursednessServer(minecraftServer);
                cursednessServer.start();
            });
            cursednessThread.start();
            cursednessThread.setName("Immersive Cursedness Thread");
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(minecraftServer -> cursednessServer.stop());

        CommandRegistrationCallback.EVENT.register((dispatcher, b, c) -> dispatcher.register(CommandManager.literal("portal")
                .then(CommandManager.literal("toggle").executes((context) -> {
                    PlayerInterface pi = (PlayerInterface)context.getSource().getPlayer();
                    if (pi != null){
                        pi.immersivecursedness$setEnabled(!pi.immersivecursedness$getEnabled());
                        context.getSource().sendFeedback(Text.literal("you have now " + (pi.immersivecursedness$getEnabled() ? "enabled" : "disabled") + " immersive portals"), false);
                        if (!pi.immersivecursedness$getEnabled()) {
                            Util.getManagerFromPlayer(context.getSource().getPlayer()).purgeCache();
                        }
                    }
                    return Command.SINGLE_SUCCESS;
                }))));

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.forgeConfigSpec);
    }

    public static class Config {
        public static final ForgeConfigSpec forgeConfigSpec;
        public static final ForgeConfigSpec.IntValue horizontalSendLimit, atmosphereRadius, renderDistance;
        public static final ForgeConfigSpec.BooleanValue debugParticles, defaultEnabled;

        static {
            ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
            builder.push("ImmersiveCursednessReforged");
            horizontalSendLimit = builder.defineInRange("horizontalSendLimit", 70, 0, Integer.MAX_VALUE);
            atmosphereRadius = builder.comment("The radius where the outer block of the atmosphere should be").defineInRange("atmosphereRadius", 28, 0, Integer.MAX_VALUE);
            renderDistance = builder.comment("Measured in chunks").defineInRange("renderDistance", 3, 0, Integer.MAX_VALUE);
            debugParticles = builder.define("debugParticles", false);
            defaultEnabled = builder.comment("Whether Immersive Cursedness is on by default").define("defaultEnabled", true);
            builder.pop();
            forgeConfigSpec = builder.build();
        }
    }
}
