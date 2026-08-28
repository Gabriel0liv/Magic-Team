package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;

/** Evo 3 uses the same target selection as the other Scourge variants but a different method descriptor. */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.item.bossweapon.scourgeofthesands.ScourgeOfTheSandsLevelThreeItem", remap = false)
public abstract class ScourgeOfTheSandsLevelThreeTargetingMixin {

    @Redirect(
            method = "spawnDesertStele(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;m_45976_(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;",
                    remap = false
            ),
            remap = false
    )
    private List<LivingEntity> magicTeam$excludeProtectedTargets(Level receiver,
                                                                  Class<LivingEntity> entityClass,
                                                                  AABB box,
                                                                  Player player,
                                                                  Level world) {
        List<LivingEntity> targets = new ArrayList<>(receiver.getEntitiesOfClass(entityClass, box));
        targets.removeIf(target -> TeamUtils.shouldBlockFriendlyFire(player, target));
        return targets;
    }
}
