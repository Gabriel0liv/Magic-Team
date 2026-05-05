package com.gabri.magicteam.mixin;

import com.gabri.magicteam.util.MagicTeamEffectContext;
import com.gabri.magicteam.util.TeamUtils;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(
            method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onAddEffect(MobEffectInstance effectInstance, CallbackInfoReturnable<Boolean> cir) {
        Entity source = MagicTeamEffectContext.getSource();
        if (source == null || effectInstance == null) {
            return;
        }

        LivingEntity target = (LivingEntity) (Object) this;
        AbstractSpell spell = MagicTeamEffectContext.getSpell();
        if (!TeamUtils.shouldAllowEffect(source, target, effectInstance, spell)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onAddEffectWithSource(MobEffectInstance effectInstance, Entity source, CallbackInfoReturnable<Boolean> cir) {
        if (source == null || effectInstance == null) {
            return;
        }

        LivingEntity target = (LivingEntity) (Object) this;
        AbstractSpell spell = MagicTeamEffectContext.getSpell();
        if (!TeamUtils.shouldAllowEffect(source, target, effectInstance, spell)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onHurt(DamageSource damageSource, float amount, CallbackInfoReturnable<Boolean> cir) {
        Entity source = MagicTeamEffectContext.getSource();
        if (source == null || damageSource == null) {
            return;
        }

        LivingEntity target = (LivingEntity) (Object) this;
        Entity attacker = damageSource.getEntity();
        if (attacker == null) {
            attacker = damageSource.getDirectEntity();
        }

        if (attacker == null) {
            attacker = source;
        }

        if (attacker == null) {
            return;
        }

        AbstractSpell spell = MagicTeamEffectContext.getSpell();
        if (TeamUtils.shouldBlockMagicDamage(attacker, target, spell)) {
            cir.setReturnValue(false);
        }
    }
}
