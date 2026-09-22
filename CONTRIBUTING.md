# Contributing

Thanks for helping improve the Dregs Java SDK.

This is a **port** of the [reference SDK](https://github.com/dregs-sdk/dregs-sdk-python), which the
TypeScript, Ruby, and PHP SDKs are also ported from. A change to the public surface here is a change
to all five, so surface changes are worth discussing in an issue before you write the code, and they
usually belong in the reference SDK first.

## Getting set up

You need a JDK 17 or newer. The Gradle wrapper brings its own Gradle, so there is nothing else to
install.

```bash
./gradlew build
```

Then:

```bash
./gradlew test                       # tests
./gradlew javadoc                    # the documentation check
./gradlew compileExamplesJava        # the examples still compile
./gradlew runTrackEvent              # an example, against $DREGS_SECRET_KEY
```

`build` runs all of it. There is no separate linter: `-Xlint:all -Werror` on the compiler and
`-Xdoclint:all -Werror` on javadoc are the static check, which is also what enforces documentation on
every public member. CI runs the same commands on Java 17, 21, and 25.

The library targets Java 17 with `--release 17`, whatever JDK you build with. It runs on other
people's JVMs, so it deliberately does not follow the Dregs API's own Java version.

## Dependencies

Gson is the only runtime dependency, deliberately: it is small, it has no transitive dependencies of
its own, and it stays confined to one class so nothing it defines reaches a public signature. Adding
a second runtime dependency needs a good reason, because every one of them is a version conflict
waiting to happen in somebody else's application.

Both the versions and the checksums are pinned:

- `gradle.lockfile` — the resolved versions. Regenerate with `./gradlew dependencies --write-locks`.
- `gradle/verification-metadata.xml` — their SHA-256 checksums. Regenerate with
  `./gradlew --write-verification-metadata sha256 --refresh-dependencies build javadoc`.

Commit both alongside any dependency change. CI fails on either being stale rather than quietly
building against something else.

## What we look for

- **Tests.** The suite runs the JDK's own `com.sun.net.httpserver.HttpServer` on loopback, so it
  exercises the real `HttpClient` with no network and no test-only HTTP dependency. New behavior
  needs a test; a bug fix needs one that fails without it.
- **Javadoc.** Every public type and method carries it, with real guidance about what an argument is
  for and what a caller should do about a failure, not a restatement of the signature. The build
  fails without it.
- **Lenient parsing.** Models tolerate fields they do not recognize and keep the raw body in `raw()`.
  An SDK that throws on a response it half-understands ages badly.
- **Both clients.** `Dregs` and `AsyncDregs` share everything but the waiting. A change to one almost
  always belongs in `AbstractDregsClient` instead, and a test pins their surfaces to each other.

## The API this wraps

The [Dregs manual](https://dregs.com/manual/api/) is the source of truth for the REST API. If this SDK
disagrees with the manual, the manual wins; please say so in your pull request so both get fixed.

## Reporting problems

Bugs and feature requests go to
[GitHub issues](https://github.com/dregs-sdk/dregs-sdk-java/issues). Security reports go to
[security@dregs.com](mailto:security@dregs.com) instead — see [SECURITY.md](SECURITY.md).
Questions about your account or the service go to [support@dregs.com](mailto:support@dregs.com).
