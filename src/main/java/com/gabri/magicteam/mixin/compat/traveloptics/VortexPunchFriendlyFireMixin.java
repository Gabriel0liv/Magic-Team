package com.gabri.magicteam.mixin.compat.traveloptics;

import com.gabri.magicteam.util.TeamUtils;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Vortex Punch directly pulls nearby entities before its persistent vortex starts ticking. */
@Pseudo
@Mixin(targets = "com.gametechbc.traveloptics.spells.ender.VortexPunchSpell", remap = false)
public abstract class VortexPunchFriendlyFireMixin {

    @Redirect(
            method = "onCast(Lnet/minecraft/world/level/Level;ILnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/spells/CastSource;Lio/redspace/ironsspellbooks/api/magic/MagicData;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;m_20256_(Lnet/minecraft/world/phys/Vec3;)V"),
            remap = false
    )
    private void magicTeam$gateImmediatePull(Entity target,
                                              Vec3 motion,
                                              Level level,
                                              int spellLevel,
                                              LivingEntity caster,
                                              CastSource castSource,
                                              MagicData magicData) {
        if (caster == null || target == null || !TeamUtils.shouldBlockFriendlyFire(caster, target)) {
            target.m_20256_(motion);
        }
    }
}
