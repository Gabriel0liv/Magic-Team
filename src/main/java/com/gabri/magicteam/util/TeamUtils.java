package com.gabri.magicteam.util;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Team;

public class TeamUtils {
    
    /**
     * Checks if two entities are on the same Vanilla team.
     */
    public static boolean areAllies(Entity a, Entity b) {
        if (a == null || b == null) return false;
        Team teamA = a.getTeam();
        Team teamB = b.getTeam();
        boolean allies = teamA != null && teamA.isAlliedTo(teamB);
        
        // DEBUG LOGGING (Console)
        if (allies) {
            System.out.println("[MagicTeam] DEBUG: " + a.getName().getString() + " and " + b.getName().getString() + " ARE allies on team " + teamA.getName());
        }
        
        return allies;
    }

    /**
     * In-game feedback for blocking.
     */
    public static void sendProtectionMessage(Entity caster) {
        if (caster instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                Component.translatable("ui.magic_team.ally_protection")
                    .withStyle(ChatFormatting.RED)
            ));
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
