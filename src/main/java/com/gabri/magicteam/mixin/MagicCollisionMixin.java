package com.gabri.magicteam.mixin;

import com.gabri.magicteam.util.TeamUtils;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import io.redspace.ironsspellbooks.entity.spells.StompAoe;
import io.redspace.ironsspellbooks.entity.spells.fire_arrow.FireArrowProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin cirúrgico para forçar a detecção de colisão com aliados
 * apenas quando o bloqueio de mira estiver desativado.
 * 
 * Usamos remap = false porque estas são classes e métodos do mod (não vanilla).
 */
@Mixin(value = {AbstractMagicProjectile.class, FireArrowProjectile.class, StompAoe.class}, remap = false)
public abstract class MagicCollisionMixin {

    // Casos sem parâmetros: handleHitDetection e checkHits
    @Inject(method = {"handleHitDetection", "checkHits"}, at = @At("HEAD"), remap = false, require = 0)
    private void onHitDetectionHead(CallbackInfo ci) {
        if (!com.gabri.magicteam.MagicTeamConfig.SERVER.enableTargetingBlock.get()) {
            TeamUtils.FORCE_ENEMIES_SCOPE.set(true);
        }
    }

    @Inject(method = {"handleHitDetection", "checkHits"}, at = @At("RETURN"), remap = false, require = 0)
    private void onHitDetectionTail(CallbackInfo ci) {
        TeamUtils.FORCE_ENEMIES_SCOPE.set(false);
    }

    // Caso com parâmetro: onHit(HitResult)
    @Inject(method = "onHit(Lnet/minecraft/world/phys/HitResult;)V", at = @At("HEAD"), remap = false, require = 0)
    private void onHitWithParamHead(net.minecraft.world.phys.HitResult hitResult, CallbackInfo ci) {
        if (!com.gabri.magicteam.MagicTeamConfig.SERVER.enableTargetingBlock.get()) {
            TeamUtils.FORCE_ENEMIES_SCOPE.set(true);
        }
    }

    @Inject(method = "onHit(Lnet/minecraft/world/phys/HitResult;)V", at = @At("RETURN"), remap = false, require = 0)
    private void onHitWithParamTail(net.minecraft.world.phys.HitResult hitResult, CallbackInfo ci) {
        TeamUtils.FORCE_ENEMIES_SCOPE.set(false);
    }
}
