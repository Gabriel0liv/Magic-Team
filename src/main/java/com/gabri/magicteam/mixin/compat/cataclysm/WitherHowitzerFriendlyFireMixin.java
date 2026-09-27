package com.gabri.magicteam.mixin.compat.cataclysm;

import com.gabri.magicteam.util.MagicTeamEffectContext;
import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Published-2.3.1 compatibility exception. Cataclysm's Wither Howitzer can
 * execute outside the Iron's spell pipeline, so its impact needs a local
 * hostile magic scope to preserve the published friendly-fire behavior.
 */
@Pseudo
@Mixin(targets = "com.github.L_Ender.cataclysm.entity.projectile.Wither_Howitzer_Entity", remap = false)
public class WitherHowitzerFriendlyFireMixin {

    @Inject(method = "m_5790_(Lnet/minecraft/world/phys/EntityHitResult;)V", at = @At("HEAD"), require = 0)
    private void magicTeam$beginEntityHit(EntityHitResult hitResult, CallbackInfo ci) {
        if (TeamUtils.isEnabled()) {
            MagicTeamEffectContext.push((Entity) (Object) this, MagicTeamEffectContext.InteractionType.HARMFUL);
        }
    }

    @Inject(method = "m_5790_(Lnet/minecraft/world/phys/EntityHitResult;)V", at = @At("RETURN"), require = 0)
    private void magicTeam$endEntityHit(EntityHitResult hitResult, CallbackInfo ci) {
        if (TeamUtils.isEnabled()) {
            MagicTeamEffectContext.pop();
        }
    }

    @Inject(method = "m_6532_(Lnet/minecraft/world/phys/HitResult;)V", at = @At("HEAD"), require = 0)
    private void magicTeam$beginHit(HitResult hitResult, CallbackInfo ci) {
        if (TeamUtils.isEnabled()) {
            MagicTeamEffectContext.push((Entity) (Object) this, MagicTeamEffectContext.InteractionType.HARMFUL);
        }
    }

    @Inject(method = "m_6532_(Lnet/minecraft/world/phys/HitResult;)V", at = @At("RETURN"), require = 0)
    private void magicTeam$endHit(HitResult hitResult, CallbackInfo ci) {
        if (TeamUtils.isEnabled()) {
            MagicTeamEffectContext.pop();
        }
    }
}
