package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.MagicTeamEffectContext;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stellothorn uses vanilla direct/AOE damage outside Iron's helpers. Scope its
 * hostile helpers while leaving the owner's return-heal outside this context.
 */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.entity.projectiles.stellothorn_projectile.StellothornProjectileEntity", remap = false)
public abstract class StellothornContextMixin {

    @Inject(method = "damageEntity(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), remap = false)
    private void magicTeam$beginDamage(Entity target, CallbackInfo ci) {
        MagicTeamEffectContext.push((Entity) (Object) this, MagicTeamEffectContext.InteractionType.HARMFUL);
    }

    @Inject(method = "damageEntity(Lnet/minecraft/world/entity/Entity;)V", at = @At("RETURN"), remap = false)
    private void magicTeam$endDamage(Entity target, CallbackInfo ci) {
        MagicTeamEffectContext.pop();
    }

    @Inject(method = "beginReturn()V", at = @At("HEAD"), remap = false)
    private void magicTeam$beginReturnAoe(CallbackInfo ci) {
        MagicTeamEffectContext.push((Entity) (Object) this, MagicTeamEffectContext.InteractionType.HARMFUL);
    }

    @Inject(method = "beginReturn()V", at = @At("RETURN"), remap = false)
    private void magicTeam$endReturnAoe(CallbackInfo ci) {
        MagicTeamEffectContext.pop();
    }
}
