package com.gabri.magicteam.util;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;

public class TeamUtils {
    public static final Set<UUID> BYPASSED_PLAYERS = new HashSet<>();
    private static final Map<UUID, Long> LAST_MESSAGE_TIME = new HashMap<>();
    private static final long MESSAGE_COOLDOWN_MS = 1000;

    /**
     * Checks if two entities are on the same Vanilla team, including owner resolution for summons.
     */
    public static boolean areAllies(Entity a, Entity b) {
        if (a == null || b == null) return false;

        // Recursively resolve owners (Summons -> Owners)
        Entity rootA = getRootOwner(a);
        Entity rootB = getRootOwner(b);

        if (rootA == (Object) rootB) return true; // Mesma entidade ou dono

        // Se a aliança global estiver desligada, ninguém é considerado aliado (permite dano/mira)
        if (!com.gabri.magicteam.MagicTeamConfig.SERVER.enableGlobalAlliance.get()) {
            return false;
        }
        
        // Se o atacante (ou seu dono) estiver em Bypass, as alianças são ignoradas
        if (rootA instanceof ServerPlayer playerA && BYPASSED_PLAYERS.contains(playerA.getUUID())) {
            return false;
        }

        Team teamA = rootA.getTeam();
        Team teamB = rootB.getTeam();
        return teamA != null && teamA.isAlliedTo(teamB);
    }

    /**
     * Resolves the true owner of an entity (Projectiles, Summons, etc).
     */
    public static Entity getRootOwner(Entity entity) {
        if (entity instanceof net.minecraft.world.entity.projectile.Projectile projectile) {
            Entity owner = projectile.getOwner();
            if (owner != null && owner != entity) return getRootOwner(owner);
        }
        
        if (entity instanceof io.redspace.ironsspellbooks.entity.mobs.IMagicSummon summon) {
            Entity owner = summon.getSummoner();
            if (owner != null && owner != entity) return getRootOwner(owner);
        }

        if (entity instanceof net.minecraft.world.entity.AreaEffectCloud cloud) {
            Entity owner = cloud.getOwner();
            if (owner != null && owner != entity) return getRootOwner(owner);
        }

        return entity;
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

    /**
     * Implements the beneficial spell targeting rules.
     */
    public static boolean shouldAllowBeneficial(LivingEntity healer, LivingEntity target) {
        if (target instanceof Player) {
            return areAllies(healer, target);
        }
        return true;
    }

    public static boolean isSpellBeneficial(AbstractSpell spell) {
        if (spell == null) return false;
        return spell.getSchoolType().toString().toLowerCase().contains("holy");
    }
}
