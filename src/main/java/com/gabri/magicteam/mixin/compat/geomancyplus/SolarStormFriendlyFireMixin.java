package com.gabri.magicteam.mixin.compat.geomancyplus;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Solar Storm is a beneficial self-effect that performs hostile target selection.
 * Only the internal hostile target predicate is changed; the effect itself remains beneficial.
 */
@Pseudo
@Mixin(targets = "com.gametechbc.gtbcs_geomancy_plus.effects.SolarStormEffect", remap = false)
public abstract class SolarStormFriendlyFireMixin {

    @Redirect(
            method = "isValidTarget(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;m_7307_(Lnet/minecraft/world/entity/Entity;)Z",
                    remap = false
            ),
            remap = false
    )
    private boolean magicTeam$useFriendlyFirePolicy(Entity target, Entity caster) {
        return TeamUtils.shouldBlockFriendlyFire(caster, target);
    }
}
