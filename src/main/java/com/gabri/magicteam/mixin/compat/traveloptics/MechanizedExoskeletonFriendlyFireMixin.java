package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.item.armor.MechanizedExoskeletonArmorItem", remap = false)
public abstract class MechanizedExoskeletonFriendlyFireMixin {
    @Redirect(
            method = "findMissileTarget(Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/entity/LivingEntity;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;m_7307_(Lnet/minecraft/world/entity/Entity;)Z", remap = false),
            require = 2,
            remap = false
    )
    private boolean magicTeam$useFriendlyFirePolicy(Entity target, Entity player) {
        return TeamUtils.shouldBlockFriendlyFire(player, target);
    }
}
