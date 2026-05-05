package com.gabri.magicteam.util;

import com.gabri.babel.core.util.BabelTeamSupport;
import com.gabri.magicteam.util.MagicTeamConfig;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeamUtils {
    private static final Map<UUID, Long> LAST_MESSAGE_TIME = new HashMap<>();
    private static final long MESSAGE_COOLDOWN_MS = 1000;

    /**
     * Determina se duas entidades devem ser tratadas como aliadas no momento atual.
     * Esta é a única regra do mod.
     */
    public static boolean areAllies(Entity a, Entity b) {
        if (a == null || b == null) return false;

        return BabelTeamSupport.areAllies(resolveComparisonEntity(a), resolveComparisonEntity(b));
    }

    /**
     * Resolves the true owner of an entity (Projectiles, Summons, etc).
     */
    public static Entity getRootOwner(Entity entity) {
        return resolveComparisonEntity(entity);
    }

    /**
     * In-game feedback for blocking.
     */
    @SuppressWarnings("null")
    public static void sendBlockedMessage(Entity entity) {
        if (entity instanceof ServerPlayer player) {
            long now = System.currentTimeMillis();
            long last = LAST_MESSAGE_TIME.getOrDefault(player.getUUID(), 0L);
            
            if (now - last > MESSAGE_COOLDOWN_MS) {
                player.sendSystemMessage(Component.translatable("magic_team.message.blocked").withStyle(ChatFormatting.RED), true);
                LAST_MESSAGE_TIME.put(player.getUUID(), now);
            }
        }
    }

    public static boolean shouldAllowHealing(Entity healer, Entity target) {
        if (healer == null || target == null) {
            return true;
        }

        return areAllies(healer, target);
    }

    public static boolean shouldAllowTarget(LivingEntity caster, LivingEntity target, AbstractSpell spell) {
        if (caster == null || target == null) {
            return true;
        }

        if (!areAllies(caster, target)) {
            return true;
        }

        return !isHarmful(spell);
    }

    public static boolean shouldAllowEffect(Entity source, Entity target, MobEffectInstance effectInstance) {
        return shouldAllowEffect(source, target, effectInstance, null);
    }

    public static boolean shouldAllowEffect(Entity source, Entity target, MobEffectInstance effectInstance, AbstractSpell spell) {
        if (source == null || target == null || effectInstance == null) {
            return true;
        }

        boolean allied = areAllies(source, target);
        return allied ? effectInstance.getEffect().isBeneficial() : !effectInstance.getEffect().isBeneficial();
    }

    /**
     * Used by damage gates that should block magic damage between allies.
     */
    public static boolean shouldBlockMagicDamage(Entity attacker, Entity target) {
        return shouldBlockMagicDamage(attacker, target, null);
    }

    public static boolean shouldBlockMagicDamage(Entity attacker, Entity target, AbstractSpell spell) {
        if (attacker == null || target == null) {
            return false;
        }

        return BabelTeamSupport.areAllies(resolveComparisonEntity(attacker), resolveComparisonEntity(target));
    }

    public static boolean isHarmful(AbstractSpell spell) {
        if (spell == null) {
            return false;
        }

        String spellId = normalizeSpellId(spell.getSpellId());
        if (spellId.isEmpty()) {
            return false;
        }

        if (matchesConfiguredSpell(spellId, MagicTeamConfig.SERVER.harmfulSpells.get())) {
            return true;
        }

        if (matchesConfiguredSpell(spellId, MagicTeamConfig.SERVER.beneficialSpells.get())) {
            return false;
        }

        return true;
    }

    public static boolean isSpellBeneficial(AbstractSpell spell) {
        return !isHarmful(spell);
    }

    private static boolean matchesConfiguredSpell(String spellId, Iterable<? extends String> entries) {
        for (String entry : entries) {
            if (entry == null) {
                continue;
            }

            String configured = normalizeSpellId(entry);
            if (configured.isEmpty()) {
                continue;
            }

            if (spellId.equals(configured)) {
                return true;
            }

            int colonIndex = spellId.indexOf(':');
            if (colonIndex >= 0 && spellId.substring(colonIndex + 1).equals(configured)) {
                return true;
            }

            if (!configured.contains(":")) {
                int configuredColon = configured.indexOf(':');
                if (configuredColon >= 0 && configured.substring(configuredColon + 1).equals(spellId)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String normalizeSpellId(String spellId) {
        return spellId == null ? "" : spellId.trim().toLowerCase();
    }

    private static Entity resolveComparisonEntity(Entity entity) {
        if (entity == null) {
            return null;
        }

        Entity root = BabelTeamSupport.getRootOwner(entity);
        return root != null ? root : entity;
    }
}
