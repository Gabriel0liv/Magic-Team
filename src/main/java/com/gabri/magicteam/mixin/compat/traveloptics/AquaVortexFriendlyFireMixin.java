package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Aqua Vortex has offensive targeting/movement outside AoeEntity#checkHits.
 * Keep extinguishing/support behavior unchanged while making Boltstrike and
 * forced pull/lift obey scoreboard friendly fire.
 */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.entity.projectiles.aqua_vortex.AquaVortexEntity", remap = false)
public abstract class AquaVortexFriendlyFireMixin {

    @Inject(
            method = "isAlly(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void magicTeam$offensiveAllyGate(LivingEntity owner, LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(TeamUtils.shouldBlockFriendlyFire(owner, target));
    }

    @Redirect(
            method = "pullEntitiesTowardsCenter()V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;m_20256_(Lnet/minecraft/world/phys/Vec3;)V"),
            remap = false
    )
    private void magicTeam$gatePull(LivingEntity target, Vec3 motion) {
        if (!magicTeam$protectedTarget(target)) {
            target.m_20256_(motion);
        }
    }

    @Redirect(
            method = "liftAndThrowEntity(Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;m_20256_(Lnet/minecraft/world/phys/Vec3;)V"),
            remap = false
    )
    private void magicTeam$gateLiftAndThrow(LivingEntity target, Vec3 motion) {
        if (!magicTeam$protectedTarget(target)) {
            target.m_20256_(motion);
        }
    }

    private boolean magicTeam$protectedTarget(LivingEntity target) {
        Entity self = (Entity) (Object) this;
        Entity owner = TeamUtils.getRootOwner(self);
        return owner != null && target != null && TeamUtils.shouldBlockFriendlyFire(owner, target);
    }
}
