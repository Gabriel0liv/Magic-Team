package com.gabri.magicteam.mixin.compat.geomancyplus;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Solar Storm is a beneficial self-effect that performs hostile target selection.
 * Guard the addon's target predicate directly instead of redirecting an internal
 * vanilla alliance call whose implementation changed in Geomancy Plus 2.0.0.
 */
@Pseudo
@Mixin(targets = "com.gametechbc.gtbcs_geomancy_plus.effects.SolarStormEffect", remap = false)
public abstract class SolarStormFriendlyFireMixin {

    @Inject(
            method = "isValidTarget(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void magicTeam$rejectProtectedTarget(LivingEntity first,
                                                  LivingEntity second,
                                                  CallbackInfoReturnable<Boolean> cir) {
        if (TeamUtils.shouldBlockFriendlyFire(first, second)) {
            cir.setReturnValue(false);
        }
    }
}
