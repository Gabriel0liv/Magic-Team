# Alshanex's Familiars v3.8 HOTFIX — Friendly-Fire Audit

Date: 2026-08-27

Audited binary/source set:

- `MineDev/alteracao_mods/alshanex_familiars-1.20.1_v3.8_HOTFIX`
- Minecraft/Forge target: 1.20.1
- Magic Team branch: `agent/friendly-fire-compat-2.3.3`

## Invariant

Friendly fire is an offensive permission, not a relationship rewrite:

```text
friendlyFire=true != areAllies=false
```

Therefore:

- hostile damage/effects/control must use `TeamUtils.shouldBlockFriendlyFire(...)`;
- support/healing/owner checks stay relationship based;
- summon AI must not start targeting its owner/team merely because scoreboard friendly fire is enabled;
- `Entity.isAlliedTo` is never globally rewritten to encode friendly-fire permission.

## Exhaustive binary scan

Every `.class` under the exact v3.8 HOTFIX addon package was scanned for hostile primitives and relationship-sensitive sinks.

```text
TOTAL_CLASSES=278
CANDIDATE_CLASSES=38
```

The scan looked for direct `hurt`, Iron's `DamageSources.applyDamage`, `DamageSources.isFriendlyFireBetween`, `addEffect`, `isAlliedTo`, fire setters, forced movement, vanilla explosions, and invulnerability-time writes.

All 38 candidates were manually classified after the scan.

## Confirmed bypasses and adapters

### HikenEntity

Problem: two direct `isAlliedTo` hostile prefilters prevented teammate hits even when scoreboard friendly fire was enabled. Hiken also performs direct hostile side effects.

Adapter:

- `compat.familiars.HikenFriendlyFireMixin`

Policy: replace only the hostile prefilters with `TeamUtils.shouldBlockFriendlyFire(owner, target)` semantics. Relationship/owner AI remains unchanged.

### IllusionistDecoy

Problem: detonation mixes central `DamageSources.applyDamage` with a vanilla `Explosion`; explosion damage can bypass the central helper.

Adapter:

- `compat.familiars.IllusionistDecoyContextMixin`

Policy: execute the synchronous detonation under `InteractionType.HARMFUL`, using the real decoy owner as context source when available.

### EndStoneEntity / PurpurPilarEntity / PurpurBricksEntity / ChorusFlowerEntity

Problem: addon side effects occur after the Iron's projectile superclass impact. Protected teammates could still receive knockback; Chorus Flower could additionally reset invulnerability and teleport the target.

Adapter:

- `compat.familiars.MayhemDirectHitFriendlyFireMixin`

Policy: allow the superclass impact hook to run, then gate addon-specific side effects. Protected impacts still consume the projectile.

### DragonEggEntity

Problem: direct damage uses Iron's central helper, but the entity writes the target's invulnerability time independently. A protected teammate could have invulnerability reset even when damage was blocked.

Adapter:

- `compat.familiars.DragonEggFriendlyFireMixin`

Policy: guard the direct `f_19802_`/`invulnerableTime` write with offensive friendly-fire policy.

### LullabySpell

Problem: the SLEEPY target predicate uses `caster.isAlliedTo(target)`, making scoreboard FF=true ineffective.

Adapter:

- `compat.familiars.LullabyFriendlyFireMixin`

Policy: patch only `lambda$applySleepy$0`; do not change global alliance semantics.

### SonataSpell

Problem: the GUIDING_BOLT target predicate uses `caster.isAlliedTo(target)`, making scoreboard FF=true ineffective.

Adapter:

- `compat.familiars.SonataFriendlyFireMixin`

Policy: patch only `lambda$shootNotes$0`.

### HarpExplosionEntity

Problem: at the damage tick it calls `DamageSources.applyDamage(...)` but ignores the returned boolean, then applies forced movement and three effects anyway. The same tick may create a vanilla `Explosion` with a spell damage source.

Adapter:

- `compat.familiars.HarpExplosionFriendlyFireMixin`

Policy:

1. filter protected teammates from the custom target list before damage, movement, and effects;
2. run the synchronous vanilla explosion under `InteractionType.HARMFUL` with guaranteed context cleanup.

### ServerEvents retaliation

Problem: `onDamageTaken` contains secondary retaliation sinks:

- Scorcher retaliation directly calls `setRemainingFireTicks(100)` on the attacker;
- Plague retaliation spawns a splash potion toward the attacker.

Normal Magic Team spell paths already reject protected damage before these sinks can be reached, but relying on that alone leaves behavior dependent on the external damage producer/event path.

Adapter:

- `compat.familiars.ServerEventsRetaliationFriendlyFireMixin`

Policy: independently guard both retaliation sinks with `TeamUtils.shouldBlockFriendlyFire(victimPet, attacker)`. FF=true preserves original retaliation; FF=false suppresses it.

## Candidates confirmed safe / centrally covered

### EndMayhemEffect

