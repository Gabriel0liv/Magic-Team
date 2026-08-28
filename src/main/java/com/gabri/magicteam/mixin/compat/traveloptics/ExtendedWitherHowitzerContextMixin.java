package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.MagicTeamEffectContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Travel Optics adds extra direct/AOE damage after the Cataclysm superclass hit
 * handlers return. Keep the whole subclass override inside a harmful scope so
 * those post-super operations use Magic Team's owner-aware friendly-fire gate.
 */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.entity.extended_projectiles.ExtendedWitherHowitzerEntity", remap = false)
public abstract class ExtendedWitherHowitzerContextMixin {

    @Inject(method = "m_5790_(Lnet/minecraft/world/phys/EntityHitResult;)V", at = @At("HEAD"), remap = false)
    private void magicTeam$beginEntityHit(EntityHitResult hitResult, CallbackInfo ci) {
        MagicTeamEffectContext.push((Entity) (Object) this, MagicTeamEffectContext.InteractionType.HARMFUL);
    }

    @Inject(method = "m_5790_(Lnet/minecraft/world/phys/EntityHitResult;)V", at = @At("RETURN"), remap = false)
    private void magicTeam$endEntityHit(EntityHitResult hitResult, CallbackInfo ci) {
        MagicTeamEffectContext.pop();
    }

    @Inject(method = "m_6532_(Lnet/minecraft/world/phys/HitResult;)V", at = @At("HEAD"), remap = false)
    private void magicTeam$beginHit(HitResult hitResult, CallbackInfo ci) {
        MagicTeamEffectContext.push((Entity) (Object) this, MagicTeamEffectContext.InteractionType.HARMFUL);
    }

    @Inject(method = "m_6532_(Lnet/minecraft/world/phys/HitResult;)V", at = @At("RETURN"), remap = false)
    private void magicTeam$endHit(HitResult hitResult, CallbackInfo ci) {
        MagicTeamEffectContext.pop();
    }
}
