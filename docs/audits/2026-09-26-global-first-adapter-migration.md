# Global-First Adapter Migration Audit

Date: 2026-09-26
Branch: `agent/friendly-fire-compat-2.3.3`

## Result

Magic Team treats shared Iron's/global hooks as the primary protection model. Spell-specific Travel Optics, Familiars and Geomancy Plus adapters that only duplicated targeting, damage, effect, projectile, AOE or delayed-context behavior were removed.

A direct comparison against the published `magic_team-2.3.1.jar` identified three coverage paths that must not be lost merely because newer generic attribution exists:

- Iron's `/cast` command can invoke `AbstractSpell#onCast` directly for non-player living entities, bypassing normal player/mob dispatch wrappers;
- `ImmolateEffect#addImmolateStack` applies its effect without passing the afflicter to `LivingEntity#addEffect`, so the generic effect gate can otherwise lose the source outside an already-attributed spell scope;
- Cataclysm Flare Bomb and Wither Howitzer are not Iron's spell entities and can be created outside a tracked spell context, so generic spawn attribution is not a complete replacement for the 2.3.1 impact bridge.

The remaining optional mixins are exceptions for behavior that is not safely represented by the normal `AbstractSpell`/magic entity/effect pipeline, or for transactions where blocking final damage would still leave an offensive side effect behind.

## Global replacements

The removed adapter families are replaced by these shared mechanisms:

- `MagicTargetingPolicy`: one addon-neutral `TargetEntityCastData` gate reused by player pre-cast/release, player channel ticks and mob virtual spell dispatch.
- `AbstractSpellMixin`, `MagicManagerCastDispatchMixin` and `AbstractSpellCastingMobDispatchMixin`: establish spell context before addon overrides execute, including overrides that never call `super`.
- `CastCommandMixin`: preserves the published 2.3.1 global command-cast entry point for Iron's `CastCommand`, covering its direct `onCast` path.
- `ImmolateEffectMixin`: preserves the published 2.3.1 afflicter bridge for the Iron's core Immolate helper when `addEffect` itself receives no source.
- `DamageSourcesMixin` and `LivingEntityMixin`: final hostile damage/effect enforcement, preferring Iron's `SpellDamageSource` and falling back to persistent attribution when native spell metadata is absent.
- `EntityMagicAttributionMixin`: captures attribution at the shared server entity-spawn boundary and re-enters it around the stable `ServerLevel.tickNonPassenger` method boundary. This avoids depending on addon entity classes or invocation layout inside the tick method.
- `MobEffectInstanceMagicAttributionMixin` + `MagicEffectAttributionIndex`: delayed effect attribution, including Rend-like periodic effects. Attribution follows the actual effect lifetime and is removed when effects are removed.
- `EntityMixin`: Babel alliance semantics for proven magic interactions regardless of addon namespace.
- `MagicSideEffectPolicy`, `EntityMagicSideEffectMixin` and `LivingEntityMagicSideEffectMixin`: common hostile movement, fire and effect-removal side effects. Concrete beneficial operations such as extinguishing fire or removing a harmful effect remain allowed.
- `AreaEffectCloudMixin` and `ThrownPotionMixin`: plain vanilla potion/cloud behavior remains outside Magic Team, while potion/cloud entities created inside an attributed spell retain the original magic context.

Notably, `OrbitalVoidFriendlyFireMixin` is removed rather than made more tolerant; the fragile 0/1 redirect that caused the runtime crash is no longer part of the architecture.

## Retained optional exceptions

Every entry below is a `GLOBAL_FIRST_EXCEPTION` and remains in a `required:false`, `defaultRequire:0` addon config.

### Travel Optics

