package nl.theepicblock.immersive_cursedness;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import nl.theepicblock.immersive_cursedness.objects.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.UUID;

import static nl.theepicblock.immersive_cursedness.ImmersiveCursedness.Config.atmosphereRadius;

public class PlayerManager {
    private final ServerPlayerEntity player;
    private final PortalManager portalManager;
    private BlockCache blockCache = new BlockCache();
    private final List<UUID> hiddenEntities = new ObjectArrayList<>();
    private ServerWorld previousWorld;

    public PlayerManager(ServerPlayerEntity player) {
        this.player = player;
        portalManager = new PortalManager(player);
    }

    public void tick(int tickCount) {
        if (!((PlayerInterface) player).immersivecursedness$getEnabled()) {
            return;
        }

        ServerWorld sourceWorld = ((PlayerInterface)player).immersivecursedness$getUnfakedWorld();
        ServerWorld destinationWorld = Util.getDestination(sourceWorld);
        AsyncWorldView sourceView = new AsyncWorldView(sourceWorld);
        AsyncWorldView destinationView = new AsyncWorldView(destinationWorld);

        boolean justWentThroughPortal = false;
        if (sourceWorld != previousWorld) {
            blockCache = new BlockCache();
            justWentThroughPortal = true;
        }
        var bottomOfWorld = sourceWorld.getBottomY();

        if (tickCount % 30 == 0 || justWentThroughPortal) {
            portalManager.update();
        }

        List<FlatStandingRectangle> sentLayers = new ObjectArrayList<>(portalManager.getPortals().size()* (atmosphereRadius.get() + 2));
        Chunk2IntMap sentBlocks = new Chunk2IntMap();
        BlockUpdateMap toBeSent = new BlockUpdateMap();
        List<BlockEntityUpdateS2CPacket> blockEntityPackets = new ArrayList<>();

        List<Entity> entities;
        try {
            entities = this.getEntitiesInRange(sourceWorld);
            if (tickCount % 200 == 0) removeNoLongerExistingEntities(entities);
        } catch (ConcurrentModificationException ignored) { entities = new ObjectArrayList<>(0); } // Not such a big deal, we'll get the entities next tick

        BlockState atmosphereBlock = (sourceWorld.getRegistryKey() == World.NETHER ? Blocks.BLUE_CONCRETE : Blocks.NETHER_WART_BLOCK).getDefaultState();
        BlockState atmosphereBetweenBlock = (sourceWorld.getRegistryKey() == World.NETHER ? Blocks.BLUE_STAINED_GLASS : Blocks.RED_STAINED_GLASS).getDefaultState();

        if (player.hasPortalCooldown()) return;

        boolean isCloseToPortal = false;
        //iterate through all portals
        for (Portal portal : portalManager.getPortals()) {
            if (portal.isCloserThan(player.getPos(), 6)) {
                isCloseToPortal = true;
            }
            TransformProfile transformProfile = portal.getTransformProfile();
            if (transformProfile == null) continue;

            if (tickCount % 40 == 0 || justWentThroughPortal) {
                //replace the portal blocks in the center of the portal with air
                BlockPos.iterate(portal.getLowerLeft(), portal.getUpperRight()).forEach(pos -> {
                    toBeSent.put(pos.toImmutable(), Blocks.AIR.getDefaultState());
                });
            }

            //iterate through all layers behind the portal
            FlatStandingRectangle rect = portal.toFlatStandingRectangle();
            for (int i = 1; i < (atmosphereRadius.get() + 2); i++) {
                FlatStandingRectangle rect2 = rect.expand(i, player.getCameraPosVec(1));
                sentLayers.add(rect2);
                if (ImmersiveCursedness.Config.debugParticles.get()) rect2.visualise(player);

                entities.removeIf((entity) -> {
                    if (rect2.contains(entity.getPos())) {
                        for (UUID uuid : hiddenEntities) {
                            if (entity.getUuid().equals(uuid)) {
                                return true; //cancel if the uuid is already in hiddenEntities
                            }
                        }
                        //If we've reached this point. The entity isn't hidden yet. So we should hide it
                        EntityPositionS2CPacket packet = createEntityPacket(entity, entity.getX() + 50, Double.MAX_VALUE);
                        player.networkHandler.sendPacket(packet);
                        hiddenEntities.add(entity.getUuid());
                        return true;
                    }
                    return false;
                });

                //go through all blocks in this layer and use the transformProfile to get the correct block in the nether. Then send it to the client
                rect2.iterateClamped(player.getPos(), ImmersiveCursedness.Config.horizontalSendLimit.get(), Util.calculateMinMax(sourceWorld, destinationWorld, transformProfile), (pos) -> {
                    double dist = Util.getDistance(pos, portal.getLowerLeft());
                    if (dist > Math.pow(atmosphereRadius.get() + 1, 2)) return;

                    BlockState ret;
                    BlockEntity entity = null;

                    if (dist >  Math.pow(atmosphereRadius.get(), 2)) {
                        ret = atmosphereBlock;
                    } else if (dist > Math.pow(atmosphereRadius.get() - 1, 2)) {
                        ret = atmosphereBetweenBlock;
                    } else {
                        ret = transformProfile.transformAndGetFromWorld(pos, destinationView);
                        entity = transformProfile.transformAndGetFromWorldBlockEntity(pos, destinationView);
                    }

                    if (pos.getY() == bottomOfWorld + 1) ret = atmosphereBetweenBlock;
                    if (pos.getY() == bottomOfWorld) ret = atmosphereBlock;

                    BlockPos imPos = pos.toImmutable();
                    sentBlocks.increment(imPos);
                    if (!(blockCache.get(imPos) == ret)) {
                        if (!ret.isAir() || !sourceView.getBlock(pos).isAir()) {
                            blockCache.put(imPos, ret);
                            toBeSent.put(imPos, ret);
                            if (entity != null) {
                                var buf = new PacketByteBuf(Unpooled.buffer());
                                buf.writeBlockPos(imPos);
                                buf.writeRegistryValue(Registry.BLOCK_ENTITY_TYPE, entity.getType());
                                buf.writeNbt(entity.toInitialChunkDataNbt());
                                blockEntityPackets.add(new BlockEntityUpdateS2CPacket(buf));
                            }
                        }
                    }
                });
            }
        }
        ((PlayerInterface)player).immersivecursedness$setCloseToPortal(isCloseToPortal);

        //get all of the old blocks and remove them
        blockCache.purge(sentBlocks, sentLayers, (pos, cachedState) -> {
            BlockState originalBlock = sourceView.getBlock(pos);
            if (originalBlock != cachedState) {
                toBeSent.put(pos, originalBlock);
                BlockEntity entity = sourceView.getBlockEntity(pos);
                if (entity != null) {
                    var buf = new PacketByteBuf(Unpooled.buffer());
                    buf.writeBlockPos(pos);
                    buf.writeRegistryValue(Registry.BLOCK_ENTITY_TYPE, entity.getType());
                    buf.writeNbt(entity.toInitialChunkDataNbt());
                    blockEntityPackets.add(new BlockEntityUpdateS2CPacket(buf));
                }
            }
            if (ImmersiveCursedness.Config.debugParticles.get()) Util.sendParticle(player, Util.getCenter(pos), 1, 0, originalBlock != cachedState ? 0 : 1);
        });

        entities.forEach(entity -> {
            for (UUID uuid : hiddenEntities) {
                if (entity.getUuid().equals(uuid)) {
                    hiddenEntities.remove(uuid);
                    player.networkHandler.sendPacket(new EntityPositionS2CPacket(entity));
                    return;
                }
            }
        });
        toBeSent.sendTo(this.player);
        for (var packet : blockEntityPackets) this.player.networkHandler.sendPacket(packet);
        previousWorld = sourceWorld;
    }

