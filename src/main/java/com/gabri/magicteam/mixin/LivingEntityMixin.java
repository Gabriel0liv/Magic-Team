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
        if (MagicTeamEffectContext.isVanillaPotionApplication()) {
            return;
        }

        Entity source = MagicTeamEffectContext.getSource();
        if (source == null || effectInstance == null) {
            return;
        }

        LivingEntity target = (LivingEntity) (Object) this;
        AbstractSpell spell = MagicTeamEffectContext.getSpell();
        MagicTeamEffectContext.InteractionType interactionType = MagicTeamEffectContext.getInteractionType();
        if (!TeamUtils.shouldAllowEffect(source, target, effectInstance, spell, interactionType)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onAddEffectWithSource(MobEffectInstance effectInstance, Entity source, CallbackInfoReturnable<Boolean> cir) {
        if (MagicTeamEffectContext.isVanillaPotionApplication()) {
            return;
        }

        if (source == null || effectInstance == null) {
            return;
        }

        LivingEntity target = (LivingEntity) (Object) this;
        AbstractSpell spell = MagicTeamEffectContext.getSpell();
        MagicTeamEffectContext.InteractionType interactionType = MagicTeamEffectContext.getInteractionType();
        if (!TeamUtils.shouldAllowEffect(source, target, effectInstance, spell, interactionType)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onHurt(DamageSource damageSource, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!MagicTeamEffectContext.shouldFilterDamage() || damageSource == null) {
            return;
        }

        Entity source = MagicTeamEffectContext.getSource();
        if (source == null) {
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

        // A context is only allowed to classify damage produced by the same resolved owner.
        // This prevents a stale context from one spell/AOE from reclassifying unrelated damage.
        Entity contextOwner = TeamUtils.getRootOwner(source);
        Entity attackerOwner = TeamUtils.getRootOwner(attacker);
        if (contextOwner != null && attackerOwner != null && contextOwner != attackerOwner) {
            return;
        }

        AbstractSpell spell = MagicTeamEffectContext.getSpell();
        if (TeamUtils.shouldBlockMagicDamage(attacker, target, spell)) {
            cir.setReturnValue(false);
        }
    }
}
