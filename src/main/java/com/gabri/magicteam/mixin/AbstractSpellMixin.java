package com.gabri.magicteam.mixin;

import com.gabri.magicteam.util.MagicTeamEffectContext;
import com.gabri.magicteam.util.TeamUtils;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractSpell.class, remap = false)
public class AbstractSpellMixin {

    /**
     * Player casts enter AbstractSpell through attemptInitiateCast/castSpell.
     * Redirect the virtual hooks here so an addon override is inside the context
     * from its first instruction, rather than only when it eventually calls super.
     */
    @Redirect(
            method = "attemptInitiateCast",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;onServerPreCast(Lnet/minecraft/world/level/Level;ILnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/magic/MagicData;)V"
            )
    )
    private void magicTeam$dispatchPlayerPreCast(AbstractSpell spell,
                                                  Level level,
                                                  int spellLevel,
                                                  LivingEntity entity,
                                                  MagicData magicData) {
        CastSource castSource = magicData == null ? null : magicData.getCastSource();
        MagicTeamEffectContext.push(entity, spell, castSource, magicTeam$interaction(spell));
        try {
            spell.onServerPreCast(level, spellLevel, entity, magicData);
        } finally {
            MagicTeamEffectContext.pop();
        }
    }

    @Redirect(
            method = "castSpell",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;onCast(Lnet/minecraft/world/level/Level;ILnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/spells/CastSource;Lio/redspace/ironsspellbooks/api/magic/MagicData;)V"
            )
    )
    private void magicTeam$dispatchPlayerCast(AbstractSpell spell,
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

    /* Fallback scopes for direct/base-hook invocations outside the normal dispatchers. */
    @Inject(
            method = "onServerPreCast(Lnet/minecraft/world/level/Level;ILnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/magic/MagicData;)V",
            at = @At("HEAD")
    )
    private void onServerPreCastStart(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, CallbackInfo ci) {
        AbstractSpell spell = (AbstractSpell) (Object) this;
        CastSource castSource = playerMagicData == null ? null : playerMagicData.getCastSource();
        MagicTeamEffectContext.push(entity, spell, castSource, magicTeam$interaction(spell));
    }

    @Inject(
            method = "onServerPreCast(Lnet/minecraft/world/level/Level;ILnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/magic/MagicData;)V",
            at = @At("RETURN")
    )
    private void onServerPreCastEnd(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, CallbackInfo ci) {
        MagicTeamEffectContext.pop();
    }

    @Inject(
            method = "onServerCastTick(Lnet/minecraft/world/level/Level;ILnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/magic/MagicData;)V",
            at = @At("HEAD")
    )
    private void onServerCastTickStart(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, CallbackInfo ci) {
        AbstractSpell spell = (AbstractSpell) (Object) this;
        CastSource castSource = playerMagicData == null ? null : playerMagicData.getCastSource();
        MagicTeamEffectContext.push(entity, spell, castSource, magicTeam$interaction(spell));
    }

    @Inject(
            method = "onServerCastTick(Lnet/minecraft/world/level/Level;ILnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/magic/MagicData;)V",
            at = @At("RETURN")
    )
    private void onServerCastTickEnd(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, CallbackInfo ci) {
        MagicTeamEffectContext.pop();
    }

    @Inject(
            method = "onCast(Lnet/minecraft/world/level/Level;ILnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/spells/CastSource;Lio/redspace/ironsspellbooks/api/magic/MagicData;)V",
            at = @At("HEAD")
    )
    private void onCastStart(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData, CallbackInfo ci) {
        AbstractSpell spell = (AbstractSpell) (Object) this;
        MagicTeamEffectContext.push(entity, spell, castSource, magicTeam$interaction(spell));
    }

    @Inject(
            method = "onCast(Lnet/minecraft/world/level/Level;ILnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/spells/CastSource;Lio/redspace/ironsspellbooks/api/magic/MagicData;)V",
            at = @At("RETURN")
    )
    private void onCastEnd(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData, CallbackInfo ci) {
        MagicTeamEffectContext.pop();
    }

    private static MagicTeamEffectContext.InteractionType magicTeam$interaction(AbstractSpell spell) {
        return TeamUtils.isHarmful(spell)
                ? MagicTeamEffectContext.InteractionType.HARMFUL
                : MagicTeamEffectContext.InteractionType.BENEFICIAL;
    }
}
