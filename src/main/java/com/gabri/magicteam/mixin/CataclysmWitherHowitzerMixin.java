package com.gabri.magicteam.mixin;

import com.gabri.magicteam.util.MagicTeamEffectContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.L_Ender.cataclysm.entity.projectile.Wither_Howitzer_Entity", remap = false)
public class CataclysmWitherHowitzerMixin {

    @Inject(method = "m_5790_(Lnet/minecraft/world/phys/EntityHitResult;)V", at = @At("HEAD"))
    private void onEntityHitStart(EntityHitResult hitResult, CallbackInfo ci) {
        MagicTeamEffectContext.push((Entity) (Object) this);
    }

    @Inject(method = "m_5790_(Lnet/minecraft/world/phys/EntityHitResult;)V", at = @At("RETURN"))
    private void onEntityHitEnd(EntityHitResult hitResult, CallbackInfo ci) {
        MagicTeamEffectContext.pop();
    }

    @Inject(method = "m_6532_(Lnet/minecraft/world/phys/HitResult;)V", at = @At("HEAD"))
    private void onHitResultStart(HitResult hitResult, CallbackInfo ci) {
        MagicTeamEffectContext.push((Entity) (Object) this);
    }

    @Inject(method = "m_6532_(Lnet/minecraft/world/phys/HitResult;)V", at = @At("RETURN"))
    private void onHitResultEnd(HitResult hitResult, CallbackInfo ci) {
        MagicTeamEffectContext.pop();
    }
}
