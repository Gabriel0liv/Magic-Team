package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.MagicTeamEffectContext;
import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Acid Rain performs hostile work after Iron's AoeEntity tick/checkHits scope has ended.
 * Cover all three audited side effects without redefining general ally semantics.
 */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.entity.projectiles.AcidRainAoe", remap = false)
public abstract class AcidRainAoeContextMixin {

    @Inject(method = "applyCorrodedEffectToEntities()V", at = @At("HEAD"), remap = false)
    private void magicTeam$beginCorrodedScope(CallbackInfo ci) {
        MagicTeamEffectContext.push((Entity) (Object) this, MagicTeamEffectContext.InteractionType.HARMFUL);
    }

    @Inject(method = "applyCorrodedEffectToEntities()V", at = @At("RETURN"), remap = false)
    private void magicTeam$endCorrodedScope(CallbackInfo ci) {
        MagicTeamEffectContext.pop();
    }

    /**
     * Acid Rain uses this helper only as a hostile-cleanse exclusion. Returning
     * shouldBlockFriendlyFire preserves the original surrounding `!isAlly(...)` shape:
     * protected allies are excluded, while friendlyFire=true allies become eligible.
     */
    @Inject(
            method = "isAlly(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void magicTeam$useFriendlyFireForCleanse(LivingEntity owner, LivingEntity target,
                                                      CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(TeamUtils.shouldBlockFriendlyFire(owner, target));
    }

    /**
     * Fire extension is a direct Entity side effect, not damage or a MobEffect, so it
     * needs its own narrow gate. The original isOnFire/range checks stay untouched.
     */
    @Redirect(
            method = "increaseFireTicksForEntities()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;m_20254_(I)V",
                    remap = false
            ),
            remap = false
    )
    private void magicTeam$gateFireExtension(Entity target, int seconds) {
        Entity owner = ((Projectile) (Object) this).getOwner();
        if (owner == null || !TeamUtils.shouldBlockFriendlyFire(owner, target)) {
            target.setSecondsOnFire(seconds);
        }
    }
}
