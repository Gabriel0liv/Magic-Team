package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.AdapterContextBridge;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Spirit Damage Helper deals delayed damage from an effect, without a reliable spell source.
 *
 * <p>Context bridge: the whole ability runs as hostile magic from the
 * attacker so the global gates protect allies (alliance checks, damage,
 * effects, knockback and fire). Depends only on the method name, not on how
 * its body is compiled; if the method disappears the adapter is skipped.</p>
 */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.effects.SpiritDamageHelperEffect", remap = false)
public abstract class SpiritDamageHelperFriendlyFireMixin {
    @Inject(method = "findAndDamageTremorsaurus(Lnet/minecraft/world/entity/LivingEntity;I)V", at = @At("HEAD"), require = 0, remap = false)
    private void magicTeam$beginAbility(LivingEntity attacker, int amplifier, CallbackInfo ci) {
        AdapterContextBridge.beginHostile(attacker);
    }

    @Inject(method = "findAndDamageTremorsaurus(Lnet/minecraft/world/entity/LivingEntity;I)V", at = @At("RETURN"), require = 0, remap = false)
    private void magicTeam$endAbility(LivingEntity attacker, int amplifier, CallbackInfo ci) {
        AdapterContextBridge.end();
    }
}
