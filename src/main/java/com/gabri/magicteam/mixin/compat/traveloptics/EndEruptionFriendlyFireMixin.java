package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.function.Predicate;

/**
 * End Eruption bypasses AoeEntity#checkHits and performs a direct hurt inside
 * its delayed triggerEruption loop. Filter protected teammates from that loop
 * while leaving the eruption timing, particles and sounds untouched.
 */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.entity.mobs.nightwarden_boss.misc.EndEruptionEntity", remap = false)
public abstract class EndEruptionFriendlyFireMixin extends Projectile {

    protected EndEruptionFriendlyFireMixin(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
    }

    @Redirect(
            method = "triggerEruption()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;m_6443_(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",
                    remap = false
            ),
            remap = false
    )
    private <T extends Entity> List<T> magicTeam$filterProtectedTargets(Level level,
                                                                        Class<T> entityClass,
                                                                        AABB box,
                                                                        Predicate<? super T> predicate) {
        Entity owner = getOwner();
        return level.getEntitiesOfClass(
                entityClass,
                box,
                target -> predicate.test(target)
                        && (owner == null || !TeamUtils.shouldBlockFriendlyFire(owner, target))
        );
    }
}
