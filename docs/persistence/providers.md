# Provider Interfaces

[&larr; Back to the BentoFX Persistence guide](guide.md)

Applications supply provider implementations to save and restore docking layouts. These providers act as factories or lookup services for runtime objects that cannot be serialized directly.

> <span style="font-size: 1.5em;">💡</span>  `DockableStateProvider` is the only required provider interface for which the framework does not include a default implementation.

- [Inline Creation Versus a Provider](#inline-vs-provider)
- [The Provider Interfaces](#provider-interfaces)
  - [`BentoProvider`](#bentoprovider)
  - [`DockableStateProvider`](#dockablestateprovider)
  - [`DockableMenuFactoryProvider`](#dockablemenufactoryprovider)
  - [`DockContainerLeafMenuFactoryProvider`](#dockcontainerleafmenufactoryprovider)
  - [`StageIconImageProvider`](#stageiconimageprovider)
  - [`DockingLayoutPersistenceProvider`](#dockinglayoutpersistenceprovider)
  - [`LayoutCodecProvider`](#layoutcodecprovider)
  - [`LayoutStorageProvider`](#layoutstorageprovider)
- [Provider Responsibilities](#provider-responsibilities)
- [Provider Lifecycle](#provider-lifecycle)

<h2 id="inline-vs-provider">Inline Creation Versus a Provider</h2>

The basic demo creates dockables inline and hands them straight to the layout:

```java
// Creates a Dockable
private Dockable buildDockable(DockBuilding builder, int s, int i, String title) {
    Dockable dockable = builder.dockable();
    dockable.setTitle(title);
    dockable.setIconFactory(d -> makeIcon(s, i));
    dockable.setNode(new Label("<" + title + ":" + i + ">"));
    // ...
    return dockable;
}

// Creates Dockables inline and hands them straight to the layout
leafWorkspaceTools.addDockables(
    buildDockable(builder, 1, 0, "Workspace"),
    buildDockable(builder, 1, 1, "Bookmarks"),
    buildDockable(builder, 1, 2, "Modifications")
);
```

This works because the application is the only thing that ever needs a dockable, and it needs each dockable exactly once. Identity is implied by the position the Dockable is placed at the call site and nothing else can ask for "the Workspace dockable", because nothing recorded its identity or position.

The persistence demo inverts the direction. Instead of the application creating content and pushing it into a layout, the framework asks for content by identifier and the application answers:

```java
@Override
public Optional<DockableState> resolveDockableState(String id) {
    return DockableProperties.findByIdentifier(id)
            .map(this::buildDockableState);
}
```

Three consequences follow, and they are the substance of the difference:

1. **Identity must be explicit and stable.** The identifier is the only thing that survives a save, so a dockable has to be restorable using its identifier rather than its position. See [Choosing Stable Identifiers](guide.md#choosing-stable-identifiers).
2. **Construction must be deferred.** A provider is typically built during application startup, where the constructor may run on the JavaFX-Launcher thread and JavaFX components cannot be created. Build the state when `resolveDockableState` is called, not when the provider is constructed.
3. **State must be built fresh on each call, not cached.** A `DockableState` carries one node instance, and a JavaFX node has only one parent. Handing the same state to two restored components would move the node into the second layout (parent) and leave the first layout (parent) showing a blank panel. Nothing prevents a `Node` from being cached, but the `DockableState` must be built fresh on every call.

The demo's provider does all three: it looks the identifier up in `DockableProperties`, and builds a new `DockableState` per call through `DockableStateBuilder`.

> <span style="font-size: 1.5em;">💡</span>  Applications are not required to abandon inline creation. A persistent application can still build its initial layout inline, as long as the same content is also reachable by identifier through a provider when a saved layout is restored later.

<h2 id="provider-interfaces">The Provider Interfaces</h2>

| Provider | Must implement? | Framework implementation |
|----------|:---------------:|--------------------------|
| [`BentoProvider`](#bentoprovider) | No | `DefaultBentoProvider` |
| [`DockableStateProvider`](#dockablestateprovider) | **Yes** | None |
| [`DockableMenuFactoryProvider`](#dockablemenufactoryprovider) | Optional | None |
| [`DockContainerLeafMenuFactoryProvider`](#dockcontainerleafmenufactoryprovider) | Optional | None |
| [`StageIconImageProvider`](#stageiconimageprovider) | Optional | None |
| [`DockingLayoutPersistenceProvider`](#dockinglayoutpersistenceprovider) | No | `DefaultDockingLayoutPersistenceProvider` |
| [`LayoutCodecProvider`](#layoutcodecprovider) | No | `JsonLayoutCodecProvider`<br>`XmlLayoutCodecProvider` |
| [`LayoutStorageProvider`](#layoutstorageprovider) | No | `FileLayoutStorageProvider`<br>`DatabaseLayoutStorageProvider` |

Each interface is further described below, with where it is passed and what each implementation must do.

<h3 id="bentoprovider"><code>BentoProvider</code></h3>

`BentoProvider` supplies the `Bento` instances whose layouts should be saved and restored. It is a non-null parameter of both `DockingLayoutPersistenceProvider.getLayoutSaver` and `DockingLayoutPersistenceProvider.getLayoutRestorer`. The framework provides `DefaultBentoProvider`, which collects `Bento` instances against their identifiers and holds them by weak reference. Instances can be passed to the constructor or added later with `addBento`, so an application rarely needs its own implementation.

<h3 id="dockablestateprovider"><code>DockableStateProvider</code></h3>

`DockableStateProvider` is the one interface every persistent application must implement. It resolves a persisted `Dockable` identifier to a `DockableState`, from which the framework reconstructs the runtime `Dockable`. And it is a non-null parameter of `DockingLayoutPersistenceProvider.getLayoutRestorer` (it is not a parameter of `getLayoutSaver`, because saving needs no dockable resolution).

`DockableStateProvider` has a single method:

```java
Optional<DockableState> resolveDockableState(String id);
```

Whereas a persisted layout records *which* dockables were open, it never records *what* was inside them. So the restorer works through the saved identifiers and asks the application for each one in turn. An empty `Optional` means the identifier cannot be reconstructed, and the restorer continues without that dockable.

It is important to recognize that the returned `DockableState` is not itself a `Dockable`. The framework builds the `Dockable` with `DockBuilding.dockable(identifier)` and applies values carried by the `DockableState` to it, leaving unspecified attributes at their defaults. All `DockableState` attributes are optional:

| Attribute                                       | Kind                                                              |
|-------------------------------------------------|-------------------------------------------------------------------|
| `title`<br/>`tooltip`<br/>`dragGroupMask`<br/>`isClosable` | Plain text, copied onto the dockable                              |
| `dockableNode`                                  | A live JavaFX `Node`, handed to the dockable and then owned by it |
| `dockableIconFactory`<br/>`dockableMenuFactory`      | Live factories the dockable calls as needed                       |
| `dockableConsumer`                              | A live `Consumer<Dockable>` applied to the finished dockable      |

As mentioned, every carried value is optional, **including the node**. A state with a title, tooltip and icon but no `dockableNode` produces a restored tab that shows those, with `placeholderBuilding()` supplying content that would have otherwise been occupied by the node. 

Carrying a live node rather than instructions for building one has two consequences that affect how a provider is written:

1. A state cannot be created before JavaFX is ready.
2. A state instance can be used exactly once.

Both are covered under [Provider Responsibilities](#provider-responsibilities).

<h3 id="dockablemenufactoryprovider"><code>DockableMenuFactoryProvider</code></h3>

`DockableMenuFactoryProvider` looks up a `DockableMenuFactory` by identifier, for application code building a `DockableState`. `DockableMenuFactoryProvider` is not a parameter of either `DockingLayoutPersistenceProvider.getLayoutSaver` or `DockingLayoutPersistenceProvider.getLayoutRestorer`, and no persistence code calls it. 

`DockableMenuFactoryProvider` is the one provider the framework never uses. A restored dockable takes its context menu from `DockableState.dockableMenuFactory`, which the application sets while building the state. The `DockableMenuFactoryProvider` interface exists because looking up a menu factory by identifier is a common thing to need at that point. But an application can set the factory directly and never implement it. The persistence demo does implement one, and passes it to `BoxAppDockableStateProvider` as a `@Nullable` constructor argument.

<h3 id="dockcontainerleafmenufactoryprovider"><code>DockContainerLeafMenuFactoryProvider</code></h3>

`DockContainerLeafMenuFactoryProvider` supplies `DockContainerLeafMenuFactory` instances when restored leaves need context menus, and it is a `@Nullable` parameter of `DockingLayoutPersistenceProvider.getLayoutRestorer`. When restored leaves need no context menus, pass a `null` `DockContainerLeafMenuFactoryProvider` to `DockingLayoutPersistenceProvider.getLayoutRestorer`.

<h3 id="stageiconimageprovider"><code>StageIconImageProvider</code></h3>

`StageIconImageProvider` supplies stage icons for restored drag/drop stages, and it is a `@Nullable` parameter of `DockingLayoutPersistenceProvider.getLayoutRestorer`. When restored drag/drop stages need no icon, pass a `null` `StageIconImageProvider` to `DockingLayoutPersistenceProvider.getLayoutRestorer`.

<h3 id="dockinglayoutpersistenceprovider"><code>DockingLayoutPersistenceProvider</code></h3>

`DockingLayoutPersistenceProvider` supplies the application-facing `LayoutSaver` and `LayoutRestorer`, and is the primary interface for interacting with the persistence framework. `DockingLayoutPersistenceProvider` is not a parameter of anything; it is the object whose methods an application calls. 

`DefaultDockingLayoutPersistenceProvider` can be constructed directly:

```java
final DockingLayoutPersistenceProvider persistenceProvider =
        new DefaultDockingLayoutPersistenceProvider();
```

It uses `ServiceLoader` to discover `LayoutCodecProvider` and `LayoutStorageProvider` implementations available on the runtime module path, or on the classpath for a non-modularized application. Selection is deterministic: an explicitly requested provider identifier wins, otherwise a single available provider is used, otherwise a single provider marked as default is used. An ambiguous configuration fails with a `BentoStateException` listing the available provider identifiers.

Once acquired, `DockingLayoutPersistenceProvider` hands back savers and restorers. The simplest form uses the codec and storage providers selected from runtime dependencies:

```java
final LayoutSaver layoutSaver = persistenceProvider.getLayoutSaver(
        layoutIdentifier,
        bentoProvider
);

final LayoutRestorer layoutRestorer = persistenceProvider.getLayoutRestorer(
        layoutIdentifier,
        bentoProvider,
        dockableStateProvider,
        stageIconImageProvider,               // Nullable
        dockContainerLeafMenuFactoryProvider  // Nullable
);
```

An application including multiple codec or storage implementations selects specific providers with a `LayoutPersistenceProfile`:

```java
final LayoutPersistenceProfile profile = new LayoutPersistenceProfile(
        layoutIdentifier,
        "json",            // codec identifier
        "file"             // storage identifier
);

final LayoutSaver layoutSaver = persistenceProvider.getLayoutSaver(
        profile,
        bentoProvider
);
```

`LayoutPersistenceProfile` makes dependency-only replacement possible while still allowing an application to save and restore several layouts through different codec or storage providers.

<h3 id="layoutcodecprovider"><code>LayoutCodecProvider</code></h3>

`LayoutCodecProvider` supplies the codec used to encode and decode persisted layout state, and exposes a stable provider identifier. A `LayoutCodecProvider` must be present, but is never passed in. It is discovered from the runtime dependencies on the module path )or classpath for non-modular applications), so an application including exactly one codec need not name it. A `LayoutCodecProvider` can be named explicitly with a `LayoutPersistenceProfile` when more than one is present.

Two `LayoutCodecProvider` implementations are bundled: `JsonLayoutCodecProvider` in `persistence-codec-json` and `XmlLayoutCodecProvider` in `persistence-codec-xml`. Writing another is covered in [Extending Persistence](extending.md#adding-a-codec).

<h3 id="layoutstorageprovider"><code>LayoutStorageProvider</code></h3>

`LayoutStorageProvider` supplies the storage destination used to read and write persisted layout state, and exposes a stable provider identifier. A `LayoutStorageProvider` must be present, but it is never passed in. The `LayoutStorageProvider` is discovered from the runtime dependencies on the module path (or classpath for non-modular applications), so an application including exactly one storage need not name it. A `LayoutStorageProvider` can be named explicitly with a `LayoutPersistenceProfile` when more than one is present.

Two `LayoutStorageProvider` implementations are bundled: `FileLayoutStorageProvider` in `persistence-storage-file` and `DatabaseLayoutStorageProvider` in `persistence-storage-db-h2`. Writing another is covered in [Extending Persistence](extending.md#adding-a-storage-destination).

<h2 id="provider-responsibilities">Provider Responsibilities</h2>

As previously described, the persistence framework restores the BentoFX layout structure and uses application providers to restore application-specific runtime objects that cannot be serialized safely. The providers can create objects statically, dynamically, eagerly, lazily, through dependency injection, or by any other mechanism. The important requirement is that a persisted identifier must resolve to the same kind of runtime object whenever the layout is restored.

When providers create JavaFX objects, they must follow JavaFX threading rules. The persistence demo's `DockableState` values contain JavaFX controls, and a JavaFX `Application` constructor runs on the JavaFX-Launcher thread, so the demo builds them on first use rather than during construction. Both callers of `resolveDockableState` - the application while it starts, and the restorer through the persistence framework - are already on the JavaFX Application Thread:

```java
@Override
public Optional<DockableState> resolveDockableState(String id) {
    return DockableProperties.findByIdentifier(id)
            .map(this::buildDockableState);
}
```

Scheduling the same work with `Platform.runLater(...)` from a constructor also puts it on the right thread, but it leaves nothing resolvable until the queued task runs, so every lookup then depends on the order in which JavaFX drains its queue. Building on demand removes that question.

<h2 id="provider-lifecycle">Provider Lifecycle</h2>

Provider implementations are typically application singletons or long-lived services created during startup. Applications generally create providers before layout restoration and reuse them for the lifetime of the application. Providers should not assume that layout restoration occurs only once. And a provider may be called repeatedly whenever layouts are restored, when a default layout is created, or when users switch saved layouts.
