package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/**
 * Shear of the Stars' DragonSpiritSpellEntity touches target hurt/knockback
 * state before delegating damage to Iron's DamageSources. Filter protected
 * allies out of the collision list so FF=false receives no pre-hit side effects,
 * while FF=true teammates remain valid targets.
 */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.entity.projectiles.dragon_spirit_spell_entity.DragonSpiritSpellEntity", remap = false)
public abstract class DragonSpiritSpellFriendlyFireMixin {

    @Redirect(
            method = "m_8119_()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;m_45933_(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;",
                    remap = false
            ),
            remap = false
    )
    private List<Entity> magicTeam$filterProtectedCollisionTargets(Level level, Entity except, AABB bounds) {
        List<Entity> entities = level.getEntities(except, bounds);
        Entity source = TeamUtils.getRootOwner((Entity) (Object) this);
        if (source == null) {
            return entities;
        }

        return entities.stream()
                .filter(target -> !TeamUtils.shouldBlockFriendlyFire(source, target))
                .toList();
    }
}
