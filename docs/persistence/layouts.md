# Managing Layouts

[&larr; Back to the BentoFX Persistence guide](guide.md)

> <span style="font-size: 1.5em;">💡</span> Everything described here is optional. An application that keeps one layout, restored at startup and saved on exit, needs nothing described here - see [Restoring the Layout](guide.md#restoring-the-layout) and [Saving the Layout](guide.md#saving-the-layout) instead.

- [A Ready-Made Layouts Menu](#layouts-menu) is a drop-in `Menu` providing user facing functionality for managing persisted layouts. It also allows application developers the ability to [change the text](#layouts-menu-text) to support specific application requirements.

- [Managing Multiple Layouts](#managing-several-layouts) describes layout naming restrictions, modifying the location where layouts are persisted, and separating layouts from different BentoFX persistence enabled applications running on the same machine.

<h2 id="layouts-menu">A Ready-Made Layouts Menu</h2>

To build a `Menu`, an application needs the ability to list, filter, and name layouts. The persistence module provides these capabilities. It also provides as the ready-made `LayoutsMenu` that can be used wherever a JavaFX `Menu` can be used - a `MenuBar`, a `Window` menu, or a context menu:

```java
windowMenu.getItems().add(new LayoutsMenu(owner, dockingLayoutRestorable));
```

where `owner` is the `Window` to which dialogs raised by `LayoutMenu` actions belong and `dockingLayoutRestorable` is usually the application, but can be any `DockingLayoutRestorable` implementation whose docking layout these
`LayoutMenu` actions switch and whose providers can be used to read and write layouts.

The `LayoutMenu` provides the ability to:

* Restore the default layout
* Restore layouts a user has saved
* Save layouts
* Rename layouts
* Delete layouts
* Group layouts
* Manage layout groups.

A check mark identifies whichever layout is showing. And the menu rebuilds itself each time it opens, so the list and the mark updates itself as the application runs.

As indicated above, the `LayoutMenu` lets users organize their layouts into groups: `Groups > New Group...`, `Rename Group`, and `Delete Group`, with `Move to Group` on each saved layout. Groups appear as submenus wherever layouts are listed, and a group holding the layout on screen is marked so finding it does not require opening each one.

A group created this way must be created before any layouts can be added to it. And a group survives its last layout being moved out. Deleting a group keeps its layouts and leaves them in ungrouped.

<h3 id="docking-layout-restorable">The `DockingLayoutRestorable` Interface</h3>

The second of the two arguments used to create a `LayoutMenu` is a `DockingLayoutRestorable` implementation, which is usually the application itself.

```java
public class MyApp extends Application implements DockingLayoutRestorable {

    @Override
    public DockingLayout getDefaultDockingLayout() { ... }

    @Override
    public DockingLayout getDockingLayout(
            LayoutPersistenceProfile profile,
            Supplier<DockingLayout> fallbackLayoutSupplier) { ... }

    @Override
    public boolean switchToLayout(
            Supplier<DockingLayout> dockingLayoutSupplier) { ... }

    @Override
    public DockingLayoutPersistenceProvider getPersistenceProvider() {
        return persistenceProvider;
    }

    @Override
    public BentoProvider getBentoProvider() {
        return bentoProvider;
    }
}
```

The two providers are on the interface rather than passed to the menu separately because an implementation cannot do without them anyway - reading a stored layout means calling `getLayoutRestorer` with a `BentoProvider`, thus anything able to implement `getDockingLayout` already holds both.

`switchToLayout` takes a `Supplier` rather than a `DockingLayout` because reading the layout is part of the switch. An implementation has to stop whatever is saving the arrangement on screen before anything reads a replacement, so it is the implementation that decides when the supplier runs. It returns `false` when nothing was applied, and leaves reporting the result of the switch to the `LayoutMenu`.

The persistence demo's `BoxApp` implements all five methods.

<h3 id="layouts-menu-text">Changing the Text</h3>

All `LayoutMenu` text comes from a `ResourceBundle` or names provided by the user when saving layouts and layout groups. To offer menu text in another language, add `LayoutsMenu_<language>.properties` beside [the existing properties file](../../persistence/core/src/main/resources/software/coley/bentofx/persistence/core/ui/LayoutsMenu.properties) in the `persistence-core` module's resource folder. Java reads these files as UTF-8, so write the target language directly.

To supply text from the application instead, hand over a bundle:

```java
new LayoutsMenu(stage, application, ResourceBundle.getBundle(
        "com.example.myapp.LayoutsMenuTexts", Locale.FRENCH));
```

That bundle needs its own base name, in the application's own package. A `LayoutsMenu_fr.properties` placed in the application's module will not be found: resources in a named module are not visible to another, so the framework's own `getBundle` call cannot see it. Loading it from a class in the module that holds it is what makes it reachable.

A substituted bundle replaces the framework's own rather than falling back to it, so it has to carry every key. A missing one raises `MissingResourceException` the first time the menu opens.

Two things are worth knowing about the values:

1. **Three of them are `MessageFormat` patterns** - the ones holding `{0}`, which is the layout name. In those three only, a literal apostrophe has to be doubled and `{0}` must not be quoted, or the name is dropped. Everywhere else an apostrophe is just an apostrophe.
2. **Mnemonics live in the text.** An underscore marks the following letter, so a translation chooses its own, and has to keep them distinct within one menu. The items naming saved layouts carry no mnemonics because the user names them, and mnemonics are not parsed parsed from user provided names. So, an underscore in a layout or layout group name shows as an underscore.

<h2 id="managing-several-layouts">Managing Layouts from Multiple BentoFX Persistence Enabled Applications</h2>

An application usually keeps one layout that follows the session, saved automatically and restored at startup. This layout has a reserved identifier, `LayoutIdentifiers.SESSION_LAYOUT_IDENTIFIER`, so that an application does not spell the name out and a user cannot take it for a layout of their own:

```java
final LayoutPersistenceProfile sessionProfile =
        LayoutPersistenceProfile.of(LayoutIdentifiers.SESSION_LAYOUT_IDENTIFIER);
```

Reserved is not the same as invalid. Every operation accepts it, because saving to it, restoring it, and deleting it (a "reset to defaults") are all things an application legitimately does. What the reservation means is that `LayoutIdentifiers.isReserved(...)` refuses it where a user chose the name, and that a menu of layouts a user may restore leaves it out.

Letting users keep layouts of their own means naming them, listing them, and removing them, and the persistence provider provides the ability to perform each of these tasks:

```java
final LayoutPersistenceProfile profile = LayoutPersistenceProfile.of("review-layout");

// Save the layout showing now, under this name. One write, nothing left running.
persistenceProvider.saveLayout(profile, bentoProvider);

// Populate a menu. The session layout is in here too, so filter it out.
final List<String> storedLayouts =
        persistenceProvider.getStoredLayoutIdentifiers(profile);

// Use these to warn before replacing, and remove on request.
final boolean wouldReplace = persistenceProvider.isLayoutStored(profile);
final boolean wasRemoved = persistenceProvider.deleteLayout(profile);
```

<h3 id="where-layouts-are-stored">Where Layouts Are Stored</h3>

In addition to naming persisted layouts, both the location layout and a namespace within it can be customized. Giving an application its own namespace lets several BentoFX-based applications run on the same machine while separating layouts so one application's layouts neither collide with nor overwrite another's.

By default, the bundled `persistence-storage-file` and `persistence-storage-db-h2` providers keep their data under `<user.home>/.bentofx`, with no namespace. Both resolve their location through `LayoutStorageLocations`, which reads two settings on every call. Either can be given as a `System` property or as an environment variable of the matching name, and the property takes precedence when both are set:

| System property | Environment variable | Effect |
|-----------------|----------------------|--------|
| `bentofx.persistence.home` | `BENTOFX_PERSISTENCE_HOME` | Replaces the base directory `<user.home>/.bentofx`. |
| `bentofx.persistence.namespace` | `BENTOFX_PERSISTENCE_NAMESPACE` | Creates a namespace within the resolved home for the application. |

The environment variable form needs no application code: set it before the process starts, the way `JAVA_HOME` does, and the next storage provider to resolve its location picks it up. In code, `LayoutStorageLocations.configureHome(Path)` and `configureNamespace(String)` are typed alternatives to setting the properties directly:

```java
LayoutStorageLocations.configureNamespace("my-app");

final DockingLayoutPersistenceProvider persistence =
        DockingLayoutPersistence.provider();
```

Order matters. Whichever way these are set, it has to happen before the first save, restore, or query, because that's when a storage provider reads their values. In practice that means before `DockingLayoutPersistence.provider()` is first called. The persistence demo does exactly this in [Runner.java](../../demos/persistence/src/main/java/software/coley/bentofx/demo/persistence/Runner.java).

A namespace separates one application's layouts from another's, not how a user's own layouts are separated from each other (which is referred to as a group). Layouts are distinguished by their identifiers, and grouped for display with [layout groups](#managing-several-layouts).

See [Configuring Storage Location](guide.md#configuring-storage-location) in the guide for the reasons an application would change either setting.
