package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.item.armor.PrimordialCrestArmorItem", remap = false)
public abstract class PrimordialCrestFriendlyFireMixin {
    @Redirect(
            method = "triggerKnockbackAndDamage(Lnet/minecraft/world/entity/player/Player;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;m_7307_(Lnet/minecraft/world/entity/Entity;)Z", remap = false),
            remap = false
    )
    private boolean magicTeam$useFriendlyFirePolicy(Entity player, Entity target) {
        return TeamUtils.shouldBlockFriendlyFire(player, target);
    }
}
