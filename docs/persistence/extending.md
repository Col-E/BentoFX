# Extending Persistence

[&larr; Back to the BentoFX Persistence guide](guide.md)

This document describes extending the persistence platform for new data formats and storage destinations. Applications that use the codec and storage implementations provided by the framework do not need anything described herein. See [Usage](guide.md#persistence-usage) instead.

Persistence can be extended at two levels:

- **Simple extension**: write a new `LayoutCodec` or `LayoutStorage` to add a serialization format or a storage destination. The framework handles everything else. This is what applications are most likely to need.
- **Advanced extension**: write a custom `DockingLayoutPersistenceProvider` that implements the entire save-and-restore cycle. This option might be useful when the storage understands the shape of the data, such as a relational database with normalized tables. Covered under [Advanced Extension](#advanced-extension).

## Table of Contents

- [How Discovery Works](#how-discovery-works)
- [Adding a Storage Destination](#adding-a-storage-destination)
  - [Storage Implementation Conventions](#storage-implementation-conventions)
- [Adding a Codec](#adding-a-codec)
- [What ServiceLoader Requires of a Provider](#serviceloader-requirements)
- [Advanced Extension: Custom Persistence Provider](#advanced-extension)
  - [When to Take This Path](#advanced-when)
  - [The Capture and Rebuild Building Blocks](#advanced-building-blocks)
  - [Example: Relational Database](#advanced-example-relational)
  - [What to Keep in Mind](#advanced-notes)
- [Deployment Environments](#deployment-environments)
- [Complete Examples](#complete-examples)
- [See Also](#see-also)

<h2 id="how-discovery-works">How Discovery Works</h2>

`DefaultDockingLayoutPersistenceProvider` uses [ServiceLoader](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ServiceLoader.html) to acquire `LayoutCodecProvider` and `LayoutStorageProvider` implementations from the runtime module path (or the classpath for non-modularized applications). Each provider exposes a stable identifier, which is how an application selects a specific codec or storage implementation when more than one is available.

Only the *provider* is discovered. The `LayoutCodec` and `LayoutStorage` it returns are created by the provider, so an implementation is free to take constructor arguments or run setup logic that `ServiceLoader` could not supply.

<h2 id="adding-a-storage-destination">Adding a Storage Destination</h2>

1. Implement `LayoutStorage`. The interface is four methods: `exists()`, `openOutputStream()`, `openInputStream()`, and a default `close()`.

```java
public class SystemLayoutStorage implements LayoutStorage {

    private final Path path;

    SystemLayoutStorage(final Path path) {
        this.path = path;
    }

    @Override
    public boolean exists() {
        return Files.exists(path) && !isEmpty(path);
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        // Write to a temporary file and move it into place on close, so a
        // failed save leaves the previously stored layout intact.
        return newAtomicOutputStream(path);
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return Files.newInputStream(path);
    }
}
```

2. Implement `LayoutStorageProvider`, which is the type `ServiceLoader` discovers.

```java
public class SystemLayoutStorageProvider implements LayoutStorageProvider {

    @Override
    public String getIdentifier() {
        return "system";
    }

    @Override
    public LayoutStorage getLayoutStorage(
            final String layoutIdentifier,
            final String codecIdentifier
    ) {
        return new SystemLayoutStorage(resolvePath(layoutIdentifier, codecIdentifier));
    }
}
```

3. Register the provider so `ServiceLoader` can find it. Both module and classpath declarations are recommended. The JVM only honors one of them, and which one it honors depends on how the consuming application is launched.

   a. For an application launched on the **module path**, declare it in `module-info.java`:

   ```java
   provides LayoutStorageProvider with SystemLayoutStorageProvider;
   ```

   b. For an application launched on the **class path**, add a provider-configuration file, named after the service interface, containing the fully qualified name of the implementation: <br/><br/>

     File:
     ```text
     src/main/resources/META-INF/services/software.coley.bentofx.persistence.core.api.provider.LayoutStorageProvider
     ```
     Text:
     ```text
     software.coley.bentofx.persistence.impl.storage.system.provider.SystemLayoutStorageProvider
     ```

   All four bundled implementations carry both. Registering only the `provides` clause is the easier mistake to make, and it fails misleadingly: a class-path application finds no provider at all and reports `No LayoutStorageProvider implementation was found. Add a runtime dependency that provides one.` even though the dependency is present. Keep the two in sync. Renaming one implementation and not the other silenly breaks the incorrectly named path.

4. Add the module to the application's runtime dependencies.

   ```kotlin
   runtimeOnly("software.coley.bento-fx:persistence-storage-system:${version}")
   ```

   The JAR is discovered on either the module path or the class path depending on how the application is launched.

<h3 id="storage-implementation-conventions">Storage Implementation Conventions</h3>

To ensure consistent behavior among storage implementations, the following conventions are recommended, the implementations provided by the framework follow them, and callers rely on them:

1. **Closing the output stream stores the layout.** Buffer what is written and publish it only when the stream closes cleanly. This ensures a save that fails part way through leaves the previously stored layout intact instead of replacing it with a partial layout.
2. **Override default interface methods.** `isLayoutStored` has a default implementation that opens the storage and calls `exists()`, which works for any implementation. Override it when the storage implementation can answer more efficiently than opening a layout.
3. **`exists()` should specify whether there is a layout to read**, not whether a layout location is present. Empty content is not a layout. If `exisits()` were to return true for an empty layout, the codec would likely attempt to decode it, resulting in an empty layout. As such, an empty layout then erroneously manifests as a decode failure when a failure indicating "nothing stored here" would have resulted in the default layout being used instead.
4. **`close()` releases what the storage owns, and only that.** Whichever provider receives a `LayoutStorage` closes it, so a storage handed a resource it did not create should leave that resource alone.

<h2 id="adding-a-codec">Adding a Codec</h2>

1. Implement three `LayoutCodec` methods:

```java
public class YamlLayoutCodec implements LayoutCodec {

    @Override
    public String getIdentifier() {
        return "yaml";
    }

    @Override
    public void encode(
            final PersistableLayout layout,
            final OutputStream outputStream
    ) throws BentoStateException {
        // Write layout to outputStream but DO NOT CLOSE IT.
        // Whoever opened the stream owns it and should close it.
    }

    @Override
    public PersistableLayout decode(
            final InputStream inputStream
    ) throws BentoStateException {
        // Either read a PersistableLayout back or throw BentoStateException.
    }
}
```

2. Implement `LayoutCodecProvider`, which is the type `ServiceLoader` discovers.

```java
public class YamlLayoutCodecProvider implements LayoutCodecProvider {

    @Override
    public String getIdentifier() {
        return "yaml";
    }

    @Override
    public LayoutCodec getLayoutCodec() {
        return new YamlLayoutCodec();
    }
}
```

3. Register the provider so `ServiceLoader` can find it. Both module and classpath declarations are recommended. The JVM only honors one of them, and which one it honors depends on how the consuming application is launched.

   a. For an application launched on the **module path**, declare it in `module-info.java`:

   ```java
   provides LayoutCodecProvider with YamlLayoutCodecProvider;
   ```

   b. For an application launched on the **class path**, add a provider-configuration file, named after the service interface, containing the fully qualified name of the implementation:

     File:
     ```text
     src/main/resources/META-INF/services/software.coley.bentofx.persistence.core.api.provider.LayoutCodecProvider
     ```
     Text:
     ```text
     software.coley.bentofx.persistence.impl.codec.yaml.provider.YamlLayoutCodecProvider
     ```

   Registering only the `provides` clause is the easier mistake to make, and it fails misleadingly: a class-path application finds no provider at all even though the dependency is present. Keep the two in sync. Renaming one implementation and not the other silently breaks the incorrectly named path.

4. Add the module to the application's runtime dependencies.

   ```kotlin
   runtimeOnly("software.coley.bento-fx:persistence-codec-yaml:${version}")
   ```

   The JAR is discovered on either the module path or the class path depending on how the application is launched.

Additional considerations:

1. **The codec identifier becomes part of how a layout is addressed.** For example, file-backed storage may join it to the layout identifier to form a file name, so it must be compatible that: see [Choosing Stable Identifiers](guide.md#choosing-stable-identifiers). 
2. **Changing codec identifier may orphan layouts** already stored under the old one.
3. **`decode` receives whatever was stored, including nothing useful.** A truncated or invalid encoding must raise `BentoStateException` rather than returning a partly-populated `PersistableLayout`. An exception is treated as "fall back to the default layout" and an incomplete or empty value as "this is the layout".

<h2 id="serviceloader-requirements">What ServiceLoader Requires of a Provider</h2>

Both provider interfaces are discovered the same way, so both implementations must:

* be a public concrete class
* have a public no-argument constructor, or an implicit default constructor
* return a stable identifier from `getIdentifier()`
* optionally return `true` from `isDefault()` to be selected automatically when several providers are present
* be registered twice: with a `provides` clause in `module-info.java` for module-path launches, and with a `META-INF/services` file for class-path launches

<h2 id="advanced-extension">Advanced Extension: Custom Persistence Provider</h2>

A custom `DockingLayoutPersistenceProvider` replaces the `LayoutCodec`/`LayoutStorage` pipeline entirely. The framework's lifecycle hooks still call it the same way, but what happens below those hooks must be implemented. In such a case, the storage can inspect, index, query, and version the layout content directly, rather than treating it as an opaque byte stream.

<h3 id="advanced-when">When to Take This Path</h3>

The `LayoutCodec`/`LayoutStorage` pair is the right answer for almost every extension. Take the advanced path only when the storage genuinely needs to understand the data rather than treat it as opaque bytes.

The driving case is a **relational database with a normalized schema**: you want to run SQL against layout content (*"which users have the terminal pane open?"*), add [JPA audit history](https://hibernate.org/orm/envers/) over structured changes, or run schema migrations via Flyway or Liquibase. A single `BLOB` column cannot support any of that, and extending the `LayoutCodec`/`LayoutStorage` pair to simulate it is more work than writing a custom `DockingLayoutPersistenceProvider` that is intentional about talking to a schema.

For anything short of that - a service that stores and returns bytes keyed by a layout identifier, a different file format, a cloud storage destination - stick with the `LayoutCodec`/`LayoutStorage` pair. Write a `LayoutStorage` whose `openOutputStream()` writes to the destination and whose `openInputStream()` reads back, pair it with any `LayoutCodec`, and let the framework do the rest. A custom `DockingLayoutPersistenceProvider` must implement all eight of its methods along with the capture/rebuild wiring.

<h3 id="advanced-building-blocks">The Capture and Rebuild Building Blocks</h3>

The two conversions the default pipeline performs are published as public API so a custom provider does not have to reimplement them:

- [`BentoStateCapturer`](../../persistence/core/src/main/java/software/coley/bentofx/persistence/core/api/BentoStateCapturer.java) - walks the live BentoFX tree into a `List<BentoState>`. One method: `capture()`.
- [`DockingLayoutRebuilder`](../../persistence/core/src/main/java/software/coley/bentofx/persistence/core/api/DockingLayoutRebuilder.java) - takes a `List<BentoState>` and returns a live `DockingLayout`. One method: `rebuild(List<BentoState>)`.

Each has a static factory that returns the framework's default implementation. Both must be called on the JavaFX Application Thread, matching the thread rules the framework applies when the default provider calls them.

A custom `DockingLayoutPersistenceProvider`'s save step uses `capture()`, decomposes the result into its own schema, and persists it. The restore step reconstructs a `List<BentoState>` from storage and calls `rebuild(List<BentoState>)`. The custom `DockingLayoutPersistenceProvider` must write everything in between.

<h3 id="advanced-example-relational">Example: Relational Database</h3>

Sketch of a `DockingLayoutPersistenceProvider` that writes layouts to normalized tables:

```java
public final class NormalizedDbPersistenceProvider
        implements DockingLayoutPersistenceProvider {

    private final LayoutRepository repository;

    @Override
    public LayoutSaver getLayoutSaver(
            final LayoutPersistenceProfile profile,
            final BentoProvider bentoProvider
    ) {
        final BentoStateCapturer capturer =
                BentoStateCapturer.create(bentoProvider);

        return () -> {
            // capture() must run on the JavaFX Application Thread. The 
            // framework's LayoutSaver handles this. A custom implementation 
            // is responsible for scheduling it correctly.
            final List<BentoState> state = capturer.capture();
            repository.saveNormalized(profile.layoutIdentifier(), state);
        };
    }

    @Override
    public LayoutRestorer getLayoutRestorer(
            final LayoutPersistenceProfile profile,
            final BentoProvider bentoProvider,
            final DockableStateProvider dockableStateProvider,
            final @Nullable StageIconImageProvider iconProvider,
            final @Nullable DockContainerLeafMenuFactoryProvider leafMenuFactoryProvider
    ) {
        final DockingLayoutRebuilder rebuilder = DockingLayoutRebuilder.create(
                bentoProvider,
                dockableStateProvider,
                iconProvider,
                leafMenuFactoryProvider
        );

        return defaultLayoutSupplier -> {
            final List<BentoState> state =
                    repository.loadNormalized(profile.layoutIdentifier());

            return state.isEmpty()
                    ? defaultLayoutSupplier.get()
                    : rebuilder.rebuild(state);
        };
    }
	
    // The remaining DockingLayoutPersistenceProvider methods all go through 
    // the repository rather than LayoutCodec and LayoutStorage.
}
```

The `LayoutRepository` is must be implemented as part of extending `DockingLayoutPersistenceProvider`. It can decompose a `BentoState` into whatever tables make sense: one per Bento, per root branch, per leaf, per dockable, plus join tables for divider positions, drag-drop stages, and so on. JPA entities with `@Version` for optimistic locking, Hibernate Envers for audit history, Flyway migrations for schema evolution, all plug in at this layer and the framework does not need to know about any of it.

<h3 id="advanced-notes">What to Consider</h3>

A custom `DockingLayoutPersistenceProvider` implementation owns thread discipline. The default framework provider runs `capture()` on the JavaFX Application Thread and schedules writing to storage off of this thread. A custom provider inherits the same responsibility: `capture()` and `rebuild()` must be called on the JavaFX Application Thread. The framework exposes [`PersistenceThreading.callOnFxThread`](../../persistence/core/src/main/java/software/coley/bentofx/persistence/core/impl/PersistenceThreading.java) via the internal implementation; a custom provider typically wraps capture/rebuild with its own equivalent using `Platform.runLater` or `FutureTask`-on-FX.

A custom `DockingLayoutPersistenceProvider` implementation that wants `LayoutsMenu` to offer group management and active-layout tracking also implements [`PersistedDockingLayoutOrganizationProvider`](../../persistence/core/src/main/java/software/coley/bentofx/persistence/core/api/provider/PersistedDockingLayoutOrganizationProvider.java). It is a separate, optional interface. The menu checks for it with `instanceof` and degrades gracefully when it is absent.

A custom `DockingLayoutPersistenceProvider` implementation should be discoverable through `ServiceLoader`, the same way the default implementation is, so the `provides` clause in `module-info.java` and the matching `META-INF/services` file both apply. Point them at `DockingLayoutPersistenceProvider`, not at `LayoutCodecProvider` or `LayoutStorageProvider`.

<h2 id="deployment-environments">Deployment Environments</h2>

The bundled codec and storage providers target the standard Java deployment surface: an application launched on the class path, on the module path (JPMS), or packaged with `jlink` or `jpackage`. Both launch styles are covered by the `provides`/`META-INF/services` pattern [described above](#how-discovery-works) and no extra configuration is required of the application.

Deployment environments outside that surface are the application's responsibility. The most common one worth calling out:

- **OSGi containers.** The bundled storage and codec modules are not OSGi bundles: they ship no bundle manifest, no `Meta-Persistence` header, and their transitive dependencies may not all be published as bundles either. Dropping these JARs into an OSGi container is not expected to work.

An application that needs to persist layouts inside an OSGi container (or any other environment the bundled providers do not support) implements its own `LayoutStorageProvider` against whatever persistence stack its environment supports, and registers it the same way as any other provider. The implementation path is identical to [Adding a Storage Destination](#adding-a-storage-destination):

1. Write a `LayoutStorage` whose `openOutputStream` and `openInputStream` talk to the chosen persistence stack.
2. Write a `LayoutStorageProvider` that returns one.
3. Register the provider via `module-info.java` for module-path launches and `META-INF/services` for class-path launches, plus whatever manifest the target container expects (an OSGi `Bundle-SymbolicName` and `Export-Package`, for example).

The application module takes on the container-specific wiring. The framework contracts stay the same: a `LayoutStorage` reads and writes bytes, a `LayoutCodec` turns those bytes into a layout and back, and the identifier a provider returns from `getIdentifier()` is how an application selects it from a `LayoutPersistenceProfile`.

<h2 id="complete-examples">Complete Examples</h2>

Four working implementations, two of each:

- [JSON Codec](../../persistence/codec/json)
- [XML Codec](../../persistence/codec/xml)
- [H2 Database Storage](../../persistence/storage/db/h2)
- [File Storage](../../persistence/storage/file)

<h2 id="see-also">See Also</h2>

- [Implementation: How a save and a restore exercises codecs and storages, including sequence diagrams](implementation.md)
- [ServiceLoader: The Java 21 API documentation](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ServiceLoader.html)
- [Introduction to the Service Provider Interface](https://docs.oracle.com/javase/tutorial/sound/SPI-intro.html)
- [Java Service Provider Interface](https://www.baeldung.com/java-spi)
