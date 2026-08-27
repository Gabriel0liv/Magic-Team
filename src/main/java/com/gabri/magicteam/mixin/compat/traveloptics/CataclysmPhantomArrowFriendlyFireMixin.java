package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cursed Volley creates Cataclysm phantom arrows with the spell caster as
 * vanilla projectile owner. The arrow may hit long after the spell context has
 * ended, so block protected teammates before fire/damage/enchantment effects.
 */
@Pseudo
@Mixin(targets = "com.github.L_Ender.cataclysm.entity.projectile.Phantom_Arrow_Entity", remap = false)
public abstract class CataclysmPhantomArrowFriendlyFireMixin {

    @Inject(
            method = "m_5790_(Lnet/minecraft/world/phys/EntityHitResult;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void magicTeam$gateDelayedHit(EntityHitResult hitResult, CallbackInfo ci) {
        if (hitResult != null
                && TeamUtils.shouldBlockFriendlyFire((Entity) (Object) this, hitResult.getEntity())) {
            ci.cancel();
        }
    }
}