Its nearby-target predicate already calls `DamageSources.isFriendlyFireBetween(candidate, caster)` before creating portals/projectiles. No adapter required.

### MegidoEntity / MegidoSpell

Initial targeting uses Iron's target helper and chained entity damage uses central `DamageSources`/friendly-fire helpers. No addon-specific bypass found.

### BlackNoteEntity / DefaultNoteEntity / MusicBolt

Each delayed projectile preserves its owner, calls `DamageSources.applyDamage(...)`, and only applies `GUIDING_BOLT` when that call returns success. Protected damage therefore also suppresses the debuff. No adapter required.

### VibrationSpell

The spell calls `DamageSources.applyDamage(...)` and then adds two hostile effects without checking its boolean return. This is nevertheless covered because Magic Team wraps the complete addon `onCast` override in explicit `InteractionType.HARMFUL` context via `AbstractSpellMixin`. The later `addEffect` calls are therefore filtered by `LivingEntityMixin` using the same FF policy.

No Vibration-specific adapter is required.

### IceAgeSpell / IceChamberSpell

Both use `DamageSources.isFriendlyFireBetween(...)` to reject protected targets before their persistent effects. No adapter required.

### FrozenEntity

Direct `hurt` calls are self/destruction behavior. Its offensive projectile is an Iron's projectile and remains covered by the existing central/projectile compatibility layer. No Familiars-specific relation rewrite required.

### DragonCircleEntity

Extends Iron's `AoeEntity`; covered by the Iron's AOE friendly-fire adapter/context. No duplicate Familiars patch required.

### EndMayhemSpell

Applies the Mayhem effect to the caster itself. The later hostile target selection is performed by `EndMayhemEffect`, which already uses Iron's central friendly-fire helper.

## Relationship/support candidates deliberately left unchanged

These are intentionally not converted to FF permission checks.

### ShadowEntity

`isAlliedTo` behavior is owner/summon relationship logic. Summon AI and ownership remain allied regardless of scoreboard FF permission.

### BlueBirdEntity / BuffAllyGoal

Healing/buff selection is support relation logic. It must continue using ally identity, not offensive FF permission.

### HealingCircleEntity

Its predicate uses `Utils.shouldHealEntity(owner, target)` or owner identity and applies healing/defensive effects. Support-only.

### PlaguePetEntity

Direct effects found in the pet class are self-protection/mobility effects. Retaliatory behavior lives in `ServerEvents` and is guarded separately.

### ScorcherPetEntity

Direct effects found in the pet class are self buffs. Retaliatory fire lives in `ServerEvents` and is guarded separately.

### DragonWarriorPetEntity

Effects are self Resistance/Regeneration. No hostile teammate path.

### IllusionistPetEntity / DecoyGoal

Effects are self invisibility/control setup. The spawned decoy's hostile detonation is covered by `IllusionistDecoyContextMixin`.

### SummonerPetEntity

Effects target the summoner as support/cleanup behavior. No offensive FF conversion.

### ClericPetEntity

The direct external-effect case is zombie-villager curing utility; the rest is healing/support. Not a teammate-combat hostile path.

### SoulEntity

Effect application is self invisibility.

### EvasionCurio / InvisibilityCurio / StatueCurio

Each selects owned/allied familiars and applies defensive buffs. Their `isAlliedTo` checks are support relationship semantics and must remain unchanged.

### UndeadChaosGoal

The goal uses ally identity while assigning AI targets. It is deliberately relationship-based. Changing it to scoreboard FF permission would make friendly summons/controlled undead target their own team when FF=true, violating the architecture invariant.

### ServerEvents non-retaliation paths

- Bard Harp pet conversion is ownership/utility behavior.
- Enderman teleport cancellation near Dragon Warrior is world-control utility.
- pet blocking/enraged self effects are incoming-defense/self behavior.

No offensive FF rewrite is appropriate for those paths.

## Spell-level binary coverage

Exact bytecode was inspected for all addon spell classes in the HOTFIX package:

- `AngelSpell`
- `BirdsSpell`
- `DefaultNoteSpell`
- `EndMayhemSpell`
- `ExplosionMelodySpell`
- `HikenSpell`
- `IceAgeSpell`
- `IceChamberSpell`
- `LullabySpell`
- `MegidoSpell`
- `MusicBoltSpell`
- `ShadowSummonSpell`
- `SonataSpell`
- `VibrationSpell`

Delayed sound entities were also inspected directly:

- `BlackNoteEntity`
- `DefaultNoteEntity`
- `HarpExplosionEntity`
- `MusicBolt`

## Structural verification

The Familiars compatibility contract enforces registration and structural invariants for the adapters above.

For each new bypass, implementation followed RED -> GREEN contract development before production code was accepted.

Runtime gameplay verification is still required before the overall Magic Team 2.3.3 release is considered complete. In particular, runtime tests must verify both scoreboard states and ensure support/summon AI remains unchanged.
