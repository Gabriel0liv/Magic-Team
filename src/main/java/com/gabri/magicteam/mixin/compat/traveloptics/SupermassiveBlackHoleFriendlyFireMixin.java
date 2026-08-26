package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The black hole's damage/hostile cleanse has a local ally prefilter, while
 * pull is applied earlier to every entity. Make both offensive paths obey FF.
 */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.entity.projectiles.supernova.supermassive_black_hole.SupermassiveBlackHoleEntity", remap = false)
public abstract class SupermassiveBlackHoleFriendlyFireMixin {

    @Inject(
            method = "isAlly(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void magicTeam$offensiveAllyGate(LivingEntity owner, LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(TeamUtils.shouldBlockFriendlyFire(owner, target));
    }

    @Redirect(
            method = "m_8119_()V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;m_20256_(Lnet/minecraft/world/phys/Vec3;)V"),
            remap = false
    )
    private void magicTeam$gatePull(Entity target, Vec3 motion) {
        Entity owner = TeamUtils.getRootOwner((Entity) (Object) this);
        if (owner == null || target == null || !TeamUtils.shouldBlockFriendlyFire(owner, target)) {
            target.m_20256_(motion);
        }
    }
}
