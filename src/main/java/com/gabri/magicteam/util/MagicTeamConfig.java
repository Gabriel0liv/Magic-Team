package com.gabri.magicteam.util;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public final class MagicTeamConfig {
    public static final String DEFAULT_BLOCKED_MESSAGE = "{\"text\":\"Você não pode ferir um aliado.\",\"color\":\"red\"}";

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

    /** Reloads the already-open Forge server config from disk and clears cached ConfigValue reads. */
    public static boolean reloadServerConfig() {
        ModConfig config = serverConfig;
        if (config == null || !(config.getConfigData() instanceof CommentedFileConfig fileConfig)) {
            return false;
        }

        fileConfig.load();
        SERVER_SPEC.acceptConfig(fileConfig);
        return true;
    }

    public static List<String> copyStrings(List<? extends String> values) {
        return values == null ? List.of() : new ArrayList<>(values);
    }

    public static final class Server {
        public final ForgeConfigSpec.BooleanValue enabled;
        public final ForgeConfigSpec.BooleanValue blockedMessageEnabled;
        public final ForgeConfigSpec.ConfigValue<String> blockedMessage;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> spellOverrides;

        Server(ForgeConfigSpec.Builder builder) {
            builder.comment("Magic-Team server config").push("magic_team");

            enabled = builder
                    .comment("Master switch. When false, Magic Team leaves gameplay interactions untouched.")
                    .define("enabled", true);

            builder.push("message");
            blockedMessageEnabled = builder
                    .comment("Show action-bar feedback to a player when Magic Team blocks their allied attack/target.")
                    .define("enabled", true);
            blockedMessage = builder
                    .comment("Blocked-action message. Accepts plain text or a vanilla text-component JSON string, like /tellraw.")
                    .define("text", DEFAULT_BLOCKED_MESSAGE);
            builder.pop();

            builder.push("spells");
            spellOverrides = builder
                    .comment("Admin overrides only. Format: namespace:spell=support or namespace:spell=hostile.",
                            "Spells not listed here use Magic Team's built-in classification.")
                    .defineList("overrides", List.of(), MagicTeamConfig::isValidOverrideEntry);
            builder.pop();

            builder.pop();
        }

        public boolean enabled() {
            return enabled.get();
        }

        public void setEnabled(boolean value) {
            enabled.set(value);
        }

        public boolean blockedMessageEnabled() {
            return blockedMessageEnabled.get();
        }

        public void setBlockedMessageEnabled(boolean value) {
            blockedMessageEnabled.set(value);
        }

        public String blockedMessage() {
            return blockedMessage.get();
        }

        public void setBlockedMessage(String value) {
            blockedMessage.set(value == null ? "" : value);
        }

        public List<String> spellOverrides() {
            return MagicTeamConfig.copyStrings(spellOverrides.get());
        }

        @SuppressWarnings("unchecked")
        public void setSpellOverrides(List<String> values) {
            ((ForgeConfigSpec.ConfigValue<List<? extends String>>) (Object) spellOverrides)
                    .set(MagicTeamConfig.copyStrings(values));
        }
    }

    private static boolean isValidOverrideEntry(Object value) {
        if (!(value instanceof String entry)) {
            return false;
        }

        int separator = entry.lastIndexOf('=');
        if (separator <= 0 || separator == entry.length() - 1) {
            return false;
        }

        String spellId = entry.substring(0, separator).trim();
        String behavior = entry.substring(separator + 1).trim();
        return spellId.contains(":") && SpellBehavior.parse(behavior) != null;
    }
}
