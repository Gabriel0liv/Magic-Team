package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.MagicTeamEffectContext;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tidal Grasp's MobEffect is beneficial on the caster but its final tick applies
 * stun, wet and damage to marked targets. Classify only that effect tick as a
 * harmful interaction so LivingEntity effect filtering follows friendly-fire.
 */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.effects.TidalGraspEffect", remap = false)
public abstract class TidalGraspEffectContextMixin {

    @Inject(
            method = "m_6742_(Lnet/minecraft/world/entity/LivingEntity;I)V",
            at = @At("HEAD"),
            remap = false
    )
    private void magicTeam$pushHarmfulContext(LivingEntity entity, int amplifier, CallbackInfo ci) {
        MagicTeamEffectContext.push(entity, MagicTeamEffectContext.InteractionType.HARMFUL);
    }

    @Inject(
            method = "m_6742_(Lnet/minecraft/world/entity/LivingEntity;I)V",
            at = @At("RETURN"),
            remap = false
    )
    private void magicTeam$popHarmfulContext(LivingEntity entity, int amplifier, CallbackInfo ci) {
        MagicTeamEffectContext.pop();
    }
}
