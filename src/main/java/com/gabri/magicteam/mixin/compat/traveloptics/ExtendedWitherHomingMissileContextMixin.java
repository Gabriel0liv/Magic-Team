package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.MagicTeamEffectContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Travel Optics appends extra damage, Wither and an explosion after the
 * Cataclysm missile superclass handler. Scope the whole override as harmful.
 */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.entity.extended_projectiles.ExtendedWitherHomingMissileEntity", remap = false)
public abstract class ExtendedWitherHomingMissileContextMixin {

    @Inject(method = "m_5790_(Lnet/minecraft/world/phys/EntityHitResult;)V", at = @At("HEAD"), remap = false)
    private void magicTeam$beginHit(EntityHitResult hitResult, CallbackInfo ci) {
        MagicTeamEffectContext.push((Entity) (Object) this, MagicTeamEffectContext.InteractionType.HARMFUL);
    }

    @Inject(method = "m_5790_(Lnet/minecraft/world/phys/EntityHitResult;)V", at = @At("RETURN"), remap = false)
    private void magicTeam$endHit(EntityHitResult hitResult, CallbackInfo ci) {
        MagicTeamEffectContext.pop();
    }
}
