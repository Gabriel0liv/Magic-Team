package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.TeamUtils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Dragon Spirit can switch from a SpellDamageSource to vanilla magic damage.
 * In that mode the central DamageSources mixin cannot infer that the operation
 * is a spell attack, so enforce the offensive policy at this concrete call site.
 */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.entity.projectiles.dragon_spirit.DragonSpiritEntity", remap = false)
public abstract class DragonSpiritFriendlyFireMixin {

    @Redirect(
            method = "m_8119_()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/damage/DamageSources;applyDamage(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/damagesource/DamageSource;)Z"
            ),
            remap = false
    )
    private boolean magicTeam$gateDragonSpiritDamage(Entity target, float amount, DamageSource damageSource) {
        Entity attacker = damageSource == null ? null : damageSource.getEntity();
        if (attacker == null && damageSource != null) {
            attacker = damageSource.getDirectEntity();
        }
        if (attacker == null) {
            attacker = TeamUtils.getRootOwner((Entity) (Object) this);
        }

        if (attacker != null && target != null && TeamUtils.shouldBlockFriendlyFire(attacker, target)) {
            return false;
        }
        return DamageSources.applyDamage(target, amount, damageSource);
    }
}
