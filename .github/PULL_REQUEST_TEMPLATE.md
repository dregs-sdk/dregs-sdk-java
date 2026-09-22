## What this changes

<!-- A sentence or two. If it changes the public surface, say so plainly: this SDK is a port of the
     reference Python SDK, and the TypeScript, Ruby, and PHP ports share its shape. -->

## Checklist

- [ ] `./gradlew build` passes, which covers the compiler warnings, javadoc, and the tests
- [ ] New behavior has a test, or the fix has one that failed before it
- [ ] Every new public type and method has javadoc that says something a signature does not
- [ ] A change to `Dregs` has its counterpart in `AsyncDregs`, or belongs in `AbstractDregsClient`
- [ ] `gradle.lockfile` and `gradle/verification-metadata.xml` are committed, if dependencies changed
- [ ] `CHANGELOG.md` has an entry under Unreleased, for anything user-visible
