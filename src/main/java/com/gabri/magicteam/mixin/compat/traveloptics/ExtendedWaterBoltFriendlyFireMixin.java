package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.entity.extended_projectiles.ExtendedWaterBoltEntity", remap = false)
public abstract class ExtendedWaterBoltFriendlyFireMixin {
    @Inject(
            method = "applyDirectDamage(Lnet/minecraft/world/phys/EntityHitResult;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void magicTeam$gateDirectDamage(EntityHitResult hitResult, CallbackInfo ci) {
        Entity owner = ((Projectile) (Object) this).getOwner();
        Entity target = hitResult.getEntity();
        if (owner != null && target != null && TeamUtils.shouldBlockFriendlyFire(owner, target)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "isAlly(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void magicTeam$useFriendlyFireForAoe(LivingEntity owner, LivingEntity target,
                                                  CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(TeamUtils.shouldBlockFriendlyFire(owner, target));
    }
}
