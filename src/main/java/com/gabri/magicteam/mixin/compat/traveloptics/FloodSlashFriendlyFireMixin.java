package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Flood Slash performs damage, applies Wet and rewards the caster with mana
 * and Replenish in one method, without checking whether applyDamage succeeded.
 * Stop the whole hit transaction for protected teammates and remember that the
 * target was processed so the expanding projectile does not retry every tick.
 */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.entity.projectiles.flood_slash.FloodSlashProjectile", remap = false)
public abstract class FloodSlashFriendlyFireMixin extends Projectile {

    @Shadow
    private List<Entity> victims;

    protected FloodSlashFriendlyFireMixin(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(
            method = "damageEntity(Lnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void magicTeam$skipProtectedHit(Entity target, CallbackInfo ci) {
        Entity owner = getOwner();
        if (owner != null && target != null && TeamUtils.shouldBlockFriendlyFire(owner, target)) {
            if (!victims.contains(target)) {
                victims.add(target);
            }
            ci.cancel();
        }
    }
}
