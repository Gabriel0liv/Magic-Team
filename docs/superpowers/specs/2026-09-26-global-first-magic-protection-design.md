# Global-First Magic Protection Design

Date: 2026-09-26
Branch: `agent/friendly-fire-compat-2.3.3`

## Intent

Magic Team should protect allies from hostile magic primarily through global Iron's Spellbooks and Minecraft interception points, not through one mixin per spell or addon implementation detail.

The previous global-first architecture was already reliable for most real server behavior. The current branch improved classification, context propagation, commands and addon coverage, but accumulated many spell-specific adapters whose injection targets are coupled to addon internals and therefore become fragile across updates.

The goal of this refactor is to keep the stronger global behavior while reducing spell-specific compatibility code to rare, technically justified exceptions.

## Success Criteria

1. New normal Iron's-compatible spells should receive Magic Team protection without Magic Team code changes.
2. Updating Iron's or an addon should usually require validating a small number of shared integration points instead of dozens of spell-specific mixins.
3. Hostile magic against allies remains blocked while support magic remains allowed according to Magic Team's classification and explicit overrides.
4. Vanilla `/team friendlyFire` remains independent from Magic Team's magic-only policy.
5. Summons, projectiles, AOEs and delayed spell entities resolve their effective caster/root owner through Babel Core where possible.
6. Delayed damage/effects should retain enough magic attribution to apply the same global policy after the original cast call stack has ended.
7. Existing adapters are removed only after equivalent global coverage is demonstrated.
8. A remaining optional compatibility adapter must be fail-soft and must not crash the server because an addon changed an internal method or call site.
9. GitHub Actions is not part of the implementation-validation workflow unless explicitly requested by the user.

## Non-Goals

- Do not intercept all arbitrary Minecraft damage/effects and guess that they are magic.
- Do not preserve spell-specific adapters merely because they already exist.
- Do not require explicit Magic Team support for every addon spell registered through normal Iron's APIs.
- Do not make vanilla combat obey Magic Team's magic protection policy.
- Do not remove an adapter before its behavior is covered by a shared mechanism or intentionally accepted as an unsupported edge case.

## Current Architecture Findings

The repository already contains the core of a global solution:

- `AbstractSpellMixin` establishes spell context around Iron's cast dispatch and fallback spell hooks.
- `DamageSourcesMixin` intercepts Iron's own friendly-fire and `SpellDamageSource` paths.
- `LivingEntityMixin` provides common gates for `hurt(...)` and `addEffect(...)` while a magic context is active.
- `AbstractMagicProjectileMixin` establishes context during Iron's projectile hit detection.
- `AoeEntityMixin` establishes context during Iron's AOE hit processing.
- `MagicTeamEffectContext` already carries source, spell, cast source, origin and harmful/beneficial interaction type.
- `TeamUtils` centralizes spell classification, explicit overrides, Babel root-owner resolution and ally policy.

The large addon-specific compatibility layer mainly compensates for a smaller set of recurring mechanism-level gaps:

1. custom target validation;
2. custom projectiles/entities outside Iron's base classes;
3. delayed effects after the original context ends;
4. custom AOEs outside Iron's base AOE implementation;
5. hostile side effects that are neither damage nor `MobEffect` application;
6. lost caster/spell attribution after spawning intermediate entities.

These gaps should be addressed by shared mechanisms before retaining spell-specific adapters.

## Chosen Architecture

### 1. Global Cast Context Remains the Primary Source of Truth

`AbstractSpell` dispatch remains the first layer.

When a spell enters server-side execution, Magic Team records:

- effective source/caster;
- spell identity;
- cast source;
- spell behavior (`HOSTILE` or `SUPPORT`);
- interaction type (`HARMFUL`, `BENEFICIAL`, or generic only where classification is unavailable).

This transient context is used for synchronous targeting, damage, effects and entity creation.

### 2. Persistent Magic Attribution for Delayed Interactions

The current `ThreadLocal` context is insufficient once a spawned entity or delayed action survives beyond the cast call stack.

Introduce a general-purpose attribution domain for magic-originated entities/interactions. Conceptually each tracked entity can carry or resolve:

