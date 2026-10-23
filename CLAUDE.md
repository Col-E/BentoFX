# BentoFX — Agent Instructions

## Ownership boundary
- You may READ core/ and demos/basic/ to understand their API.
- NEVER modify any file under core/ or demos/basic/. Authored by someone else.
- No change may span both my modules and core/ or demos/basic/. If a change
  appears to require editing those, stop and describe what is needed.
- Do not push, pull, fetch, or open pull requests. Local only.
- You may run ./gradlew build and tests. Leave commits to me.

## Code standards for my modules
- Public API is a contract: any type or method that is public is something I
  must support. Flag accidental public surface.
- Javadoc on every public type and method, explaining contract and threading
  requirements, not restating the signature.
- Prefer immutability. Fields final unless mutation is required.
- Fail fast on invalid arguments with Objects.requireNonNull / IllegalArgumentException
  and a message naming the offending parameter.
- No swallowed exceptions. No empty catch blocks. No printStackTrace.
- JavaFX: state that touches the scene graph must be documented as
  FX-Application-Thread-only. Flag anything that could be called off-thread.
- Listener registration must have a matching removal path. Flag potential leaks.
- Persistence format changes are breaking changes. Flag anything that would
  fail to restore a layout saved by a previous version.

## Design principles
Apply SOLID as a diagnostic, not a target. Flag a violation only when it
causes a concrete problem: a class that must change for unrelated reasons,
an interface forcing implementors to stub methods, a dependency on a concrete
type that prevents testing. Do not propose abstractions for hypothetical
future needs.
