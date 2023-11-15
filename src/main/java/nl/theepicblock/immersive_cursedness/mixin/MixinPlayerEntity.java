package nl.theepicblock.immersive_cursedness.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity extends LivingEntity {
    protected MixinPlayerEntity(EntityType<? extends LivingEntity> arg, World arg2) {
        super(arg, arg2);
    }

    /**
     * @reason makes it so the portal always takes 1 tick to go through. Even when in survival
     * @author TheEpicBlock_TEB
     */
    @SuppressWarnings("CancellableInjectionUsage")
    @Inject(method = "getMaxNetherPortalTime", at = @At("HEAD"), cancellable = true)
    public void handleGetMaxNetherPortalTime(CallbackInfoReturnable<Integer> cir) {}
}