- `compat.traveloptics.AbyssalHideFriendlyFireMixin` — **GLOBAL_FIRST_EXCEPTION:** armor-driven target selection is not an Iron's spell cast and therefore has no `AbstractSpell` context.
- `compat.traveloptics.AbyssalStrikeFriendlyFireMixin` — **GLOBAL_FIRST_EXCEPTION:** standalone effect behavior can originate outside a tracked spell context and performs its own ally decision.
- `compat.traveloptics.ChargedSandsLevelOneFriendlyFireMixin` — **GLOBAL_FIRST_EXCEPTION:** boss-weapon projectile selection is item-driven, outside the spell API.
- `compat.traveloptics.ChargedSandsLevelTwoFriendlyFireMixin` — **GLOBAL_FIRST_EXCEPTION:** boss-weapon projectile selection is item-driven, outside the spell API.
- `compat.traveloptics.CursedWraithbladeFriendlyFireMixin` — **GLOBAL_FIRST_EXCEPTION:** item ability directly damages targets without spell attribution.
- `compat.traveloptics.FloodSlashFriendlyFireMixin` — **GLOBAL_FIRST_EXCEPTION:** one hit transaction combines damage, Wet, mana and Replenish rewards; blocking only final damage would leave offensive/reward side effects.
- `compat.traveloptics.ForgeServerEventsFriendlyFireMixin` — **GLOBAL_FIRST_EXCEPTION:** curio poison is emitted by a Forge attack event rather than the spell pipeline.
- `compat.traveloptics.ForlornHarbingerFriendlyFireMixin` — **GLOBAL_FIRST_EXCEPTION:** armor ability is not an Iron's spell cast.
- `compat.traveloptics.GalenaShatterFriendlyFireMixin` — **GLOBAL_FIRST_EXCEPTION:** stacked-target processing consumes stacks and creates a delayed mark independently of the initial damage result.
- `compat.traveloptics.HarbingersWrathFriendlyFireMixin` — **GLOBAL_FIRST_EXCEPTION:** boss-weapon pulse targeting is item-driven and outside spell attribution.
- `compat.traveloptics.MechanizedExoskeletonFriendlyFireMixin` — **GLOBAL_FIRST_EXCEPTION:** armor missile target selection happens before any attributed magic entity exists.
- `compat.traveloptics.PrimordialCrestFriendlyFireMixin` — **GLOBAL_FIRST_EXCEPTION:** armor shockwave is item-driven and outside the spell API.
- `compat.traveloptics.ReversalFriendlyFireMixin` — **GLOBAL_FIRST_EXCEPTION:** delayed retaliation may be entered as an effect without reliable original spell attribution and recreates ownerless reflected projectiles; retain until runtime proves the generic effect path fully replaces it.
- `compat.traveloptics.SpiritDamageHelperFriendlyFireMixin` — **GLOBAL_FIRST_EXCEPTION:** delayed helper damage may execute from an effect without a reliably attributable spell source; retain until runtime proves generic effect attribution covers every source.
- `compat.traveloptics.TectonicCrestFriendlyFireMixin` — **GLOBAL_FIRST_EXCEPTION:** armor shockwave is item-driven and outside the spell API.

### Alshanex's Familiars

- `compat.familiars.ServerEventsRetaliationFriendlyFireMixin` — **GLOBAL_FIRST_EXCEPTION:** Scorcher/Plague retaliation is produced by `LivingDamageEvent`, not an Iron's spell execution path.

### GTBC Geomancy Plus

- `compat.geomancyplus.TremorStepFriendlyFireMixin` — **GLOBAL_FIRST_EXCEPTION:** the shockwave resets target invulnerability state after damage, so a final damage block alone does not prevent the offensive side effect.

### Cataclysm

- `compat.cataclysm.FlareBombFriendlyFireMixin` — **GLOBAL_FIRST_EXCEPTION:** preserved from the published 2.3.1 behavior. Flare Bomb is a Cataclysm `ThrowableProjectile`, not an Iron's magic entity, and can be created outside a tracked spell scope; the adapter re-enters hostile magic context only for its impact logic.
- `compat.cataclysm.WitherHowitzerFriendlyFireMixin` — **GLOBAL_FIRST_EXCEPTION:** same published-2.3.1 parity requirement for Wither Howitzer, which can execute independently of Iron's spell attribution.

These two Cataclysm exceptions do not represent a return to per-spell adaptation. They preserve two non-Iron's projectile entry points that the published 2.3.1 JAR explicitly handled and that generic spell-spawn attribution cannot prove when the projectile originates outside Magic Team context.

## Optional-adapter failure semantics

The optional configs remain `required:false` with `defaultRequire:0`, so normal zero-match injector drift does not turn an adapter into a fatal 0/1 injection requirement.

`OptionalAddonMixinErrorHandler` is deliberately conservative:

- prepare-time failure of an optional adapter can degrade to `WARN`, because the adapter can be skipped before transformation;
- apply-time failure is logged but preserves Mixin's original action, because forcing continuation after transformation has begun could leave the target class partially transformed;
- core Magic Team mixins are never downgraded by this handler.

Therefore fail-soft applies only where Mixin can safely continue; it is not a blanket instruction to ignore every transformation error.

## Removed categories

The migration removed per-spell/per-projectile adapters for normal target filtering, direct spell damage, standard projectile/AOE behavior, delayed magic entities, delayed MobEffects and common hostile side effects. This includes the previous Orbital Void, Aqua Missiles, Tidal Grasp, Solar Storm, Hiken and numerous Travel Optics extended-projectile adapters.

The published 2.3.1 `AnnihilationSpellMixin` remains removed because the current player/mob virtual dispatch wraps the entire addon spell override before `applyAoEDamageAndExplosion` executes, so the specific helper wrapper is redundant. Cataclysm Flare Bomb/Wither Howitzer and Iron's Immolate were not equivalent cases and therefore retain explicit parity bridges.

## Runtime follow-up

The retained Reversal and Spirit Damage Helper adapters are deliberately conservative candidates for future removal. If local Forge/Arclight testing confirms that their effects always inherit `MagicEffectAttributionIndex` context, they should be removed rather than maintained indefinitely.

Runtime validation should focus on mechanism classes rather than exhaustive spell lists: direct hostile/support spells, standard targeting, projectile, AOE, delayed entity, delayed effect/Rend-like damage, summon/root-owner, attributed potion/cloud, forced movement/fire/effect removal, Iron's command cast, Immolate chaining, the two published Cataclysm projectile exceptions, one item/armor exception, and Magic Team disabled mode.
