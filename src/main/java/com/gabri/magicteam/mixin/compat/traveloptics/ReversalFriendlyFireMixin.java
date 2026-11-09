package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.AdapterContextBridge;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Reversal retaliates from an effect, without reliable original spell
 * attribution.
 *
 * <ul>
 *     <li>The raycast retaliation runs as hostile magic from the reflector
 *     (context bridge), so the global gates protect allies.</li>
 *     <li>Reflected projectiles are re-spawned ownerless by the addon; give them
 *     the reflector as owner so Babel can resolve them later. This applies to
 *     every spawn in the method instead of a fixed position, so a reordered
 *     method body keeps working.</li>
 * </ul>
 */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.effects.Reversal.ReversalEffect", remap = false)
public abstract class ReversalFriendlyFireMixin {
    @Inject(method = "performRaycastAndApplyDamage(Lnet/minecraft/world/entity/LivingEntity;F)V",
            at = @At("HEAD"), require = 0, remap = false)
    private void magicTeam$beginRetaliation(LivingEntity reflector, float damage, CallbackInfo ci) {
        AdapterContextBridge.beginHostile(reflector);
    }

    @Inject(method = "performRaycastAndApplyDamage(Lnet/minecraft/world/entity/LivingEntity;F)V",
            at = @At("RETURN"), require = 0, remap = false)
    private void magicTeam$endRetaliation(LivingEntity reflector, float damage, CallbackInfo ci) {
        AdapterContextBridge.end();
    }

    @Redirect(
            method = "reflectProjectile(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/projectile/Projectile;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;m_7967_(Lnet/minecraft/world/entity/Entity;)Z",
                    remap = false
            ),
            require = 0,
            remap = false
    )
    private boolean magicTeam$preserveReflectedProjectileOwner(Level level,
                                                                 Entity spawned,
                                                                 LivingEntity reflector,
                                                                 Projectile originalProjectile) {
        if (spawned instanceof Projectile projectile && projectile.getOwner() == null && reflector != null) {
            projectile.setOwner(reflector);
        }
        return level.addFreshEntity(spawned);
    }
}
