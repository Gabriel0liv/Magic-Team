package com.gabri.magicteam.mixin.compat.familiars;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Dragon Egg uses Iron's DamageSources for damage, but unconditionally resets
 * the directly-hit entity's invulnerability time afterwards. Keep its AOE and
 * Dragon Circle behavior intact while suppressing only that direct side effect
 * for protected teammates.
 */
@Pseudo
@Mixin(targets = "net.alshanex.alshanex_familiars.entity.misc.DragonEggEntity", remap = false)
public abstract class DragonEggFriendlyFireMixin extends Projectile {

    protected DragonEggFriendlyFireMixin(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
    }

    @Redirect(
            method = "m_5790_(Lnet/minecraft/world/phys/EntityHitResult;)V",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/entity/Entity;f_19802_:I",
                    opcode = Opcodes.PUTFIELD,
                    remap = false
            ),
            remap = false
    )
    private void magicTeam$gateInvulnerabilityReset(Entity target, int value) {
        Entity owner = getOwner();
        if (owner == null || target == null || !TeamUtils.shouldBlockFriendlyFire(owner, target)) {
            target.invulnerableTime = value;
        }
    }
}
