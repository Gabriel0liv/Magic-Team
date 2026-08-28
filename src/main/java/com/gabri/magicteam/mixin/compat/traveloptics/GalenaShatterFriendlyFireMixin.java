package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Galena Shatter's initial pulse uses Iron's DamageSources, but stacked targets
 * are processed separately afterwards. Reject protected teammates before the
 * spell consumes their stacks or creates a delayed Galena Mark.
 */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.spells.lightning.GalenaShatterSpell", remap = false)
public abstract class GalenaShatterFriendlyFireMixin {

    @Inject(
            method = "processStackedTarget(Lnet/minecraft/world/entity/LivingEntity;Ljava/lang/String;ILnet/minecraft/world/entity/LivingEntity;Z)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void magicTeam$gateGalenaMarkApplication(
            LivingEntity target,
            String stackName,
            int spellLevel,
            LivingEntity caster,
            boolean isAzure,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (caster != null && target != null && TeamUtils.shouldBlockFriendlyFire(caster, target)) {
            cir.setReturnValue(false);
        }
    }
}
