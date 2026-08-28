package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.MagicTeamEffectContext;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Void Slash performs direct hurt() calls and applies Void Collapse itself,
 * including a vanilla-magic damage mode. Scope only its hostile action helpers
 * so both damage and debuffs respect scoreboard friendly fire.
 */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.entity.projectiles.void_slash.VoidSlashProjectile", remap = false)
public abstract class VoidSlashContextMixin {

    @Inject(method = "damageEntity(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), remap = false)
    private void magicTeam$beginDamageEntity(Entity target, CallbackInfo ci) {
        MagicTeamEffectContext.push((Entity) (Object) this, MagicTeamEffectContext.InteractionType.HARMFUL);
    }

    @Inject(method = "damageEntity(Lnet/minecraft/world/entity/Entity;)V", at = @At("RETURN"), remap = false)
    private void magicTeam$endDamageEntity(Entity target, CallbackInfo ci) {
        MagicTeamEffectContext.pop();
    }

    @Inject(method = "doCrossBehavior(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), remap = false)
    private void magicTeam$beginCrossBehavior(Entity triggerSource, CallbackInfo ci) {
        MagicTeamEffectContext.push((Entity) (Object) this, MagicTeamEffectContext.InteractionType.HARMFUL);
    }

    @Inject(method = "doCrossBehavior(Lnet/minecraft/world/entity/Entity;)V", at = @At("RETURN"), remap = false)
    private void magicTeam$endCrossBehavior(Entity triggerSource, CallbackInfo ci) {
        MagicTeamEffectContext.pop();
    }
}
