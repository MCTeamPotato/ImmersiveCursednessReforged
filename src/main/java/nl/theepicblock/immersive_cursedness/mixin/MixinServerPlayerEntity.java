package nl.theepicblock.immersive_cursedness.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import nl.theepicblock.immersive_cursedness.ImmersiveCursedness;
import nl.theepicblock.immersive_cursedness.PlayerInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity extends MixinPlayerEntity implements PlayerInterface {
	protected MixinServerPlayerEntity(EntityType<? extends LivingEntity> arg, World arg2) {
		super(arg, arg2);
	}

	@Shadow public abstract ServerWorld getWorld();

	@Shadow public abstract void setWorld(ServerWorld world);

	@Unique private volatile boolean ic$isCloseToPortal;
	@Unique private ServerWorld ic$unFakedWorld;
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
	public void immersivecursedness$fakeWorld(ServerWorld world) {
		ic$unFakedWorld = this.getWorld();
		this.setWorld(world);
	}

	@Override
	public void immersivecursedness$deFakeWorld() {
		if (ic$unFakedWorld != null) {
			this.setWorld(ic$unFakedWorld);
			ic$unFakedWorld = null;
		}
	}

	@Override
	public ServerWorld immersivecursedness$getUnfakedWorld() {
		if (ic$unFakedWorld != null) return ic$unFakedWorld;
		return getWorld();
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

	@Override
	public void handleGetMaxNetherPortalTime(CallbackInfoReturnable<Integer> cir) {
		if (ic$enabled) cir.setReturnValue(1);
	}
}