- root caster/source identity;
- spell id where known;
- spell behavior at creation time or enough information to resolve it later;
- interaction type;
- expiration/lifetime metadata where external storage is used.

The attribution mechanism must be generic and reusable. It must not be named after or designed around a particular addon spell.

Preferred resolution order for an interaction source:

1. direct active spell context;
2. persistent magic attribution on or associated with the entity;
3. Babel Core root-owner relation;
4. native damage/spell source metadata;
5. no magic classification if none of the above proves the interaction is magic.

The final step is intentionally conservative to avoid treating unrelated mod or vanilla interactions as magic.

### 3. Shared Global Enforcement Points

Magic Team should converge on a small set of shared gates.

#### Targeting

Use common Iron's targeting/validation points where they exist. A spell-specific targeting mixin is not justified if the same decision can be enforced at a shared target-selection or cast-validation boundary.

Targeting is an optimization and UX layer; final damage/effect enforcement remains authoritative so a missed targeting hook does not automatically mean allied damage is allowed.

#### Damage

Prefer, in order:

1. Iron's `DamageSources` / `SpellDamageSource` interception;
2. magic-context-aware `LivingEntity.hurt(...)`;
3. attributed custom entity/projectile damage through the same central policy.

All damage decisions must call the same policy helpers instead of duplicating team logic in adapters.

#### Effects

`LivingEntity.addEffect(...)` remains the shared final gate while a harmful/support context or persistent attribution is available.

The global effect system must preserve:

- vanilla potion behavior;
- support spells on allies;
- explicit spell overrides;
- harmful effects blocked on protected allies.

This layer is where previous edge cases such as Rend-like effect application should be solved generically where possible.

#### AOE and Projectiles

Iron's `AoeEntity` and `AbstractMagicProjectile` remain shared context entry points.

For addon entities that do not extend these classes, prefer generic attribution at creation/ownership boundaries instead of mixins against each concrete spell/entity implementation.

#### Summons and Ownership

Babel Core root-owner resolution remains the canonical relationship mechanism for projectile/summon ownership where Babel knows the relationship.

Magic attribution supplements Babel with spell identity/classification; it does not replace Babel's ownership domain.

### 4. Generic Mechanism Adapters Before Spell Adapters

When global Iron's hooks cannot cover a behavior, compatibility should be implemented at the broadest reusable mechanism boundary available.

Examples of acceptable mechanism categories:

- custom magic projectile base class;
- custom AOE base class;
- delayed hostile entity base class;
- shared addon damage helper;
- shared addon target helper;
- shared side-effect helper;
- common entity-spawn/owner propagation path.

A mechanism adapter is preferred when one hook can cover multiple spells or entities without relying on individual spell internals.

### 5. Spell-Specific Adapters Are Last Resort

A spell-specific adapter may remain only when all of the following are true:

1. the behavior is not covered by global Iron's/Minecraft gates;
2. no reusable addon mechanism/base/helper can cover it;
3. caster or spell attribution cannot be recovered generically;
4. the missing behavior materially affects Magic Team protection;
5. the adapter is optional/fail-soft.

Each retained spell-specific adapter must have a short code comment or audit note explaining why a global/mechanism-level solution is not currently possible.

## Adapter Migration Strategy

Do not delete all adapters at once.

Each existing adapter will be classified into one of four states:

### A. Redundant

Global enforcement already covers the behavior.

Action: remove the adapter and its registration/test assumptions.

### B. Replaceable by General Attribution

The adapter exists because caster/spell context is lost across ticks/entities.

Action: implement persistent attribution, prove coverage, then remove the spell adapter.

### C. Replaceable by Mechanism Adapter

Multiple adapters hook the same type of custom projectile/AOE/helper behavior.

Action: introduce one shared mechanism-level hook and remove the duplicated spell/entity adapters it replaces.

### D. Irreducible Exception

No global or shared mechanism can safely identify and control the interaction.

Action: retain a fail-soft optional adapter with documented justification.

## Initial Audit Priorities

The migration should start with adapters that are both fragile and likely redundant.

High-priority groups:

