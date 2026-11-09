package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Harbinger's Wrath pulse targeting asks its own {@code isAllyOrTamed(target,
 * attacker)} helper. Answer "ally" up front for Magic Team allies and let the
 * addon's original tamed/team logic run otherwise. Depends only on the helper
 * name, not on how its body is compiled.
 */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.item.bossweapon.harbingerwrath.HarbingersWrathLevelThreeItem", remap = false)
public abstract class HarbingersWrathFriendlyFireMixin {
    @Inject(
            method = "isAllyOrTamed(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private void magicTeam$treatMagicTeamAlliesAsAllies(LivingEntity target,
                                                         LivingEntity attacker,
                                                         CallbackInfoReturnable<Boolean> cir) {
        if (attacker != null && target != null && !attacker.level().isClientSide()
                && TeamUtils.shouldBlockFriendlyFire(attacker, target)) {
            cir.setReturnValue(true);
        }
    }
}
