package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.function.Predicate;

/**
 * Arcane Artillery's missile raycast previously considered protected teammates
 * valid lock-on targets. Filter them before the closest-target selection so the
 * spell can continue to the next valid hostile target instead of wasting shots.
 */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.spells.lightning.ArcaneArtillerySpell", remap = false)
public abstract class ArcaneArtilleryTargetingMixin {

    @Redirect(
            method = "getRaycastTarget(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;D)Lnet/minecraft/world/entity/Entity;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;m_6249_(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",
                    remap = false
            ),
            remap = false
    )
    private List<Entity> magicTeam$excludeProtectedTargets(Level receiver,
                                                            Entity source,
                                                            AABB box,
                                                            Predicate<? super Entity> predicate,
                                                            Level level,
                                                            LivingEntity caster,
                                                            double range) {
        return receiver.getEntities(source, box,
                target -> predicate.test(target) && !TeamUtils.shouldBlockFriendlyFire(caster, target));
    }
}
