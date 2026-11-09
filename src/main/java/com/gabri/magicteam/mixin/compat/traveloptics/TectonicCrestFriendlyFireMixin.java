package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.AdapterContextBridge;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tectonic Crest armor shockwave damages nearby entities.
 *
 * <p>Context bridge: the whole ability runs as hostile magic from the
 * player so the global gates protect allies (alliance checks, damage,
 * effects, knockback and fire). Depends only on the method name, not on how
 * its body is compiled; if the method disappears the adapter is skipped.</p>
 */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.item.armor.TectonicCrestArmorItem", remap = false)
public abstract class TectonicCrestFriendlyFireMixin {
    @Inject(method = "triggerTectonicShockwave(Lnet/minecraft/world/entity/player/Player;F)V", at = @At("HEAD"), require = 0, remap = false)
    private static void magicTeam$beginAbility(Player player, float power, CallbackInfo ci) {
        AdapterContextBridge.beginHostile(player);
    }

    @Inject(method = "triggerTectonicShockwave(Lnet/minecraft/world/entity/player/Player;F)V", at = @At("RETURN"), require = 0, remap = false)
    private static void magicTeam$endAbility(Player player, float power, CallbackInfo ci) {
        AdapterContextBridge.end();
    }
}
