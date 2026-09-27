package com.gabri.magicteam.mixin;

import com.gabri.magicteam.util.MagicTeamEffectContext;
import com.gabri.magicteam.util.TeamUtils;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.command.CastCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collection;

/**
 * Preserves the global command-cast context bridge present in the published
 * 2.3.1 build. Iron's CastCommand can invoke onCast directly for non-player
 * living entities, bypassing the normal player and mob dispatch wrappers.
 */
@Mixin(value = CastCommand.class, remap = false)
public class CastCommandMixin {

    @Redirect(
            method = "castSpell(Lnet/minecraft/commands/CommandSourceStack;Ljava/util/Collection;Ljava/lang/String;I)I",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;onCast(Lnet/minecraft/world/level/Level;ILnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/spells/CastSource;Lio/redspace/ironsspellbooks/api/magic/MagicData;)V"
            ),
            remap = false
    )
    private static void magicTeam$wrapCommandCast(AbstractSpell spell,
                                                   Level level,
                                                   int spellLevel,
                                                   LivingEntity entity,
                                                   CastSource castSource,
                                                   MagicData magicData,
                                                   CommandSourceStack source,
                                                   Collection<? extends Entity> targets,
                                                   String spellId,
                                                   int requestedSpellLevel) {
        if (!TeamUtils.isEnabled()) {
            spell.onCast(level, spellLevel, entity, castSource, magicData);
            return;
        }

        MagicTeamEffectContext.InteractionType interactionType = TeamUtils.isHarmful(spell)
                ? MagicTeamEffectContext.InteractionType.HARMFUL
                : MagicTeamEffectContext.InteractionType.BENEFICIAL;

        MagicTeamEffectContext.push(entity, spell, castSource, interactionType);
        try {
            spell.onCast(level, spellLevel, entity, castSource, magicData);
        } finally {
            MagicTeamEffectContext.pop();
        }
    }
}
