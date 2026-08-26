package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Travel Optics has a parallel AOE hierarchy which filters owner allies before applyEffect.
 * In the audited version its direct subclasses are hostile (Flood Pool and Brine Pool).
 */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.api.entity.SyncedAoeEntity", remap = false)
public abstract class SyncedAoeEntityFriendlyFireMixin {

    @Redirect(
            method = "m_5603_(Lnet/minecraft/world/entity/Entity;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;m_7307_(Lnet/minecraft/world/entity/Entity;)Z",
                    remap = false
            ),
            remap = false
    )
    private boolean magicTeam$useFriendlyFirePolicy(Entity owner, Entity target) {
        return TeamUtils.shouldBlockFriendlyFire(owner, target);
    }
}
