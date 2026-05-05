package com.gabri.magicteam.mixin;

import com.gabri.magicteam.util.MagicTeamEffectContext;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.spells.fire.AnnihilationSpell", remap = false)
public class AnnihilationSpellMixin {

    @Inject(
            method = "applyAoEDamageAndExplosion(Lnet/minecraft/world/level/Level;ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;FLnet/minecraft/world/effect/MobEffect;)V",
            at = @At("HEAD")
    )
    private void onApplyAoEDamageAndExplosionStart(Level level, int spellLevel, LivingEntity caster, LivingEntity livingTarget, float damage, MobEffect effect, CallbackInfo ci) {
        MagicTeamEffectContext.push(caster, (AbstractSpell) (Object) this, null);
    }

    @Inject(
            method = "applyAoEDamageAndExplosion(Lnet/minecraft/world/level/Level;ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;FLnet/minecraft/world/effect/MobEffect;)V",
            at = @At("RETURN")
    )
    private void onApplyAoEDamageAndExplosionEnd(Level level, int spellLevel, LivingEntity caster, LivingEntity livingTarget, float damage, MobEffect effect, CallbackInfo ci) {
        MagicTeamEffectContext.pop();
    }
}
