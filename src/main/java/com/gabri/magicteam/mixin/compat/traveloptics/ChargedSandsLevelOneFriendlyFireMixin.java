package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.AdapterContextBridge;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Charged Sands (level one) selects projectile targets from an item, outside the spell API.
 *
 * <p>Context bridge: the whole ability runs as hostile magic from the
 * shooter so the global gates protect allies (alliance checks, damage,
 * effects, knockback and fire). Depends only on the method name, not on how
 * its body is compiled; if the method disappears the adapter is skipped.</p>
 */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.item.bossweapon.charged_sands.ChargedSandsLevelOneItem", remap = false)
public abstract class ChargedSandsLevelOneFriendlyFireMixin {
    @Inject(method = "spawnProjectiles(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;FFF)V", at = @At("HEAD"), require = 0, remap = false)
    private void magicTeam$beginAbility(Level level, LivingEntity shooter, float a, float b, float c, CallbackInfo ci) {
        AdapterContextBridge.beginHostile(shooter);
    }

    @Inject(method = "spawnProjectiles(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;FFF)V", at = @At("RETURN"), require = 0, remap = false)
    private void magicTeam$endAbility(Level level, LivingEntity shooter, float a, float b, float c, CallbackInfo ci) {
        AdapterContextBridge.end();
    }
}
