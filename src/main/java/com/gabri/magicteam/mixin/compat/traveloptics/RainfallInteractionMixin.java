package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.MagicTeamEffectContext;
import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Rainfall mixes support and hostile behavior in one entity. Its cleanse ally
 * check remains relational, while WET application is explicitly harmful.
 */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.entity.projectiles.RainfallAoe", remap = false)
public abstract class RainfallInteractionMixin {

    @Inject(
            method = "isAlly(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void magicTeam$supportRelation(LivingEntity owner, LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(TeamUtils.areAllies(owner, target));
    }

    @Inject(method = "applyWetEffectToEntities()V", at = @At("HEAD"), remap = false)
    private void magicTeam$beginWet(CallbackInfo ci) {
        MagicTeamEffectContext.push((Entity) (Object) this, MagicTeamEffectContext.InteractionType.HARMFUL);
    }

    @Inject(method = "applyWetEffectToEntities()V", at = @At("RETURN"), remap = false)
    private void magicTeam$endWet(CallbackInfo ci) {
        MagicTeamEffectContext.pop();
    }
}
