package com.gabri.magicteam.mixin;

import com.gabri.magicteam.util.MagicAttribution;
import com.gabri.magicteam.util.MagicAttributionIndex;
import com.gabri.magicteam.util.MagicEffectAttributionIndex;
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
        if (MagicTeamEffectContext.isVanillaPotionApplication() || !TeamUtils.isEnabled()) {
            return;
        }

        Entity source = MagicTeamEffectContext.getSource();
        if (source == null || effectInstance == null) {
            return;
        }

        LivingEntity target = (LivingEntity) (Object) this;
        AbstractSpell spell = MagicTeamEffectContext.getSpell();
        MagicAttribution attribution = resolveAttribution(source);
        MagicTeamEffectContext.InteractionType interactionType = MagicTeamEffectContext.getInteractionType();
        if (!TeamUtils.shouldAllowEffect(source, target, effectInstance, spell, attribution, interactionType)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z",
            at = @At("RETURN")
    )
    private void magicTeam$recordEffectAttribution(MobEffectInstance effectInstance,
                                                   CallbackInfoReturnable<Boolean> cir) {
        if (!TeamUtils.isEnabled() || effectInstance == null || !cir.getReturnValueZ()) {
            return;
        }

        MagicAttribution attribution = MagicTeamEffectContext.currentAttribution();
        if (attribution == null) {
            return;
        }

        LivingEntity target = (LivingEntity) (Object) this;
        MagicEffectAttributionIndex.record(
                target,
                effectInstance,
                attribution,
                target.level().getGameTime()
        );
    }

    @Inject(
            method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onAddEffectWithSource(MobEffectInstance effectInstance, Entity source, CallbackInfoReturnable<Boolean> cir) {
        if (MagicTeamEffectContext.isVanillaPotionApplication() || !TeamUtils.isEnabled()) {
            return;
        }

        if (source == null || effectInstance == null) {
            return;
        }

        LivingEntity target = (LivingEntity) (Object) this;
        AbstractSpell spell = MagicTeamEffectContext.getSpell();
        MagicAttribution attribution = resolveAttribution(source);
        MagicTeamEffectContext.InteractionType interactionType = MagicTeamEffectContext.getInteractionType();
        if (!TeamUtils.shouldAllowEffect(source, target, effectInstance, spell, attribution, interactionType)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("RETURN")
    )
    private void magicTeam$recordEffectAttributionWithSource(MobEffectInstance effectInstance,
                                                             Entity source,
                                                             CallbackInfoReturnable<Boolean> cir) {
        if (!TeamUtils.isEnabled() || effectInstance == null || source == null || !cir.getReturnValueZ()) {
            return;
        }

        MagicAttribution attribution = resolveAttribution(source);
        if (attribution == null) {
            return;
        }

        LivingEntity target = (LivingEntity) (Object) this;
        MagicEffectAttributionIndex.record(
                target,
                effectInstance,
                attribution,
                target.level().getGameTime()
        );
    }

    @Inject(
            method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onHurt(DamageSource damageSource, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!TeamUtils.isEnabled() || damageSource == null) {
            return;
        }

        LivingEntity target = (LivingEntity) (Object) this;
        Entity attacker = damageSource.getEntity();
        if (attacker == null) {
            attacker = damageSource.getDirectEntity();
        }

        Entity contextSource = MagicTeamEffectContext.getSource();
        if (attacker == null) {
            attacker = contextSource;
        }
        if (attacker == null) {
            return;
        }

        MagicAttribution attribution = resolveAttribution(attacker);
        AbstractSpell spell = MagicTeamEffectContext.getSpell();
        MagicTeamEffectContext.InteractionType interactionType = MagicTeamEffectContext.getInteractionType();

        boolean hasMagicEvidence = spell != null
                || attribution != null
                || MagicTeamEffectContext.shouldFilterDamage();
        if (!hasMagicEvidence) {
            return;
        }

        if (contextSource != null && attribution == null) {
            Entity contextOwner = TeamUtils.getRootOwner(contextSource);
            Entity attackerOwner = TeamUtils.getRootOwner(attacker);
            if (contextOwner != null && attackerOwner != null && contextOwner != attackerOwner) {
                return;
            }
        }

        if (TeamUtils.shouldBlockMagicDamage(attacker, target, spell, attribution, interactionType)) {
            cir.setReturnValue(false);
        }
    }

    private static MagicAttribution resolveAttribution(Entity source) {
        MagicAttribution current = MagicTeamEffectContext.currentAttribution();
        if (current != null) {
            return current;
        }
        return MagicAttributionIndex.get(source, source.level().getGameTime());
    }
}
