package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Travel Optics' Thorns of Oblivion spawns Cataclysm amethyst clusters with the
 * player as owner. Cataclysm rejects allied targets before hurt(), so scoreboard
 * friendlyFire=true would otherwise never reach Magic Team's damage policy.
 * Only scoreboard-team relations are replaced; native Cataclysm alliances are
 * preserved for every other case.
 */
@Pseudo
@Mixin(targets = "com.github.L_Ender.cataclysm.entity.projectile.Amethyst_Cluster_Projectile_Entity", remap = false)
public abstract class CataclysmAmethystClusterFriendlyFireMixin {

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
