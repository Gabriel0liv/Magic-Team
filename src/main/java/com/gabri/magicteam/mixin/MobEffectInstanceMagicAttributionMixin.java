package com.gabri.magicteam.mixin;

import com.gabri.magicteam.util.MagicAttribution;
import com.gabri.magicteam.util.MagicEffectAttributionIndex;
import com.gabri.magicteam.util.MagicTeamEffectContext;
import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Re-enters the original magic context while an attributed MobEffect performs its delayed tick. */
@Mixin(MobEffectInstance.class)
public abstract class MobEffectInstanceMagicAttributionMixin {

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/effect/MobEffect;applyEffectTick(Lnet/minecraft/world/entity/LivingEntity;I)V"
            )
    )
    private void magicTeam$withEffectAttribution(MobEffect effect, LivingEntity target, int amplifier) {
        if (!TeamUtils.isEnabled() || target == null) {
            effect.applyEffectTick(target, amplifier);
            return;
        }

        MagicAttribution attribution = MagicEffectAttributionIndex.get(
                target,
                effect,
                target.level().getGameTime()
        );
        if (attribution == null) {
            effect.applyEffectTick(target, amplifier);
            return;
        }

        MagicTeamEffectContext.push(target, attribution);
        try {
            effect.applyEffectTick(target, amplifier);
        } finally {
            MagicTeamEffectContext.pop();
        }
    }
}
