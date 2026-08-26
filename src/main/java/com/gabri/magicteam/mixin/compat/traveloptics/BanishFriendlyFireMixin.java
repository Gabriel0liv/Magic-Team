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

/**
 * Banish removes every beneficial effect from nearby targets without consulting
 * Iron's DamageSources. The spell dispatcher supplies the caster context; skip
 * only the hostile cleanse when scoreboard friendly fire protects the target.
 */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.spells.holy.BanishSpell", remap = false)
public abstract class BanishFriendlyFireMixin {

    @Inject(
            method = "removePositiveEffects(Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void magicTeam$gateHostileCleanse(LivingEntity target, CallbackInfo ci) {
        Entity caster = MagicTeamEffectContext.getSource();
        if (caster != null && target != null && TeamUtils.shouldBlockFriendlyFire(caster, target)) {
            ci.cancel();
        }
    }
}
