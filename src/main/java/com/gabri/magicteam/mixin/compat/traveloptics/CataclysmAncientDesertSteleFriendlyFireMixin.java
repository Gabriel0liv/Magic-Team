package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Stele Cascade and Scourge of the Sands preserve Cataclysm's custom caster on
 * Ancient_Desert_Stele_Entity, but Cataclysm rejects allied targets before
 * damage. Replace only scoreboard-team alliance with Magic Team's FF policy;
 * native non-scoreboard Cataclysm alliances remain unchanged.
 */
@Pseudo
@Mixin(targets = "com.github.L_Ender.cataclysm.entity.projectile.Ancient_Desert_Stele_Entity", remap = false)
public abstract class CataclysmAncientDesertSteleFriendlyFireMixin {

    @Redirect(
            method = "m_5790_(Lnet/minecraft/world/phys/EntityHitResult;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;m_7307_(Lnet/minecraft/world/entity/Entity;)Z",
                    remap = false
            ),
            remap = false
    )
    private boolean magicTeam$scoreboardAwareAlliance(Entity source, Entity target) {
        if (source == null || target == null) {
            return false;
        }

        Team sourceTeam = source.getTeam();
        Team targetTeam = target.getTeam();
        if (sourceTeam != null && targetTeam != null && sourceTeam.isAlliedTo(targetTeam)) {
            return TeamUtils.shouldBlockFriendlyFire(source, target);
        }

        return source.isAlliedTo(target);
    }
}
