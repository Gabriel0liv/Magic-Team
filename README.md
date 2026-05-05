## Overview

Magic-Team is a server-side Forge mod for Minecraft 1.20.1 that enforces team-based spell protection for Iron's Spells 'n Spellbooks. The mod is designed for servers where allies should be safe from hostile spell targeting, hostile spell effects, and spell damage, while still being able to receive helpful spells from teammates.

This mod is meant for SMPs, modpacks, and private servers where combat groups need a simple rule set: allies should cooperate without accidental friendly fire, and non-allies should not receive positive support spells from enemy casters.

## The Problem

In spell-heavy servers, team rules alone are not always enough. Many magic systems allow spells to:

1. Target a living entity directly
2. Apply a beneficial or harmful status effect
3. Deal magic damage without using normal melee combat rules

Without extra protection, players can accidentally or intentionally:

1. Cast debuffs on teammates during combat
2. Hit allies with spell damage
3. Apply buffs to enemy players who should not receive support
4. Bypass the intended meaning of teams by using spells instead of weapons

Magic-Team closes those gaps by checking team relationships at runtime.

## How It Works

Magic-Team uses a layered protection model:

1. **Target Validation**: Before a spell locks onto a living target, the mod checks whether the spell is classified as beneficial or harmful.
2. **Effect Validation**: When a spell tries to apply a status effect, the mod checks the source and the target before allowing it through.
3. **Damage Validation**: When spell damage is applied, the mod blocks hostile magical damage between allies.
4. **Team Resolution**: The mod resolves owners for summons, projectiles, and related entities so team checks still work in combat chains.

The result is simple: beneficial spells can still help allies, harmful spells are blocked from targeting allies, and spell damage is prevented between allied players.

## Features

* **Server-Side Only**: No client installation is required for players
* **Team-Based Protection**: Ally checks are the core rule used at runtime
* **Target Blocking**: Harmful spells cannot lock onto allied targets
* **Effect Blocking**: Harmful spell effects are stopped before they apply to allies
* **Damage Blocking**: Allied spell damage is prevented
* **Configurable Spell Lists**: Beneficial and harmful spell lists can be edited from commands
* **Registry-Aware Autocomplete**: Spell suggestions come from the Iron's Spells registry

## Configuration

The mod stores its server configuration in the Forge server config file under the `magic_team` section. The following options are available:

```toml
[magic_team.spells]
beneficialSpells = ["fortify", "haste", "cloud_of_regeneration", "cleanse", "blessing_of_life", "healing_circle", "wisp"]
harmfulSpells = ["slow", "blight", "root", "heat_surge", "poison_splash", "acid_spit"]
```

* Beneficial spells are treated as safe for allied targeting.
* Harmful spells are blocked from allied targeting.
* The lists accept either full IDs like `irons_spellbooks:root` or short paths like `root`.

## Commands

All commands require operator permission level 2.

### Save and Reload

```text
/magicteam save
/magicteam reload
```

Saves the current server config or reloads the current config state.

### Spell Filters

```text
/magicteam filter view beneficial
/magicteam filter view harmful
/magicteam filter add beneficial <spell>
/magicteam filter add harmful <spell>
/magicteam filter remove beneficial <spell>
/magicteam filter remove harmful <spell>
```

These commands manage the beneficial and harmful spell lists used for target blocking.

## Compatibility

* **Minecraft Version**: 1.20.1
* **Mod Loader**: Forge 47.4.x+
* **Dependency**: Iron's Spells 'n Spellbooks
* **Side**: Server-side

Addon compatibility can vary. Magic-Team works best when an addon uses standard magic system. Some addons apply damage or debuffs through custom entities, delayed explosions, or non-standard hooks, and those cases may need extra integration or may not be fully covered.

## Why This Mod?

This mod is useful when you want team-based combat rules without relying on manual moderation during every fight.

It helps with:

* Preventing teammates from accidentally debuffing each other
* Keeping allied spell support reliable
* Stopping hostile spell damage between allies
* Preserving clear team roles in PvP and cooperative PvE

If two players are not on the same team, the mod does not treat them as protected allies.

## Credits

**Author**: [SatDPhoe](https://x.com/SatPhoe)

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/satdphoe)
