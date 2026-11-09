package com.gabri.magicteam.util;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Map;

/** Dependency-free contract for detecting optional adapters that injected nothing. */
public final class OptionalAdapterDiagnosticsContractTest {
    private static final String MIXIN = "com.gabri.magicteam.mixin.compat.traveloptics.TectonicCrestFriendlyFireMixin";
    private static final String TARGET = "com.gametechbc.traveloptics.item.armor.TectonicCrestArmorItem";

    private OptionalAdapterDiagnosticsContractTest() {
    }

    public static void main(String[] args) {
        ClassNode injected = target(true);
        check(OptionalAdapterDiagnostics.countHandlerCalls(injected) == 2,
                "HEAD and RETURN calls into merged magicTeam$ handlers must be counted");

        ClassNode inert = target(false);
        check(OptionalAdapterDiagnostics.countHandlerCalls(inert) == 0,
                "a merged handler that nothing calls means the adapter is inert");

        OptionalAdapterDiagnostics.recordApplied(TARGET, inert, MIXIN);
        Map<String, Integer> results = OptionalAdapterDiagnostics.snapshotForTests();
        check(results.get("traveloptics.TectonicCrestFriendlyFireMixin -> TectonicCrestArmorItem") == 0,
                "inert adapters must be recorded with zero injected calls");

        check("traveloptics".equals(OptionalAdapterDiagnostics.modIdFor("traveloptics.X -> Y")), "Travel Optics mod id");
        check("cataclysm".equals(OptionalAdapterDiagnostics.modIdFor("cataclysm.X -> Y")), "Cataclysm mod id");
        check("gtbcs_geomancy_plus".equals(OptionalAdapterDiagnostics.modIdFor("geomancyplus.X -> Y")),
                "Geomancy Plus mod id");
        check("alshanex_familiars".equals(OptionalAdapterDiagnostics.modIdFor("familiars.X -> Y")), "Familiars mod id");

        // Must not throw whether or not the addon is installed.
        OptionalAdapterDiagnostics.logSummary(modId -> false);
        OptionalAdapterDiagnostics.logSummary(modId -> true);
    }

    /** Builds a target class with merged handlers, optionally called from the ability method. */
    private static ClassNode target(boolean injected) {
        ClassNode node = new ClassNode();
        node.name = TARGET.replace('.', '/');

        MethodNode ability = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                "triggerTectonicShockwave", "(Lnet/minecraft/world/entity/player/Player;F)V", null, null);
        if (injected) {
            ability.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, node.name,
                    "handler$zza000$magicTeam$beginAbility", "()V", false));
        }
        ability.instructions.add(new InsnNode(Opcodes.NOP));
        if (injected) {
            ability.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, node.name,
                    "handler$zza001$magicTeam$endAbility", "()V", false));
        }
        ability.instructions.add(new InsnNode(Opcodes.RETURN));
        node.methods.add(ability);

        for (String handler : new String[]{"handler$zza000$magicTeam$beginAbility", "handler$zza001$magicTeam$endAbility"}) {
            MethodNode merged = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, handler, "()V", null, null);
            merged.instructions.add(new InsnNode(Opcodes.RETURN));
            node.methods.add(merged);
        }
        return node;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
