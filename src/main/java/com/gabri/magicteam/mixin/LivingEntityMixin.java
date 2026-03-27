package com.gabri.magicteam.mixin;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.world.entity.LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void onAddEffect(net.minecraft.world.effect.MobEffectInstance effectInstance, Entity source, CallbackInfoReturnable<Boolean> cir) {
        if (source == null) return;

        // Se eles são aliados (respeitando Aliança Global e Bypass)
        if (TeamUtils.areAllies(source, (Entity) (Object) this)) {
            // Se o efeito for de uma magia e for nocivo, bloqueamos.
            // (Assumimos que efeitos negativos de magias são indesejados entre aliados por padrão)
            if (!effectInstance.getEffect().isBeneficial()) {
                TeamUtils.sendBlockedMessage(source);
                cir.setReturnValue(false);
            }
        }
    }
}
