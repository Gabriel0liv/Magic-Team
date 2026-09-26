## Overview

Magic-Team is a server-side Forge mod for Minecraft 1.20.1 that enforces team-based spell protection for Iron's Spells 'n Spellbooks. The mod is designed for servers where allies should be safe from hostile spell targeting, hostile spell effects, and spell damage while still being able to receive support spells from teammates.

Scoreboard alliance and vanilla friendly fire are separate from Magic Team's spell policy. Vanilla `/team friendlyFire` continues to govern normal Minecraft combat, while Magic Team decides whether hostile magic may affect allies when Magic Team is enabled.

## How It Works

Magic-Team uses a global-first layered protection model:

1. **Cast/Target Context**: normal Iron's casts and standard `TargetEntityCastData` selection are handled centrally, including addon overrides that use the Iron's spell API.
2. **Persistent Attribution**: entities, projectiles, AOEs and delayed MobEffects created by a spell can retain caster/spell behavior after the original cast stack ends.
3. **Effect Validation**: hostile spell effects are filtered before they apply to protected allies, including delayed attributed effects.
4. **Damage Validation**: Iron's `SpellDamageSource` is preferred when available; attributed custom/delayed entities fall back to the same global policy.
5. **Common Side Effects**: attributed hostile magic also gates shared forced-movement, fire and effect-removal paths without naming individual spells.
6. **Team Resolution**: Babel Core resolves root owners for summons, projectiles and related entities.
7. **Admin Overrides**: every registered Iron's spell can be explicitly treated as `support` or `hostile`; spells without an override use Magic Team's built-in classification.

## Features

* **Server-Side Only**: players do not need Magic Team or Babel Core installed on the client when no separate client-side Babel consumer is present.
* **Global Runtime Toggle**: disable all Magic Team gameplay filtering without removing the mod or restarting the server.
* **Magic-Only Ally Protection**: hostile magic is blocked against allies while Magic Team is enabled, independently from vanilla `/team friendlyFire`.
* **Global-First Addon Coverage**: addons that follow normal Iron's cast/projectile/AOE/effect paths usually require no spell-specific Magic Team adapter.
* **Delayed Interaction Attribution**: custom entities and MobEffects can preserve the original caster/spell classification across later ticks.
* **Spell Overrides**: admins can override any registered Iron's/addon spell as `support` or `hostile`.
* **Registry-Aware Autocomplete**: command suggestions include all spells currently registered in the Iron's spell registry, including normal addons.
* **Configurable Feedback**: the blocked-action message accepts plain text or vanilla tellraw-style JSON components.
* **Target Notification Control**: the Iron's targeted-player notification can be enabled or disabled server-side.
* **Runtime Debugging**: an optional non-persistent debug mode logs protection decisions for troubleshooting.

## Configuration

The Forge server config stores only server policy and explicit admin overrides:

```toml
[magic_team]
enabled = true

[magic_team.message]
enabled = true
text = '{"text":"Você não pode ferir um aliado.","color":"red"}'

[magic_team.spells]
overrides = ["examplemod:some_spell=support", "examplemod:other_spell=hostile"]
```

Spells that are not present in `overrides` use Magic Team's built-in classification. The old beneficial/harmful administration lists are no longer used.

## Commands

All commands require operator permission level 2. Changes that belong to the server config are saved immediately.

```text
/magicteam enabled <true|false>
/magicteam status
/magicteam reload
/magicteam debug <true|false>
/magicteam targetnotification <true|false>

/magicteam message enabled <true|false>
/magicteam message set <plain text or JSON component>
/magicteam message reset

/magicteam spell info <spell>
/magicteam spell set <spell> support
/magicteam spell set <spell> hostile
/magicteam spell reset <spell>
/magicteam spell overrides
/magicteam spell list [namespace]
```

`/magicteam enabled false` makes Magic Team transparent to spell gameplay while leaving its commands available. `/magicteam reload` rereads the Forge server config from disk. Debug mode intentionally resets after a server restart.

`spell set` creates an explicit override. `spell reset` removes it and returns the spell to Magic Team's built-in behavior. Full registry IDs are stored in the config; short spell paths are accepted only when they resolve unambiguously.

The message command accepts either ordinary text:

```text
/magicteam message set Você não pode ferir um aliado!
```

or a vanilla text component:

```text
/magicteam message set {"text":"Você não pode ferir um aliado!","color":"red","bold":true}
```

Malformed JSON is rejected instead of being saved.

## Compatibility

* **Minecraft Version**: 1.20.1
* **Mod Loader**: Forge 47.4.x+
* **Server Dependencies**: Iron's Spells 'n Spellbooks and Babel Core
* **Side**: Server-side

Magic Team 2.4.0 uses the shared Iron's/global runtime pipeline as its primary addon-compatibility model. Normal addon spells should not require one Magic Team mixin per spell: cast context, selected targets, spell damage, projectiles, AOEs, delayed entities, delayed MobEffects and common hostile side effects are handled by shared hooks whenever enough attribution exists.

A small optional compatibility layer remains only for mechanisms outside that pipeline, such as item/armor abilities, Forge-event retaliation and transactions where blocking final damage would still leave another offensive side effect. Those configs are `required:false` with zero required injector matches. A missing/changed optional injection point therefore does not become a fatal 0/1 match failure. Prepare-time adapter failures can be skipped with a warning; apply-time transformation failures preserve Mixin's own safety action instead of forcing the server to continue with a potentially partially transformed class.

The previous fragile per-spell redirects, including the Orbital Void redirect that depended on a specific addon-internal `isAlliedTo` call, are no longer part of the normal architecture. Addons that register normal `AbstractSpell` entries continue to appear in spell command autocomplete without needing a dedicated gameplay adapter.

## Why This Mod?

This mod is useful when you want team-based spell protection without tying that policy to normal Minecraft combat.

It helps with:

* Preventing teammates from accidentally receiving hostile magic
* Keeping allied spell support reliable
* Keeping vanilla `/team friendlyFire` responsible for normal combat
* Blocking hostile allied magic independently while Magic Team is enabled
* Preserving summon, owner and ally identity independently from offensive permission

## License

Magic-Team is distributed under a proprietary license. See `LICENSE.txt` for the full terms.

## Credits

**Author**: [SatDPhoe](https://x.com/SatPhoe)

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/satdphoe)
