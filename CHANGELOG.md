# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - Unreleased

The first stable release. It catches the SDK up with the public API: groups, the rest of the identity
reads and writes, events, devices, and escalations, with paging throughout. Nothing from 0.1.0 is removed
or renamed, so upgrading needs no code changes.

### Added

- `TrackRequest.Builder.group()`: the organizations, teams, workspaces, or other groups the user is
  acting in, each with an `id`, a `type` (`"organization"` when null), and optional `data`. Dregs
  makes the identity a member of each. `TrackRequest.groups()` returns them as `TrackGroup` records.
- `groups()`: `list()`, `listAll()`, `get()`, `create()`, `update()`, `addMember()`, and
  `removeMember()`.
- `identities().list()`, `listAll()`, `update()` (to disregard an identity or merge attributes, with
  an `IdentityUpdate`), `history()`, `links()`, `reviews()`, and `review()`, and an
  `analysis(identityId, cycleId)` overload for a past cycle.
- `events()`: `list()`, `listAll()`, and `get()`, with the groups each event was sent with.
- `devices()`: `list()`, `listAll()`, `get()`, and `update()` to disregard a device.
- `escalations()`: `list()`, `listAll()`, `get()`, `update()` to move an escalation along, and
  `summary()`.
- `Page`, an `Iterable` of one page's items with its position and totals, and a `listAll()` on every
  namespace that walks the pages for you. Filters, paging, and order go in `IdentityListParams`,
  `GroupListParams`, `EventListParams`, `DeviceListParams`, and `EscalationListParams`.
- The same on `AsyncDregs`, where `listAll()` completes with every page's items.

### Changed

- An error response without a message in its body is now described by its status's reason phrase,
  such as `HTTP 404: Not Found`, rather than `Request failed`. For a status without a standard phrase,
  the start of a non-JSON body (a proxy's error page, say) is quoted instead.

## [0.1.0] - 2026-09-22

The first release. A server-side client for tracking events and reading scores.

### Added

- `Dregs` and `AsyncDregs` clients, authenticating with a credential's `sk_` secret key against a
  configurable base URL.
- `track()` for recording backend events against an identity, always sending an idempotency `id`
  so a retry cannot double-count.
- `identities().get()`, `identities().scores()`, `identities().analysis()`, and
  `identities().analyze()`.
- Typed exceptions for 400, 401, 402, 403, 404, 429, and 5xx, plus connection and timeout failures,
  all deriving from `DregsException`.
- Automatic retries with exponential backoff and jitter, honouring `Retry-After`.
- `Webhooks.verify()` for checking a webhook's signature and rejecting replays.
- Lenient record models with a `raw()` escape hatch for fields added after this release.

[1.0.0]: https://github.com/dregs-sdk/dregs-sdk-java/compare/v0.1.0...v1.0.0
[0.1.0]: https://github.com/dregs-sdk/dregs-sdk-java/releases/tag/v0.1.0