    public BlockPos transform(BlockPos p) {
        for (Portal portal : portalManager.getPortals()) {
            if (portal.isBlockposBehind(p, player.getPos()) && portal.getTransformProfile() != null) {
                return portal.getTransformProfile().transform(p);
            }
        }
        return null;
    }

    public TransformProfile getTransformProfile(BlockPos p) {
        for (Portal portal : portalManager.getPortals()) {
            if (portal.isBlockposBehind(p, player.getPos()) && portal.getTransformProfile() != null) {
                return portal.getTransformProfile();
            }
        }
        return null;
    }

    public void purgeCache() {
        BlockUpdateMap packetStorage = new BlockUpdateMap();
        ((PlayerInterface)player).immersivecursedness$setCloseToPortal(false);
        blockCache.purgeAll((pos, cachedState) -> {
            BlockState originalBlock = Util.getBlockAsync(player.getWorld(), pos);
            if (originalBlock != cachedState) {
                packetStorage.put(pos, originalBlock);
            }
            if (ImmersiveCursedness.Config.debugParticles.get()) Util.sendParticle(player, Util.getCenter(pos), 1, 0, originalBlock != cachedState ? 0 : 1);
        });
        for (Portal portal : portalManager.getPortals()) {
            BlockPos.iterate(portal.getLowerLeft(), portal.getUpperRight()).forEach(pos -> {
                packetStorage.put(pos.toImmutable(), Util.getBlockAsync(player.getWorld(), pos));
            });
        }
        packetStorage.sendTo(this.player);
    }

    private List<Entity> getEntitiesInRange(@NotNull ServerWorld world) {
        return world.getEntitiesByType(
                new AllExceptPlayer(),
                new Box(
                        player.getX() - ImmersiveCursedness.Config.renderDistance.get()*16,
                        player.getY() - ImmersiveCursedness.Config.renderDistance.get()*16,
                        player.getZ() - ImmersiveCursedness.Config.renderDistance.get()*16,
                        player.getX() + ImmersiveCursedness.Config.renderDistance.get()*16,
                        player.getY() + ImmersiveCursedness.Config.renderDistance.get()*16,
                        player.getZ() + ImmersiveCursedness.Config.renderDistance.get()*16
                ),
                (entity) -> true
        );
    }

    private void removeNoLongerExistingEntities(List<Entity> existingEntities) {
        hiddenEntities.removeIf((uuid) -> existingEntities.stream().noneMatch(entity -> uuid.equals(entity.getUuid())));
    }

    @Contract("_, _, _ -> new")
    private static @NotNull EntityPositionS2CPacket createEntityPacket(@NotNull Entity entity, double x, double y) {
        var buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(entity.getId());
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(entity.getZ());
        buf.writeByte((byte)((int)(entity.getYaw() * 256.0F / 360.0F)));
        buf.writeByte((byte)((int)(entity.getPitch() * 256.0F / 360.0F)));
        buf.writeBoolean(false);

        return new EntityPositionS2CPacket(buf);
    }

    private static class AllExceptPlayer implements TypeFilter<Entity, Entity> {

        @Nullable
        @Override
        public Entity downcast(Entity obj) {
            return obj;
        }

        @Override
        public Class<? extends Entity> getBaseClass() {
            return Entity.class;
        }
    }
}
