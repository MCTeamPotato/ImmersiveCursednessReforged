package nl.theepicblock.immersive_cursedness;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.apache.logging.log4j.Level;

import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;

public class CursednessServer {
    private final MinecraftServer server;
    private volatile boolean isServerActive = true;
    private long nextTick;
    private int tickCount;
    private final Map<ServerPlayerEntity, PlayerManager> playerManagers = new Object2ObjectOpenHashMap<>();
    public CursednessServer(MinecraftServer server) {
        this.server = server;
    }

    public void start() {
        ImmersiveCursedness.LOGGER.info("Starting immersive cursedness thread");
        while (isServerActive) {
            long currentTime = System.currentTimeMillis();
            if (currentTime < nextTick) {
                try {
                    Thread.sleep(nextTick-currentTime);
                } catch (InterruptedException ignored) {}
                continue;
            }
            try {
                tick();
                tickCount++;
            } catch (ConcurrentModificationException ignored) {
            } catch (Exception e) {
                ImmersiveCursedness.LOGGER.log(Level.ERROR, e.getMessage(), "Exception occurred whilst ticking the immersive cursedness thread. This is probably not bad unless it's spamming your console");
            }
            nextTick = System.currentTimeMillis()+50;
        }
    }

    public void stop() {
        ImmersiveCursedness.LOGGER.info("Stopping immersive cursedness thread");
        isServerActive = false;
    }

    public void tick() {
        //Sync player managers
        List<ServerPlayerEntity> playerList = server.getPlayerManager().getPlayerList();

        playerManagers.entrySet().removeIf(i -> !playerList.contains(i.getKey()));
        for (ServerPlayerEntity player : playerList) {
            if (!playerManagers.containsKey(player)) {
                playerManagers.put(player, new PlayerManager(player));
            }
        }

        //Tick player managers
        playerManagers.forEach((player, manager) -> manager.tick(tickCount));
    }

    public PlayerManager getManager(ServerPlayerEntity player) {
        return playerManagers.get(player);
    }
}
