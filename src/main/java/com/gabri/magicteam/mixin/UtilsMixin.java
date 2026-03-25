package com.gabri.magicteam.mixin;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import com.gabri.magicteam.util.TeamUtils;
import com.gabri.magicteam.MagicTeamConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import java.util.function.Predicate;

@Mixin(value = Utils.class, remap = false)
public class UtilsMixin {

    /**
     * Hook Mestre: Modificamos o Filtro (Predicate) do seletor de alvos.
     */
    @ModifyVariable(
        method = "preCastTargetHelper(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/magic/MagicData;Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;IFZLjava/util/function/Predicate;)Lnet/minecraft/world/entity/LivingEntity;",
        at = @At("HEAD"),
        argsOnly = true,
        remap = false,
        require = 0
    )
    private static Predicate<LivingEntity> wrapFilter_3153(Predicate<LivingEntity> original, Level level, LivingEntity caster, MagicData magicData, AbstractSpell spell) {
        return wrapPredicate(original, caster, spell);
    }

    @ModifyVariable(
        method = "preCastTargetHelper(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/magic/MagicData;Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;IFZLjava/util/function/Predicate;)Z",
        at = @At("HEAD"),
        argsOnly = true,
        remap = false,
        require = 0
    )
    private static Predicate<LivingEntity> wrapFilter_3154(Predicate<LivingEntity> original, Level level, LivingEntity caster, MagicData magicData, AbstractSpell spell) {
        return wrapPredicate(original, caster, spell);
    }

    private static Predicate<LivingEntity> wrapPredicate(Predicate<LivingEntity> original, LivingEntity caster, AbstractSpell spell) {
        if (!MagicTeamConfig.SERVER.enableTargetingBlock.get()) return original;

        if (spell != null && isHarmful(spell)) {
            return (target) -> {
                boolean isAlly = TeamUtils.areAllies(caster, target);
                if (isAlly) {
                    TeamUtils.LAST_BLOCK_WAS_ALLY.set(true); // Sinaliza para substituição da mensagem no Redirect
                    return false;
                }
                return original == null || original.test(target);
            };
        }
        return original;
    }

    private static boolean isHarmful(AbstractSpell spell) {
        if (spell == null) return false;

        String id = spell.getSpellId().toLowerCase();
        
        // 1. Verificar se está na blacklist (magias ofensivas explicitas)
        // Suporta tanto "smite" quanto "irons_spellbooks:smite"
        boolean isExplicitlyHarmful = MagicTeamConfig.SERVER.explicitHarmfulSpells.get().stream()
                .anyMatch(harmfulStr -> {
                    String h = harmfulStr.toLowerCase();
                    return id.equals(h) || id.equals("irons_spellbooks:" + h) || id.contains(":" + h);
                });
        
        if (isExplicitlyHarmful) return true;
        
        // 2. Verificar se está na whitelist (magias de suporte/buff)
        boolean isSupport = MagicTeamConfig.SERVER.beneficialSpells.get().stream()
                .anyMatch(beneficialStr -> {
                    String b = beneficialStr.toLowerCase();
                    return id.equals(b) || id.equals("irons_spellbooks:" + b) || id.contains(":" + b);
                });

        if (isSupport) return false;
        
        // 3. Fallback: Se for da escola HOLY e não está na blacklist, é de suporte
        try {
            if (spell.getSchoolType().getId().equals(SchoolRegistry.HOLY_RESOURCE)) {
                return false;
            }
        } catch (Exception e) {}

        return true;
    }

    /**
     * Suprime a mensagem genérica do ISS ("Requires a target") se acabarmos de bloquear por ser aliado.
     * Versão 3.15.3 (Criação de pacote direta)
     */
    @SuppressWarnings("all")
    @Redirect(method = {
        "preCastTargetHelper(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/magic/MagicData;Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;IFZLjava/util/function/Predicate;)Lnet/minecraft/world/entity/LivingEntity;",
        "preCastTargetHelper(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/magic/MagicData;Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;IFZLjava/util/function/Predicate;)Z"
    }, 
    at = @At(value = "NEW", target = "net/minecraft/network/protocol/game/ClientboundSetActionBarTextPacket"),
    remap = false, require = 0)
    private static ClientboundSetActionBarTextPacket onCastErrorTargetMessage(Component component) {
        if (TeamUtils.LAST_BLOCK_WAS_ALLY.get()) {
            TeamUtils.LAST_BLOCK_WAS_ALLY.set(false); // Reset
            return new ClientboundSetActionBarTextPacket(net.minecraft.network.chat.Component.translatable("magic_team.message.blocked").withStyle(net.minecraft.ChatFormatting.RED));
        }
        return new ClientboundSetActionBarTextPacket(component != null ? component : Component.empty());
    }

    /**
     * Versão 3.15.4+ (Chamada de método de mensagem)
     * Tentamos capturar chamadas de sendSystemMessage ou similares.
     */
    @SuppressWarnings("all")
    @Redirect(method = {
        "preCastTargetHelper(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/magic/MagicData;Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;IFZLjava/util/function/Predicate;)Lnet/minecraft/world/entity/LivingEntity;",
        "preCastTargetHelper(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/magic/MagicData;Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;IFZLjava/util/function/Predicate;)Z"
    },
    at = @At(value = "INVOKE", 
             target = "Lnet/minecraft/world/entity/player/Player;displayClientMessage(Lnet/minecraft/network/chat/Component;Z)V"),
    remap = false, require = 0)
    private static void onCastErrorTargetMessage_3154(net.minecraft.world.entity.player.Player player, Component component, boolean actionBar) {
        if (TeamUtils.LAST_BLOCK_WAS_ALLY.get()) {
            TeamUtils.LAST_BLOCK_WAS_ALLY.set(false); // Reset
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("magic_team.message.blocked").withStyle(net.minecraft.ChatFormatting.RED), true);
            return;
        }
        player.displayClientMessage(component, actionBar);
    }

    /**
     * Hook para o Raycast visual (remove o outline instantaneamente).
     */
    @SuppressWarnings("null")
    @Inject(method = "raycastForEntity(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;FZF)Lnet/minecraft/world/phys/HitResult;", 
            at = @At("RETURN"), cancellable = true)
    private static void onRaycastForEntity(Level level, Entity originEntity, float distance, boolean checkForBlocks, float bbInflation, CallbackInfoReturnable<HitResult> cir) {
        if (!MagicTeamConfig.SERVER.enableTargetingBlock.get()) return;

        HitResult hitResult = cir.getReturnValue();
        if (hitResult instanceof EntityHitResult entityHitResult) {
            Entity target = entityHitResult.getEntity();
            if (TeamUtils.areAllies(originEntity, target)) {
                Vec3 eyePos = originEntity.getEyePosition();
                Vec3 viewVec = originEntity.getViewVector(1.0F);
                if (eyePos != null && viewVec != null) {
                    Vec3 scaledView = viewVec.scale((double)distance);
                    if (scaledView != null) {
                        Vec3 missPos = eyePos.add(scaledView);
                        if (missPos != null) {
                            cir.setReturnValue(BlockHitResult.miss(missPos, Direction.UP, BlockPos.containing(missPos)));
                        }
                    }
                }
            }
        }
    }
}