package nl.theepicblock.immersive_cursedness;

import com.mojang.brigadier.Command;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

@Mod("immersive_cursedness")
public class ImmersiveCursedness {
    public static final Logger LOGGER = LogManager.getLogger("ImmersiveCursedness");
    public static Thread cursednessThread;
    public static CursednessServer cursednessServer;

    @SubscribeEvent
    public void onServerStarted(@NotNull ServerStartedEvent event) {
        MinecraftServer minecraftServer = event.getServer();
        cursednessThread = new Thread(() -> {
            cursednessServer = new CursednessServer(minecraftServer);
            cursednessServer.start();
        });
        cursednessThread.start();
        cursednessThread.setName("Immersive Cursedness Thread");
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        cursednessServer.stop();
    }

    @SubscribeEvent
    public void onCmdRegister(RegisterCommandsEvent event) {
        event.getDispatcher().register(CommandManager.literal("portal")
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
                })));
    }

    public ImmersiveCursedness() {
        MinecraftForge.EVENT_BUS.register(this);
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
