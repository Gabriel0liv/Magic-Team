package com.gabri.magicteam.mixin;

import com.gabri.magicteam.util.MagicTeamEffectContext;
import com.gabri.magicteam.util.TeamUtils;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Wraps the player's virtual onServerCastTick hook before addon overrides execute. */
@Mixin(value = MagicManager.class, remap = false)
public abstract class MagicManagerCastDispatchMixin {

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;onServerCastTick(Lnet/minecraft/world/level/Level;ILnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/magic/MagicData;)V"
            ),
            remap = false
    )
    private void magicTeam$dispatchPlayerCastTick(AbstractSpell spell,
                                                   Level level,
                                                   int spellLevel,
                                                   LivingEntity entity,
                                                   MagicData magicData) {
        CastSource castSource = magicData == null ? null : magicData.getCastSource();
        MagicTeamEffectContext.push(entity, spell, castSource, magicTeam$interaction(spell));
        try {
            spell.onServerCastTick(level, spellLevel, entity, magicData);
        } finally {
            MagicTeamEffectContext.pop();
        }
    }

    private static MagicTeamEffectContext.InteractionType magicTeam$interaction(AbstractSpell spell) {
        return TeamUtils.isHarmful(spell)
                ? MagicTeamEffectContext.InteractionType.HARMFUL
                : MagicTeamEffectContext.InteractionType.BENEFICIAL;
    }
}
