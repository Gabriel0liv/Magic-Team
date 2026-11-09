package com.gabri.magicteam.util;

public enum SpellBehavior {
    SUPPORT,
    HOSTILE;

    public static SpellBehavior parse(String value) {
        if (value == null) {
            return null;
        }

        return switch (value.trim().toLowerCase()) {
            case "support" -> SUPPORT;
            case "hostile" -> HOSTILE;
            default -> null;
        };
    }

    public String configName() {
        return name().toLowerCase();
    }
}
