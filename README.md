## Overview

Magic-Team is a server-side Forge mod for Minecraft 1.20.1 that enforces team-based spell protection for Iron's Spells 'n Spellbooks. The mod is designed for servers where allies should be safe from hostile spell targeting, hostile spell effects, and spell damage while still being able to receive support spells from teammates.

Scoreboard alliance and offensive permission are separate concepts: players remain allies even when their team allows friendly fire. Hostile interactions follow the team's `friendlyFire` setting instead of redefining the alliance itself.

## How It Works

Magic-Team uses a layered protection model:

1. **Target Validation**: hostile spells respect allied friendly-fire rules before locking onto a target.
2. **Effect Validation**: hostile spell effects are filtered before they apply to protected allies.
3. **Damage Validation**: spell damage, projectiles, AOEs and supported addon paths respect the same friendly-fire decision.
4. **Team Resolution**: Babel Core resolves root owners for summons, projectiles and related entities.
5. **Admin Overrides**: every registered Iron's spell can be explicitly treated as `support` or `hostile`; spells without an override use Magic Team's built-in classification.

## Features

* **Server-Side Only**: players do not need Magic Team installed on the client.
* **Global Runtime Toggle**: disable all Magic Team gameplay filtering without removing the mod or restarting the server.
* **Team-Based Protection**: scoreboard friendly-fire permission remains authoritative for hostile interactions.
* **Spell Overrides**: admins can override any registered Iron's/addon spell as `support` or `hostile`.
* **Registry-Aware Autocomplete**: command suggestions include all spells currently registered in the Iron's spell registry, including normal addons.
* **Configurable Feedback**: the blocked-action message accepts plain text or vanilla tellraw-style JSON components.
* **Runtime Debugging**: an optional non-persistent debug mode logs friendly-fire decisions for troubleshooting.

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

`/magicteam enabled false` makes Magic Team transparent to gameplay while leaving its commands available. `/magicteam reload` rereads the Forge server config from disk. Debug mode intentionally resets after a server restart.

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
* **Dependencies**: Iron's Spells 'n Spellbooks and Babel Core
* **Side**: Server-side

Magic Team 2.3.3 also contains explicit compatibility work for the audited Travel Optics, GTBC Geomancy Plus and Alshanex's Familiars hostile paths. Addons that register normal `AbstractSpell` entries automatically appear in the spell command autocomplete even when no special gameplay adapter is required.

## Why This Mod?

This mod is useful when you want team-based combat rules without relying on manual moderation during every fight.

It helps with:

* Preventing teammates from accidentally debuffing each other
* Keeping allied spell support reliable
* Stopping hostile spell damage when scoreboard friendly fire is disabled
* Allowing hostile allied combat when scoreboard friendly fire is enabled
* Preserving summon, owner and ally identity independently from offensive permission

## License

Magic-Team is distributed under a proprietary license. See `LICENSE.txt` for the full terms.

## Credits

**Author**: [SatDPhoe](https://x.com/SatPhoe)

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/satdphoe)