1. spell `isValidTarget(...)` redirects such as Orbital Void;
2. direct `Entity.isAlliedTo(...)` redirects used only to replace target/friendly-fire decisions;
3. custom projectile entities whose owner/root owner is already recoverable;
4. AOE entities that can inherit or receive generic magic attribution;
5. delayed effects currently creating one-off context scopes;
6. repeated Cataclysm/Travel Optics projectile adapters that share the same damage/alliance pattern.

More unusual direct side effects such as forced movement, teleport, fire-tick changes or custom resource manipulation are audited later because they may legitimately require mechanism-level gates outside `hurt(...)` and `addEffect(...)`.

## Policy Semantics That Must Not Change

### Magic Team Enabled

- hostile magic + protected ally -> blocked;
- support magic + ally -> allowed;
- vanilla `/team friendlyFire` does not authorize hostile magic;
- self/summon/root-owner relationships continue to use Babel-aware resolution.

### Magic Team Disabled

Magic Team must be transparent to gameplay filtering and allow Iron's/addon original behavior.

### Unknown Spells

Normal registered spells without explicit overrides use Magic Team's default classification. Current policy remains hostile-by-default except known/support-overridden spells.

This allows newly installed addon spells to receive safe global behavior without new Magic Team code.

## Fail-Soft Compatibility Policy

Core/global Magic Team integration points may remain strict when their absence means the central protection model is invalid.

Optional addon compatibility must remain non-fatal:

- optional config;
- zero-match optional injectors non-fatal;
- WARN when an optional adapter degrades and Mixin can safely continue;
- no failure in one addon adapter disables unrelated core/global protection;
- generic Magic Team protection remains active even if a specific optional enhancement does not apply.

The goal of the global-first refactor is to make this optional layer much smaller over time.

## Testing Strategy

### Structural Contracts

Add/adjust local dependency-free contracts to verify architectural boundaries:

- core mixin config contains only global/essential hooks plus justified shared compatibility;
- no new spell-specific adapter can be added without explicit exception metadata/comment;
- optional adapters remain fail-soft;
- removed adapters are also removed from mixin configs;
- attribution infrastructure remains addon-neutral in names and dependencies.

### Unit/Logic Tests

Test the pure attribution/policy layer independently where possible:

- active context resolution;
- persistent entity attribution;
- expiration/cleanup;
- nested contexts;
- root-owner fallback;
- support vs hostile classification;
- disabled-mode transparency;
- no attribution -> no accidental magic classification.

### Local Build

User validation command remains local, for example:

```powershell
.\gradlew.bat clean test build --no-daemon --max-workers=1
```

GitHub Actions must not be used unless explicitly requested by the user.

### Runtime Validation

Use the user's real Forge/Arclight modpack after meaningful migration batches.

Test representative mechanisms rather than every spell:

- normal hostile direct spell;
- support spell;
- Iron's projectile;
- Iron's AOE;
- addon custom projectile;
- delayed entity damage;
- delayed harmful effect;
- summon/owner case;
- one unusual direct side effect;
- Magic Team disabled mode.

Existing historically problematic spells can be used as regression examples, but they should not define the architecture.

## Migration Safety Rules

1. Never remove an adapter solely because it looks redundant; first identify the shared enforcement path replacing it.
2. Do not weaken final damage/effect gates merely because targeting is globally filtered.
3. Do not classify arbitrary vanilla/mod damage as magic without attribution evidence.
4. Preserve Babel root-owner semantics.
5. Preserve support spell behavior and explicit admin overrides.
6. Keep migration commits grouped by mechanism so regressions can be isolated.
7. Prefer deleting code once shared coverage exists rather than leaving dormant duplicate protection.

## Expected End State

The final Magic Team architecture should contain:

- a small strict global mixin set around Iron's shared APIs and common Minecraft enforcement points;
- one central magic interaction/context model;
- generic persistent attribution for delayed/projectile/AOE/summon interactions;
- a small number of reusable mechanism-level compatibility hooks;
- very few, ideally near-zero, spell-specific addon adapters;
- optional exceptions that fail softly and are documented as unavoidable.

Addon updates should therefore primarily require checking shared integration boundaries instead of re-auditing every supported spell implementation.
