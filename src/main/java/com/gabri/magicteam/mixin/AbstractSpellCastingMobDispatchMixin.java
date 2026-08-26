package com.gabri.magicteam.mixin;

import com.gabri.magicteam.util.MagicTeamEffectContext;
import com.gabri.magicteam.util.TeamUtils;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Wraps mob spell virtual dispatch before addon overrides execute.
 *
 * <p>The enclosing vanilla override is selected with its Mojmap name so Mixin's
 * refmap can translate it for production. Calls into Iron's own spell API are
 * explicitly left unremapped because those method names are not Minecraft
 * mappings.</p>
 */
@Mixin(AbstractSpellCastingMob.class)
public abstract class AbstractSpellCastingMobDispatchMixin {

    @Redirect(
            method = "customServerAiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;onServerCastTick(Lnet/minecraft/world/level/Level;ILnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/magic/MagicData;)V",
                    remap = false
            )
    )
    private void magicTeam$dispatchMobCastTick(AbstractSpell spell,
                                                Level level,
                                                int spellLevel,
                                                LivingEntity entity,
                                                MagicData magicData) {
        MagicTeamEffectContext.push(entity, spell, CastSource.MOB, magicTeam$interaction(spell));
        try {
            spell.onServerCastTick(level, spellLevel, entity, magicData);
        } finally {
            MagicTeamEffectContext.pop();
        }
    }

    @Redirect(
            method = "customServerAiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;onCast(Lnet/minecraft/world/level/Level;ILnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/spells/CastSource;Lio/redspace/ironsspellbooks/api/magic/MagicData;)V",
                    remap = false
            )
    )
    private void magicTeam$dispatchMobCast(AbstractSpell spell,
                                            Level level,
                                            int spellLevel,
                                            LivingEntity entity,
                                            CastSource castSource,
                                            MagicData magicData) {
        MagicTeamEffectContext.push(entity, spell, castSource, magicTeam$interaction(spell));
        try {
            spell.onCast(level, spellLevel, entity, castSource, magicData);
        } finally {
            MagicTeamEffectContext.pop();
        }
    }

    @Redirect(
            method = "initiateCastSpell",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;onServerPreCast(Lnet/minecraft/world/level/Level;ILnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/magic/MagicData;)V",
                    remap = false
            ),
            remap = false
    )
    private void magicTeam$dispatchMobPreCast(AbstractSpell spell,
                                               Level level,
                                               int spellLevel,
                                               LivingEntity entity,
                                               MagicData magicData) {
        MagicTeamEffectContext.push(entity, spell, CastSource.MOB, magicTeam$interaction(spell));
        try {
            spell.onServerPreCast(level, spellLevel, entity, magicData);
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
