package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Meteor Storm is beneficial on its caster but uses isTeammate only to exclude
 * targets from hostile projectile selection. Friendly-fire permission belongs here.
 */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.effects.MeteorStormEffect", remap = false)
public abstract class MeteorStormFriendlyFireMixin {
    @Inject(
            method = "isTeammate(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void magicTeam$useFriendlyFirePolicy(LivingEntity caster, LivingEntity target,
                                                  CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(TeamUtils.shouldBlockFriendlyFire(caster, target));
    }
}
