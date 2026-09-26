package com.gabri.magicteam.mixin;

import com.gabri.magicteam.util.MagicTeamEffectContext;
import com.gabri.magicteam.util.TeamUtils;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
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
            at = @At("HEAD"),
            cancellable = true
    )
    private void onServerPreCastStart(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, CallbackInfo ci) {
        AbstractSpell spell = (AbstractSpell) (Object) this;
        if (magicTeam$cancelProtectedTarget(level, entity, playerMagicData, spell)) {
            TeamUtils.sendBlockedMessage(entity);
            ci.cancel();
            return;
        }

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
            at = @At("HEAD"),
            cancellable = true
    )
    private void onServerCastTickStart(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, CallbackInfo ci) {
        AbstractSpell spell = (AbstractSpell) (Object) this;
        if (magicTeam$cancelProtectedTarget(level, entity, playerMagicData, spell)) {
            TeamUtils.sendBlockedMessage(entity);
            ci.cancel();
            return;
        }

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
            at = @At("HEAD"),
            cancellable = true
    )
    private void onCastStart(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData, CallbackInfo ci) {
        AbstractSpell spell = (AbstractSpell) (Object) this;
        if (magicTeam$cancelProtectedTarget(level, entity, playerMagicData, spell)) {
            TeamUtils.sendBlockedMessage(entity);
            ci.cancel();
            return;
        }

        MagicTeamEffectContext.push(entity, spell, castSource, magicTeam$interaction(spell));
    }

    @Inject(
            method = "onCast(Lnet/minecraft/world/level/Level;ILnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/spells/CastSource;Lio/redspace/ironsspellbooks/api/magic/MagicData;)V",
            at = @At("RETURN")
    )
    private void onCastEnd(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData, CallbackInfo ci) {
        MagicTeamEffectContext.pop();
    }

    private static boolean magicTeam$cancelProtectedTarget(Level level,
                                                            LivingEntity caster,
                                                            MagicData magicData,
                                                            AbstractSpell spell) {
        if (!TeamUtils.isEnabled() || !TeamUtils.isHarmful(spell) || caster == null || magicData == null) {
            return false;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (!(magicData.getAdditionalCastData() instanceof TargetEntityCastData targetData)) {
            return false;
        }

        Entity target = targetData.getTarget(serverLevel);
        return target != null && TeamUtils.shouldBlockFriendlyFire(caster, target);
    }

    private static MagicTeamEffectContext.InteractionType magicTeam$interaction(AbstractSpell spell) {
        return TeamUtils.isHarmful(spell)
                ? MagicTeamEffectContext.InteractionType.HARMFUL
                : MagicTeamEffectContext.InteractionType.BENEFICIAL;
    }
}
