package com.gabri.magicteam.util;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public final class MagicTeamConfig {
    public static final Server SERVER;
    public static final ForgeConfigSpec SERVER_SPEC;

    private static volatile ModConfig serverConfig;

    static {
        Pair<Server, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Server::new);
        SERVER = pair.getLeft();
        SERVER_SPEC = pair.getRight();
    }

    private MagicTeamConfig() {
    }

    public static void onConfigEvent(ModConfigEvent event) {
        ModConfig config = event.getConfig();
        if (config != null && config.getSpec() == SERVER_SPEC) {
            serverConfig = config;
        }
    }

    public static void saveServerConfig() {
        ModConfig config = serverConfig;
        if (config != null) {
            config.save();
        }
    }

    public static List<String> copyStrings(List<? extends String> values) {
        return values == null ? List.of() : new ArrayList<>(values);
    }

    public static final class Server {
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> beneficialSpells;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> harmfulSpells;

        Server(ForgeConfigSpec.Builder builder) {
            builder.comment("Magic-Team server config").push("magic_team");
            builder.push("spells");
            beneficialSpells = builder
                    .comment("Spells that should be treated as beneficial on allies and allowed for target selection.",
                            "Use full IDs like `irons_spellbooks:heal` or short paths like `heal`.")
                    .defineList("beneficialSpells", List.of(
                            "fortify",
                            "haste",
                            "cloud_of_regeneration",
                            "cleanse",
                            "blessing_of_life",
                            "healing_circle",
                            "wisp"
                    ), value -> value instanceof String);

            harmfulSpells = builder
                    .comment("Spells that should be treated as harmful on allies and blocked for target selection.",
                            "Use full IDs like `irons_spellbooks:root` or short paths like `root`.")
                    .defineList("harmfulSpells", List.of(
                            "slow",
                            "blight",
                            "root",
                            "heat_surge",
                            "poison_splash",
                            "acid_spit"
                    ), value -> value instanceof String);
            builder.pop();
            builder.pop();
        }

        public List<String> beneficialSpells() {
            return MagicTeamConfig.copyStrings(beneficialSpells.get());
        }

        public List<String> harmfulSpells() {
            return MagicTeamConfig.copyStrings(harmfulSpells.get());
        }

        @SuppressWarnings("unchecked")
        public void setBeneficialSpells(List<String> values) {
            ((ForgeConfigSpec.ConfigValue<List<? extends String>>) (Object) beneficialSpells).set(MagicTeamConfig.copyStrings(values));
        }

        @SuppressWarnings("unchecked")
        public void setHarmfulSpells(List<String> values) {
            ((ForgeConfigSpec.ConfigValue<List<? extends String>>) (Object) harmfulSpells).set(MagicTeamConfig.copyStrings(values));
        }
    }
}
