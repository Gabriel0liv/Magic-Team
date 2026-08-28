package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Psychic Control's secondary AOE uses a local offensive ally prefilter. */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.entity.projectiles.psychic_bolt.PsychicBoltProjectile", remap = false)
public abstract class PsychicBoltFriendlyFireMixin {

    @Inject(
            method = "isAlly(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void magicTeam$offensiveAllyGate(LivingEntity owner, LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(TeamUtils.shouldBlockFriendlyFire(owner, target));
    }
}
