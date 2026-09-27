package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.effects.AbyssalStrike.AbyssalStrikeEffect", remap = false)
public abstract class AbyssalStrikeFriendlyFireMixin {
    @Inject(
            method = "isAlly(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void magicTeam$useFriendlyFireForPlayers(Player attacker, LivingEntity target,
                                                      CallbackInfoReturnable<Boolean> cir) {
        if (target instanceof Player) {
            cir.setReturnValue(TeamUtils.shouldBlockFriendlyFire(attacker, target));
        }
    }
}
