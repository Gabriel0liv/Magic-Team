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
    
    public static void loadBypassData() {
        BYPASSED_PLAYERS.clear();
        BYPASSED_PLAYERS.addAll(BypassDataStorage.load());
    }

    public static void saveBypassData() {
        BypassDataStorage.save(BYPASSED_PLAYERS);
    }
    
    /**
     * Determina se duas entidades devem ser tratadas como aliadas no momento atual.
     * Esta é a "autoridade absoluta" do mod, respeitando bypass e configurações de PvP.
     */
    public static boolean areAllies(Entity a, Entity b) {
        if (a == null || b == null) return false;

        // 1. Resolvemos os donos reais (Players por trás de projéteis/summons)
        Entity rootA = getRootOwner(a);
        Entity rootB = getRootOwner(b);

        // 2. Se for o próprio ou tiver o mesmo dono, é aliado
        if (rootA == rootB) return true;

        // 3. Se ALGUM dos lados estiver em BYPASS, tratamos como INIMIGOS (PvP Livre)
        if (isBypassed(rootA) || isBypassed(rootB)) {
            return false;
        }

        // 4. Se a aliança global estiver DESLIGADA, ninguém é aliado (PvP Global)
        if (!com.gabri.magicteam.MagicTeamConfig.SERVER.enableGlobalAlliance.get()) {
            return false;
        }

        // 5. Se chegamos aqui, respeitamos os times do Minecraft Vanilla
        Team teamA = rootA.getTeam();
        Team teamB = rootB.getTeam();
        if (teamA != null && teamB != null) {
            return teamA.isAlliedTo(teamB);
        }

        // Sem time, não são aliados por padrão
        return false;
    }

    public static boolean isBypassed(Entity entity) {
        Entity root = getRootOwner(entity);
        if (root instanceof ServerPlayer player) {
            return BYPASSED_PLAYERS.contains(player.getUUID());
        }
        return false;
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

    /**
     * Centralized logic to determine if a spell is harmful.
     * Checks explicit harmful list, beneficial list, and fallback to school type.
     */
    public static boolean isHarmful(AbstractSpell spell) {
        if (spell == null) return false;

        String id = spell.getSpellId().toLowerCase();
        
        // 1. Check explicit harmful blacklist
        boolean isExplicitlyHarmful = com.gabri.magicteam.MagicTeamConfig.SERVER.explicitHarmfulSpells.get().stream()
                .anyMatch(h -> id.contains(h.toLowerCase()));
        
        if (isExplicitlyHarmful) {
            return true;
        }
        
        // 2. Check beneficial whitelist
        boolean isBeneficial = com.gabri.magicteam.MagicTeamConfig.SERVER.beneficialSpells.get().stream()
                .anyMatch(b -> id.contains(b.toLowerCase()));

        if (isBeneficial) {
            return false;
        }
        
        // 3. Fallback: Everything else is considered harmful by default (to be safe)
        return true;
    }

    public static boolean isSpellBeneficial(AbstractSpell spell) {
        return !isHarmful(spell);
    }
}
