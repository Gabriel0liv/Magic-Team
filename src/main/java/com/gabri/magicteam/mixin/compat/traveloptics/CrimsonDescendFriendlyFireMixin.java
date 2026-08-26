package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/**
 * Crimson Descend's landing shockwave applies movement, damage and healing credit
 * in one loop. Filter protected teammates before that loop so they receive no
 * knockback/damage and cannot incorrectly contribute healing to the caster.
 */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.effects.CrimsonDescendEffect", remap = false)
public abstract class CrimsonDescendFriendlyFireMixin {

    @Redirect(
            method = "executeShockwave(Lnet/minecraft/world/entity/LivingEntity;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;m_45976_(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;",
                    remap = false
            ),
            remap = false
    )
    private List<LivingEntity> magicTeam$filterShockwaveTargets(ServerLevel level,
                                                                 Class<LivingEntity> entityClass,
                                                                 AABB area,
                                                                 LivingEntity caster,
                                                                 int amplifier) {
        List<LivingEntity> targets = level.getEntitiesOfClass(entityClass, area);
        if (caster == null) {
            return targets;
        }
        return targets.stream()
                .filter(target -> target == caster || !TeamUtils.shouldBlockFriendlyFire(caster, target))
                .toList();
    }
}
