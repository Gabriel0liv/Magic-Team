package com.gabri.magicteam.mixin;

import com.gabri.magicteam.util.TeamUtils;
import com.gabri.magicteam.MagicTeamConfig;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void onAddEffect(MobEffectInstance effectInstance, @Nullable Entity source, CallbackInfoReturnable<Boolean> cir) {
        if (!MagicTeamConfig.SERVER.enableEffectBlock.get()) return;

        if (source == null) return;

        Entity trueSource = source;

        // Extract true caster from projectiles
        if (source instanceof Projectile projectile) {
            trueSource = projectile.getOwner();
        } 
        // Extract true caster from area clouds
        else if (source instanceof AreaEffectCloud cloud) {
            trueSource = cloud.getOwner();
        }
        // Fallback for Iron's Spells custom entities (if they don't implement Projectile/AreaEffectCloud)
        // Many ISS projectiles inherit from Projectile, so they are covered.
        
        if (trueSource == null || trueSource == (Object) this) return;

        if (TeamUtils.areAllies(trueSource, (Entity) (Object) this)) {
            // Block if the effect is NOT beneficial (Harmful or Neutral)
            if (effectInstance.getEffect().getCategory() != MobEffectCategory.BENEFICIAL) {
                // Visual Feedback
                TeamUtils.sendBlockedMessage(trueSource);
                
                cir.setReturnValue(false);
            }
        }
    }
}
