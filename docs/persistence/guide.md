# BentoFX Persistence

[&larr; Back to the BentoFX README](../../README.md)

## Overview   

The [persistence](../../persistence) modules, herein referred to as the "persistence framework", or just "framework", supplements docking by saving and restoring BentoFX docking layouts across application executions. The framework saves the structure of the docking layout, selected dockables, divider positions, collapsed containers, and drag/drop stages. The framework does **not** serialize non-docking components such as JavaFX `Node`, `Stage`, and `Menu` nor does it serialize application-domain objects.

Because a saved layout records only structure, restoring a layout requires turning a saved identifier back into a live object, which is what **providers** do. Providers are small interfaces the application implements so the framework can ask for the content it cannot serialize. A saved layout says "a dockable called `terminal` was open here"; the provider is what provides the actual `terminal` that goes there. The framework defines eight providers, most with one or more default implementations available. [Provider Interfaces](providers.md) describes them all.

> <span style="font-size: 1.5em;">💡</span>  `DockableStateProvider` is the only required provider interface for which the framework does not include a default implementation.

Application developers control the serialized format and storage destination by adding runtime dependencies for `LayoutCodecProvider` and `LayoutStorageProvider` implementations. In the common case, changing from one codec or storage implementation to another only requires changing runtime dependencies, not application code.

