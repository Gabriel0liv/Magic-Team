package com.gabri.magicteam;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import java.util.List;
import java.util.Arrays;

public class MagicTeamConfig {
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final ServerConfig SERVER;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        SERVER = new ServerConfig(builder);
        SERVER_SPEC = builder.build();
    }

    /**
     * Registra a configuração do servidor. 
     * Nota: ModLoadingContext.get() pode reportar aviso de depreciação em ambientes 1.21+, 
     * mas é o método padrão e necessário para 1.20.1.
     */
    @SuppressWarnings("removal")
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SERVER_SPEC);
    }

    public static class ServerConfig {
        public final ForgeConfigSpec.BooleanValue enableGlobalAlliance;

        public final ForgeConfigSpec.ConfigValue<List<? extends String>> beneficialSpells;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> explicitHarmfulSpells;

        public ServerConfig(ForgeConfigSpec.Builder builder) {
            builder.push("protection");
            enableGlobalAlliance = builder
                    .comment("Ativa a lógica de proteção global entre aliados. Se desativado, o PvP é livre.")
                    .define("enableGlobalAlliance", true);
            builder.pop();

            builder.push("spells");
            beneficialSpells = builder
                    .comment("Lista de IDs de magias (ou partes do nome) que são SEMPRE consideradas benéficas/suporte.",
                             "Funciona para o Iron's Spells e qualquer Addon (Geomancy, Familiars, etc).",
                             "Se o ID da magia contiver qualquer uma destas palavras, será permitida em aliados.")
                    .defineList("beneficialSpells", Arrays.asList(
                            "heal", "haste", "fortify", "oakskin", "angel", 
                            "cleanse", "blessing", "ward", "regeneration", 
                            "invisibility", "spider_aspect", "ascension", 
                            "planar_sight", "evasion", "slow_fall", "charge", "teleport"
                    ), obj -> obj instanceof String);

            explicitHarmfulSpells = builder
                    .comment("Lista de IDs de magias que NUNCA devem ser usadas em aliados,",
                             "mesmo que pertençam a uma escola de magia de cura (como a HOLY).")
                    .defineList("explicitHarmfulSpells", Arrays.asList(
                            "smite", "sunbeam", "guiding_bolt", "wisp"
                    ), obj -> obj instanceof String);
            builder.pop();
        }
    }
}
