package nl.theepicblock.immersive_cursedness.mixin.interdimensionalpackets;

import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerActionC2SPacket.class)
public class ClientPlayerActionMixin {
	@Redirect(method = "<init>(Lnet/minecraft/network/packet/c2s/play/PlayerActionC2SPacket$Action;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;toImmutable()Lnet/minecraft/util/math/BlockPos;"))
	public BlockPos toImmutableInject(@NotNull BlockPos pos) {
		return pos.mutableCopy();
	}
}
