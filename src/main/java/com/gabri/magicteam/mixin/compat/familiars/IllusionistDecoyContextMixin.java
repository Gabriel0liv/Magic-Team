package com.gabri.magicteam.mixin.compat.familiars;

import com.gabri.magicteam.util.MagicTeamEffectContext;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The decoy's detonation mixes DamageSources.applyDamage with a vanilla Explosion.
 * Keep one harmful magic scope around the synchronous detonation so explosion damage
 * receives the same owner-aware friendly-fire gate as the explicit damage loop.
 */
@Pseudo
@Mixin(targets = "net.alshanex.alshanex_familiars.entity.misc.IllusionistDecoy", remap = false)
public abstract class IllusionistDecoyContextMixin {

    @Inject(
            method = "m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At("HEAD"),
            remap = false
    )
    private void magicTeam$beginDetonationScope(net.minecraft.world.damagesource.DamageSource source,
                                                 float amount,
                                                 CallbackInfoReturnable<Boolean> cir) {
        MagicTeamEffectContext.push((Entity) (Object) this, MagicTeamEffectContext.InteractionType.HARMFUL);
    }

    @Inject(
            method = "m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At("RETURN"),
            remap = false
    )
    private void magicTeam$endDetonationScope(net.minecraft.world.damagesource.DamageSource source,
                                               float amount,
                                               CallbackInfoReturnable<Boolean> cir) {
        MagicTeamEffectContext.pop();
    }
}
