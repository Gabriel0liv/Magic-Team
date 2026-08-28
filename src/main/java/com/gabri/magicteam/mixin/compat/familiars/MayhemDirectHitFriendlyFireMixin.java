package com.gabri.magicteam.mixin.compat.familiars;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * End Mayhem's direct-hit projectiles execute addon side effects after Iron's
 * base projectile hit hook: three variants knock the target back and Chorus
 * Flower teleports it. Those effects run even when DamageSources later blocks
 * friendly-fire damage, so protected teammates must stop after the base hook.
 */
@Pseudo
@Mixin(targets = {
        "net.alshanex.alshanex_familiars.entity.misc.EndStoneEntity",
        "net.alshanex.alshanex_familiars.entity.misc.PurpurPilarEntity",
        "net.alshanex.alshanex_familiars.entity.misc.PurpurBricksEntity",
        "net.alshanex.alshanex_familiars.entity.misc.ChorusFlowerEntity"
}, remap = false)
public abstract class MayhemDirectHitFriendlyFireMixin extends Projectile {

    protected MayhemDirectHitFriendlyFireMixin(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
    }

    @Inject(
            method = "m_5790_(Lnet/minecraft/world/phys/EntityHitResult;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/entity/spells/AbstractMagicProjectile;m_5790_(Lnet/minecraft/world/phys/EntityHitResult;)V",
                    shift = At.Shift.AFTER,
                    remap = false
            ),
            cancellable = true,
            remap = false
    )
    private void magicTeam$skipProtectedDirectSideEffects(EntityHitResult hitResult, CallbackInfo ci) {
        Entity owner = getOwner();
        Entity target = hitResult.getEntity();
        if (owner != null && target != null && TeamUtils.shouldBlockFriendlyFire(owner, target)) {
            discard();
            ci.cancel();
        }
    }
}
