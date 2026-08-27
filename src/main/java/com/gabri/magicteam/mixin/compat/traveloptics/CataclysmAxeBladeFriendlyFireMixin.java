package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/**
 * Despair spawns Cataclysm's Axe_Blade_Entity and the projectile can hit long
 * after the spell cast context has ended. Gate the delayed entity hit using the
 * projectile's persisted owner so scoreboard friendly fire is still enforced.
 */
@Pseudo
@Mixin(targets = "com.github.L_Ender.cataclysm.entity.projectile.Axe_Blade_Entity", remap = false)
public abstract class CataclysmAxeBladeFriendlyFireMixin {

    @Shadow(remap = false)
    @Nullable
    public abstract LivingEntity getOwner();

    @Inject(
            method = "onHitEntity(Lnet/minecraft/world/phys/EntityHitResult;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void magicTeam$gateDelayedHit(EntityHitResult hitResult, CallbackInfo ci) {
        LivingEntity owner = getOwner();
        if (owner != null && hitResult != null
                && TeamUtils.shouldBlockFriendlyFire(owner, hitResult.getEntity())) {
            ci.cancel();
        }
    }
}