To further support persisting docking layouts, the framework also offers a ready-to-use [LayoutsMenu](layouts.md#layouts-menu) for applications to include in their own menu bars. The Layouts menu allows users to save, restore, and manage custom layouts. Because the menu text comes from a `ResourceBundle`, applications can translate or otherwise replace wording to suit application specific requirements.

> <span style="font-size: 1.5em;">💡</span> A saver and a restorer each work with one layout, named by a layout identifier, in one format, at one storage destination. Applications can use multiple codecs and storage locations because the codec and the storage destination are chosen per saver and per restorer using a `LayoutPersistenceProfile`. For additional information, see [Managing Several Layouts](layouts.md#managing-several-layouts).

## Table of Contents

- [Overview](#overview)
- [Usage](#persistence-usage)
  - [Gradle (Groovy DSL)](#persistence-gradle-groovy-dsl)
  - [Gradle (Kotlin DSL)](#persistence-gradle-kotlin-dsl)
  - [Maven](#persistence-maven)
- [Quick Start](#persistence-quick-start)
- [Concepts](#persistence-concepts)
  - [Application Design for Persistence](#application-design-for-persistence)
  - [Choosing Stable Identifiers](#choosing-stable-identifiers)
- [Common Tasks](#common-tasks)
  - [Configuring Storage Location](#configuring-storage-location)
  - [Restoring the Layout](#restoring-the-layout)
  - [Saving the Layout](#saving-the-layout)
- [Runtime Considerations](#runtime-considerations)
  - [JavaFX Application Thread](#javafx-application-thread)
  - [Application Evolution](#application-evolution)
- [Where to Go Next](#where-to-go-next)

Five companion documents continue from here. [Where to Go Next](#where-to-go-next) at the end of this guide says which one answers what.

<h3 id="persistence-usage">Usage</h3>

In addition to the `core` module, applications using persistence need:

* `persistence-core`
* one codec implementation, such as `persistence-codec-json` or `persistence-codec-xml`
* one storage implementation, such as `persistence-storage-file` or `persistence-storage-db-h2`

For debugging purposes, applications can also enable logging in the persistence modules by adding an optional [SLF4J runtime dependency](https://www.slf4j.org/manual.html#swapping). Depending on which SLF4J implementation is used, applications might also need to include a logging configuration file. See [logging.properties](../../demos/persistence/src/main/resources/logging.properties) in the persistence demo project for an example Java Utils Logging (JUL) configuration file.

The codec and storage provider implementations are discovered at runtime using the Java [ServiceLoader](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ServiceLoader.html) and Service Provider Interfaces (SPIs). When exactly one codec provider and one storage provider are available, the default persistence provider selects them automatically. When multiple providers are available, applications can select providers explicitly using `LayoutPersistenceProfile`; otherwise, the framework uses a single default provider when one is available or fails with a configuration error.

<h4 id="persistence-gradle-groovy-dsl">Gradle (Groovy DSL)</h4>

```groovy
implementation 'software.coley.bento-fx:persistence-core:${version}'
runtimeOnly 'software.coley.bento-fx:persistence-codec-xml:${version}'
runtimeOnly 'software.coley.bento-fx:persistence-storage-file:${version}'
// (optional example to enable persistence logging using JUL)
runtimeOnly 'org.slf4j:slf4j-jdk14:${slf4j-version}'
```

<h4 id="persistence-gradle-kotlin-dsl">Gradle (Kotlin DSL)</h4>

```kotlin
implementation("software.coley.bento-fx:persistence-core:${version}")
runtimeOnly("software.coley.bento-fx:persistence-codec-xml:${version}")
runtimeOnly("software.coley.bento-fx:persistence-storage-file:${version}")
// (optional example to enable persistence logging using JUL)
runtimeOnly("org.slf4j:slf4j-jdk14:${slf4j-version}")
```

<h4 id="persistence-maven">Maven</h4>

```xml
<dependency>
    <groupId>software.coley.bento-fx</groupId>
    <artifactId>persistence-core</artifactId>
    <version>${version}</version>
</dependency>
<dependency>
    <groupId>software.coley.bento-fx</groupId>
    <artifactId>persistence-codec-xml</artifactId>
    <version>${version}</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>software.coley.bento-fx</groupId>
    <artifactId>persistence-storage-file</artifactId>
    <version>${version}</version>
    <scope>runtime</scope>
</dependency>
<!-- optional example to enable persistence logging using JUL -->
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-jdk14</artifactId>
    <version>${slf4j-version}</version>
    <scope>runtime</scope>
</dependency>
```

<h3 id="persistence-quick-start">Quick Start</h3>

A persistent application generally follows this startup flow:

1. Create the application's `Bento` with a stable identifier.
2. Register the `Bento` with a `BentoProvider`.
3. Create implementations for `DockableStateProvider`, `DockableMenuFactoryProvider`, `DockContainerLeafMenuFactoryProvider`, and `StageIconImageProvider` as needed. Only [`DockableStateProvider`](providers.md#dockablestateprovider) has no framework implementation, so it is the only one an application must implement for itself.
4. Build the application's default `DockingLayout` using the same providers that restoration will use.
5. Ask [`LayoutRestorer`](#restoring-the-layout) to restore the last saved layout, passing the default layout supplier as the fallback when one does not exist or cannot be restored.
6. Apply the returned `DockingLayout` to the JavaFX stage.
7. Obtain a [`LayoutSaver`](#saving-the-layout) and keep it. The default `LayoutSaver` provided by the framework runs auto-save for the duration of the application execution. Accordingly, the LayoutSaver should be acquired/instantiated after the layout is applied, because a save reads the root branches that have a `Scene`.
8. Save the layout and close the saver before windows are closed.

An application that does not want layouts to be auto-saved skips steps 7 and 8, and calls `DockingLayoutPersistenceProvider.saveLayout(...)` instead, which saves once and holds nothing (see [Saving the Layout](#saving-the-layout)). The [persistence demo](../../demos/persistence) follows this pattern exactly and is the fastest way to see persistence in action:

```bash
./gradlew :demos:persistence:run
```

`BoxApp` in the [persistence demo](../../demos/persistence/src/main/java/software/coley/bentofx/demo/persistence/BoxApp.java) is derived from `BoxApp` in the [basic demo](../../demos/basic/src/main/java/demo/BoxApp.java), so a diff between the two shows what persistence adds. `BoxApp.applyDockingLayout(DockingLayout)` and `BoxApp.saveDockingLayout(WindowEvent)` are the two methods worth reading first.

The basic demo focuses on container construction and docking behavior. The persistence demo adds provider-backed reconstruction so layouts survive across executions, and deliberately introduces abstractions the core docking framework does not need, such as providers and state objects. For a row-by-row comparison, see [Basic demo vs persistence demo](implementation.md#basic-demo-vs-persistence-demo).

<h3 id="persistence-concepts">Concepts</h3>

The persistence framework has two responsibilities:

1. Save the current BentoFX container graph into serializable state.
2. Restore that state into runtime BentoFX objects.

The framework can save and restore BentoFX layout structure, but the application must still know how to create its own runtime content. For that reason, persistent applications should construct dockable states through stable identifiers and providers rather than only creating dockables inline. Runtime content can be created statically, dynamically, eagerly, lazily, through dependency injection, or by any other mechanism chosen by the application developer. During restoration, providers are given the identifier associated with a persisted object and are expected to return the corresponding runtime object - if one can be reconstructed.

<h4 id="application-design-for-persistence">Application Design for Persistence</h4>

A persistent application should separate two concerns:

1. **dockable creation**: how to construct a runtime `Dockable` for a stable identifier
2. **dockable placement**: where that dockable appears in the current layout

The persistence demo shows this separation. It statically defines stable dockable identifiers in `DockableProperties` as `enum` values, resolves those identifiers through `BoxAppDockableStateProvider`, and then builds runtime `Dockable` instances from the resolved `DockableState`.

```java
dockableStateProvider.resolveDockableState(dockableProperties.getIdentifier())
        .ifPresent(dockableState ->
                container.addDockable(buildDockable(dockableState)));
```

That same provider is passed to the `LayoutRestorer`, so the default layout and the restored layout use one dockable resolution strategy between them. The default layout places dockables for the first run; the restored layout uses persisted placement and asks the same provider to recreate each dockable by identifier.

<table>
<tr><th align="left">Default layout startup</th><th align="left">Saved layout restore</th></tr>
<tr valign="top"><td><pre>
Dockable identifier
    -> DockableStateProvider
        -> DockableState
            -> BoxApp.buildDockable(DockableState)
                -> Dockable
                    -> Added to default container
</pre></td><td><pre>
Persisted dockable identifier
    -> DockableStateProvider
        -> DockableState
            -> LayoutRestorer builds the Dockable
                -> Dockable
                    -> Added to restored container
</pre></td></tr>
</table>

The middle of each flow is the only difference, and that is the whole point. Where the identifier comes from, and who builds the `Dockable`, is the only thing that needs to change.

Application code therefore needs no separate implementation for first-run dockables and restored ones. If a dockable can be created for the default layout, it can also be recreated when the persisted layout refers to the same identifier.

<h4 id="choosing-stable-identifiers">Choosing Stable Identifiers</h4>

Identifiers should remain stable across application executions and software upgrades.

Identifiers may represent application views, tools, documents, domain objects, or any other concept that can be consistently reconstructed by the application.

Examples include:

- `projects`
- `terminal`
- `editor`
- `workspace-explorer`
- `document:12345`
- `customer:98765`
- `order:ABC123`

Avoid identifiers based on:

- runtime object instances
- memory addresses
- hash codes
- generated UUIDs that change between executions
- timestamps
- localized display labels
- values that change between application executions

Changing an identifier effectively creates a new dockable from the perspective of the persistence framework and may prevent previously saved layouts from restoring correctly. If an application intentionally renames an identifier, the `DockableStateProvider` should consider mapping the old identifier to the new runtime object when backward compatibility is required.

Applications should also consider how identifiers are resolved when the underlying object is no longer available. For example, a persisted identifier may refer to a document, record, or domain object that no longer exists when a layout is restored. In such cases, providers may choose to return an alternative dockable, a placeholder dockable, or no dockable at all, depending on the application's requirements. 

> <span style="font-size: 1.5em;">💡</span>  An empty `Optional<DockableStateProvider>` means the identifier cannot be reconstructed, and the restorer continues without that dockable.

<h3 id="common-tasks">Common Tasks</h3>

<h4 id="configuring-storage-location">Configuring Storage Location</h4>

By default, the included `persistence-storage-file` and `persistence-storage-db-h2` providers keep their data under `<user.home>/.bentofx`. Two situations call for changing that:

* an application wants its data somewhere more appropriate than the user's home directory, such as a platform-specific application-data directory in a packaged desktop build.
* more than one BentoFX-based application runs on the same machine. Left unconfigured, they share the same default directory and the same H2 database file, so one application's layouts collide with another's.

Both providers resolve their location through `software.coley.bentofx.persistence.core.api.storage.LayoutStorageLocations`, which reads two settings fresh on every call. Each can be given either as a `System` property or as an environment variable of the matching name - the property always wins when both are set:

| System property | Environment variable | Effect |
|------------------|-----------------------|--------|
| `bentofx.persistence.home` | `BENTOFX_PERSISTENCE_HOME` | Overrides the base directory, in place of `<user.home>/.bentofx`. |
| `bentofx.persistence.namespace` | `BENTOFX_PERSISTENCE_NAMESPACE` | Names a subdirectory of the resolved home that is this application's own, so a different BentoFX-based application on the same machine does not share it. |

The environment variable form needs no application code at all - set it before the process starts, the same way `JAVA_HOME` or `GRADLE_USER_HOME` work, and the next storage provider that resolves its location picks it up. The persistence demo's [Runner.java](../../demos/persistence/src/main/java/software/coley/bentofx/demo/persistence/Runner.java) sets its namespace this way, in code, using `LayoutStorageLocations.configureNamespace("persistence-demo")`.

`LayoutStorageLocations.configureNamespace` and `configureHome(Path)` are typed alternatives to calling `System.setProperty` directly. Whichever way these options are set, the call has to happen before the first save, restore, or catalog call. In practice, this is before `DockingLayoutPersistence.provider()` is first called, since that is when a storage provider actually reads the location:

```java
LayoutStorageLocations.configureNamespace("my-app");

final DockingLayoutPersistenceProvider persistence =
        DockingLayoutPersistence.provider();
```

<h4 id="restoring-the-layout">Restoring the Layout</h4>

The `LayoutRestorer` restores the last saved layout when one exists. If no persisted layout exists, or if deserialization fails, the default layout supplier is used.

A restorer owns the `LayoutStorage` it was given and closes it, so obtain it as a resource. The layout it returns is fully built by the time `restoreLayout` returns, so closing the storage afterward costs nothing:

```java
private DockingLayout getDockingLayout() {
    try (final LayoutRestorer layoutRestorer =
                 persistenceProvider.getLayoutRestorer(
                         SESSION_LAYOUT_IDENTIFIER,
                         bentoProvider,
                         dockableStateProvider,
                         stageIconImageProvider,
                         dockContainerLeafMenuFactoryProvider
                 )
        ) {
        return layoutRestorer.restoreLayout(this::getDefaultDockingLayout);
    } catch (BentoStateException e) {
        logger.warn("Could not create the docking layout restorer.", e);
        return getDefaultDockingLayout();
    }
}
```

Unlike a saver, a restorer holds no scheduler and no listeners, so building one per restore is inexpensive.

An empty `Optional` from `resolveDockableState` is not an error. The restorer logs a warning naming the identifier, skips that one dockable, and restoration continues. The leaf is still created and added, and divider positions, collapsed state, sibling dockables and drag/drop stages all restore normally. 

Two consequences follow:

1. If the skipped dockable was the selected one, the leaf opens on whichever tab it defaults to instead.
2. If every dockable in a leaf is skipped, the empty leaf is still added. Nothing prunes it, so the user sees the node from `placeholderBuilding()` where its content would have been. Restoration succeeds, minus the tab.

Applying the returned layout can still fail. For example, a stored layout may hold root branches the application does not know how to place. In such instances, report whether anything was applied and fall back to the default layout, because a stage that never receives a `Scene` is never shown, and an application whose only exit path runs when its window is never shown cannot then be closed:

```java
DockingLayout dockingLayout = getDockingLayout();

if (!applyDockingLayout(dockingLayout)) {
    logger.warn(
            "Could not apply the restored docking layout; " +
                    "applying the default docking layout instead."
    );

    if (!applyDockingLayout(getDefaultDockingLayout())) {
        logger.error("Could not apply the default docking layout.");
    }
}
```

`applyDockingLayout` above is not framework API. It is the application's own helper, and it returns whether it managed to apply anything, which is what makes the fallback decision above possible:

```java
private boolean applyDockingLayout(final DockingLayout dockingLayout) {
    boolean isApplied = false;

    for (final BentoLayout bentoLayout : dockingLayout.getBentoLayouts()) {
        if (bentoLayout.matchesIdentity(bento)) {
            isApplied |= applyBentoLayout(bentoLayout);
        }
    }

    return isApplied;
}
```

The persistence demo applies the matching `BentoLayout` to the primary stage this way, then shows any restored `DragDropStage` instances.

<h4 id="saving-the-layout">Saving the Layout</h4>

The default `LayoutSaver` implementation also supports automatic scheduled saves. A saver obtained from a `DockingLayoutPersistenceProvider` arrives with auto-save running: it listens for `DockEvent` notifications from every `Bento` supplied by the `BentoProvider` and schedules a save task at a default interval of five minutes. `AbstractAutoCloseableLayoutSaver` deliberately does not start auto-save from its constructor, because that would hand a partly-built object to a scheduler thread and to every `Bento` event bus; a saver constructed directly starts auto-save with `enableAutoSave(long, TimeUnit)`.

The scheduled task does not write the layout on every interval. It first checks whether any `DockEvent` was received since the previous save attempt. If no docking changes were detected, the task skips the save operation.

Because auto-save runs for as long as the saver exists, obtain one saver while the application starts and keep it. A saver built where the layout is saved arms a scheduler for an application that is already exiting.

Applications should still save explicitly during shutdown, because the scheduled task may not have run after the user's most recent layout change. Save before the primary stage and any secondary stages are closed, since closed windows are no longer discoverable by the saver.

Closing the saver is what removes its listener from each `Bento`, stops the scheduler, and releases the storage it was given. Close it while the windows still exist. An application whose exit path calls `System.exit(...)` never runs `Application.stop()`, so the close request handler is the last point at which closing still happens:

```java
stage.setOnCloseRequest(this::saveDockingLayout);
```

```java
private void saveDockingLayout(final WindowEvent windowEvent) {
    final LayoutSaver saver = layoutSaver;

    try (saver) {
        if (saver == null) {
            return;
        }
        saver.saveLayout();
    } catch (final BentoStateException e) {
        logger.warn("Could not save the docking layout.", e);
    }
}
```

Saving explicitly before the close is deliberate: closing saves only when a `DockEvent` has arrived since the last save, and an application usually wants a layout written on exit either way.

<h3 id="runtime-considerations">Runtime Considerations</h3>

Applications should keep two runtime considerations in mind when using
layout persistence.

<h4 id="javafx-application-thread">JavaFX Application Thread</h4>

Restoration separates persistence operations from JavaFX object creation.
Storage access and codec decoding can occur independently of the JavaFX
Application Thread, but JavaFX objects must still be created and modified
according to normal JavaFX threading rules.

The persistence framework restores BentoFX runtime objects on the JavaFX
Application Thread when those objects are created by the framework.

Application providers have the same responsibility for the objects they
supply. If a provider creates JavaFX objects such as `Node`, `Tooltip`,
`ContextMenu`, `MenuItem`, `Image`, or `Stage`, those objects should be
created on the JavaFX Application Thread.

The persistence demo builds its `DockableState` instances on first use,
because those states contain JavaFX controls and the thread that
constructs a provider is not necessarily the JavaFX Application Thread.
Applications using lazy loading, dependency injection, or other dynamic
resolution strategies should apply the same principle whenever providers
create JavaFX runtime objects.

<h4 id="application-evolution">Application Evolution</h4>

Persisted layouts may outlive individual software releases.

Applications should therefore treat identifiers as long-lived contracts
between persisted layouts and runtime objects. When identifiers,
dockable types, menu structures, or other persisted concepts change,
applications should maintain backward compatibility where practical.

For example, if an identifier changes from `workspace` to `projects`,
the `DockableStateProvider` can continue accepting `workspace` and
return the newer dockable state. This allows previously saved layouts
to continue restoring successfully after an application upgrade.

The stored format carries a schema version of its own, and the framework
reads every version up to the one it writes. **The current version is 1**,
and it is the only version there has been, so it covers everything the stored
metadata holds, a layout's group and the group catalog included. A layout
written by a newer framework is refused by an older one, with a message naming
the version rather than a parse failure, so downgrading an application strands
the layouts saved by the newer one.

Providers are the primary mechanism for adapting persisted layouts to
application changes until explicit layout migration support is added.

<h2 id="where-to-go-next">Where to Go Next</h2>

Most applications need a [DockableStateProvider](providers.md) and nothing else on this list. The rest are for a specific need.

[Write the one provider an application must implement (and understand why a provider is needed at all)](providers.md)  
[Let users save, name, list, switch and delete layouts of their own](layouts.md)  
[Write a codec or a storage destination of your own](extending.md)  
[Follow what a save and a restore actually do, step by step, with sequence diagrams](implementation.md)  
