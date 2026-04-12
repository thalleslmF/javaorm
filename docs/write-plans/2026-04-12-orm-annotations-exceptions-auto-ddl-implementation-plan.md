# Implementation Plan: ORM annotations, exceptions, metadata, and auto-DDL

**Date:** 2026-04-12
**Status:** planned
**Source spec:** `docs/superpowers/specs/2026-04-11-orm-annotations-exceptions-auto-ddl-design.md`

## Goal

Re-implement the approved ORM feature set in a controlled way after reverting the previous implementation commit.

## Why this plan exists

The previous implementation was reverted so the feature can be rebuilt with clearer checkpoints, documented decisions, and review-friendly commits.

## Constraints

- Work must happen on a dedicated feature branch, not directly on `main`
- The approved spec is the source of truth unless a new documented decision updates it
- Significant implementation decisions should be written down here as the work progresses

## Delivery phases

### Phase 1: annotations and validation
- Add `@Entity`, `@Table`, `@Id`, `@Column`, `@Transient`
- Add `EntityValidator`
- Add tests for validation rules

### Phase 2: metadata model
- Add `ColumnMetadata`
- Add `EntityMetadata`
- Implement structural metadata extraction and per-instance value copying
- Add tests for annotation mapping and transient exclusion

### Phase 3: parser refactor
- Change parser contract to consume `EntityMetadata`
- Refactor create, create-table, and find parsers
- Implement type-aware quoting and DDL constraint assembly
- Update parser tests

### Phase 4: exceptions and translation
- Add ORM exception hierarchy
- Add `ExceptionTranslator`
- Add tests for SQLState translation

### Phase 5: DB integration and config
- Add `OrmConfig` and `DdlAuto`
- Add metadata cache and initialized-entity tracking in `DB`
- Integrate validation, exception translation, lazy auto-DDL, and the `Find` cursor fix
- Update integration tests

### Phase 6: polish and review
- Verify package structure matches the spec
- Run tests
- Review for spec drift
- Prepare PR notes from this plan

## Initial decisions

1. Reverted commit `b7ebaad` before re-implementation to restore a clean baseline.
2. The next implementation should be split into small reviewable commits by phase.
3. This document is the running log for implementation decisions and review notes.

## Review checklist

- [ ] validation rules match the spec exactly
- [ ] `@Transient` fields are excluded from all SQL operations
- [ ] parser API is metadata-based end-to-end
- [ ] numeric values are not quoted in INSERT SQL
- [ ] `CreateTable` uses `IF NOT EXISTS`
- [ ] auto-DDL runs only on `Insert`
- [ ] `Find` does not skip the first row
- [ ] tests cover config, metadata, validator, translator, and integration flow

## Follow-up notes

Add dated notes below when implementation decisions change or additional tradeoffs appear.
