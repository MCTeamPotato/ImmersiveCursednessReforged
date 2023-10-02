package nl.theepicblock.immersive_cursedness.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.encryption.PlayerPublicKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import nl.theepicblock.immersive_cursedness.ImmersiveCursedness;
import nl.theepicblock.immersive_cursedness.PlayerInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity extends PlayerEntity implements PlayerInterface {
	public MixinServerPlayerEntity(World world, BlockPos pos, float yaw, GameProfile profile, PlayerPublicKey publicKey) {
		super(world, pos, yaw, profile, publicKey);
	}

	@Unique private volatile boolean ic$isCloseToPortal;
	@Unique private World ic$unFakedWorld;
	@Unique private boolean ic$enabled = true;

	@Override
	public void immersivecursedness$setCloseToPortal(boolean v) {
		ic$isCloseToPortal = v;
	}

	@Override
	public boolean immersivecursedness$getCloseToPortal() {
		return ic$isCloseToPortal;
	}

	@Override
	public void immersivecursedness$fakeWorld(World world) {
		ic$unFakedWorld = this.world;
		this.world = world;
	}

	@Override
	public void immersivecursedness$deFakeWorld() {
		if (ic$unFakedWorld != null) {
			this.world = ic$unFakedWorld;
			ic$unFakedWorld = null;
		}
	}

	@Override
	public ServerWorld immersivecursedness$getUnfakedWorld() {
		if (ic$unFakedWorld != null) return (ServerWorld) ic$unFakedWorld;
		return (ServerWorld) getWorld();
	}

	@Inject(method = "writeCustomDataToNbt", at = @At("HEAD"))
	public void writeInject(NbtCompound tag, CallbackInfo ci) {
		if (ic$enabled != ImmersiveCursedness.Config.defaultEnabled.get()) {
			tag.putBoolean("immersivecursednessenabled", ic$enabled);
		}
	}

	@Inject(method = "readCustomDataFromNbt", at = @At("HEAD"))
	public void readInject(NbtCompound tag, CallbackInfo ci) {
		if (tag.contains("immersivecursednessenabled")) {
			ic$enabled = tag.getBoolean("immersivecursednessenabled");
		} else {
			ic$enabled = ImmersiveCursedness.Config.defaultEnabled.get();
		}
	}

	@Override
	public void immersivecursedness$setEnabled(boolean v) {
		ic$enabled = v;
	}

	@Override
	public boolean immersivecursedness$getEnabled() {
		return ic$enabled;
	}
}
