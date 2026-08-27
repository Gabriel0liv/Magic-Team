package com.gabri.magicteam.mixin;

import com.gabri.magicteam.util.TeamUtils;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.entity.spells.root.PreventDismount;
import io.redspace.ironsspellbooks.network.casting.SyncTargetingDataPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.entity.PartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Mixin(value = Utils.class, remap = false)
public class UtilsMixin {

    @Inject(
            method = "preCastTargetHelper(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/magic/MagicData;Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;IFZLjava/util/function/Predicate;)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void onPreCastTargetHelper(Level level, LivingEntity caster, MagicData playerMagicData, AbstractSpell spell, int range, float aimAssist, boolean sendFailureMessage, Predicate<LivingEntity> filter, CallbackInfoReturnable<Boolean> cir) {
        if (!TeamUtils.isEnabled()) {
            return;
        }

        if (level == null || caster == null || playerMagicData == null || spell == null) {
            return;
        }

        HitResult target = Utils.raycastForEntity(level, caster, range, true, aimAssist);
        LivingEntity livingTarget = null;

        if (target instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            if (entity instanceof LivingEntity livingEntity && (filter == null || filter.test(livingEntity))) {
                livingTarget = livingEntity;
            } else if (entity instanceof PartEntity<?> partEntity &&
                    partEntity.getParent() instanceof LivingEntity livingParent &&
                    !caster.equals(livingParent) &&
                    (filter == null || filter.test(livingParent))) {
                livingTarget = livingParent;
            } else if (entity instanceof PreventDismount && entity.getFirstPassenger() instanceof LivingEntity livingRooted) {
                livingTarget = livingRooted;
            }
        }

        if (livingTarget != null && !TeamUtils.shouldAllowTarget(caster, livingTarget, spell)) {
            if (sendFailureMessage && caster instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket(
                        Component.translatable("ui.irons_spellbooks.cast_error_target").withStyle(ChatFormatting.RED)
                ));
            }
            TeamUtils.sendBlockedMessage(caster);
            cir.setReturnValue(false);
            return;
        }

        if (livingTarget != null) {
            playerMagicData.setAdditionalCastData(new TargetEntityCastData(livingTarget));
            if (caster instanceof ServerPlayer serverPlayer) {
                if (spell.getCastType() != CastType.INSTANT) {
                    PacketDistributor.sendToPlayer(serverPlayer, new SyncTargetingDataPacket(livingTarget, spell));
                }
                serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket(
                        Component.translatable("ui.irons_spellbooks.spell_target_success", livingTarget.getDisplayName().getString(), spell.getDisplayName(serverPlayer))
                                .withStyle(ChatFormatting.GREEN)
                ));
            }
            if (livingTarget instanceof ServerPlayer serverPlayer) {
                Utils.sendTargetedNotification(serverPlayer, caster, spell);
            }
            cir.setReturnValue(true);
            return;
        }

        if (sendFailureMessage && caster instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket(
                    Component.translatable("ui.irons_spellbooks.cast_error_target").withStyle(ChatFormatting.RED)
            ));
        }
        cir.setReturnValue(false);
    }

    @Inject(
            method = "shouldHealEntity(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void onShouldHealEntity(Entity healer, Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (!TeamUtils.isEnabled()) {
            return;
        }

        if (!TeamUtils.shouldAllowHealing(healer, target)) {
            cir.setReturnValue(false);
        }
    }
}
