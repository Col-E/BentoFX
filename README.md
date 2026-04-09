# BentoFX

A docking system for JavaFX.

## Table of Contents

* [Requirements](#requirements)
* [Core Framework](#core-module)
  * [Usage](#core-usage)
    * [Gradle (Groovy DSL)](#core-gradle-groovy-dsl)
    * [Gradle (Kotlin DSL)](#core-gradle-kotlin-dsl)
    * [Maven](#core-maven)
  * [Overview](#overview)
    * [Containers](#containers)
    * [Controls](#controls)
    * [Dockables](#dockables)
  * [Example](#example)
    * [Construct the Default Docking Layout](#construct-the-default-layout)
    * [Show the Layout](#show-it)
* [Demo Application](#demo-application)

## Requirements

- JavaFX 23+
- Java 21+

## Core Module

The [core](./core) module is a framework of user interface controls that can be used to group, dock, and undock other user interface controls using drag and drop. 

<h3 id="core-usage">Usage</h3>

<h4 id="core-gradle-groovy-dsl">Gradle (Groovy DSL)</h4>

```groovy
implementation 'software.coley.bentofx:core:${version}'
```

<h4 id="core-gradle-kotlin-dsl">Gradle (Kotlin DSL)</h4>

```kotlin
implementation("software.coley.bentofx:core:${version}")
```

<h4 id="core-maven">Maven</h4>

```xml
<dependency>
    <groupId>software.coley.bentofx</groupId>
    <artifactId>core</artifactId>
    <version>${version}</version>
</dependency>
```

### Overview

![overview](assets/overview.png)

In terms of hierarchy, the `Node` structure of Bento goes like:

- `DockContainerRootBranch`
    - `DockContainerBranch` _(Nesting levels depends on which kind of implementation used)_
        - `DockContainerLeaf`
            - `Dockable` _(Zero or more)_

Each level of `*DockContainer` in the given hierarchy and `Dockable` instances can be constructed via a `Bento`
instance's builder offered by `bento.dockBuilding()`.

#### Containers

![containers](assets/containers.png)

Bento has a very simple model of branches and leaves. Branches hold additional child containers. Leaves
display `Dockable` items and handle drag-n-drop operations.

| Container type        | Description                                                                                                                                                             |
|-----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `DockContainerBranch` | Used to show multiple child `DockContainer` instances in a `SplitPane` display. Orientation and child node scaling are thus specified the same way as with `SplitPane`. |
| `DockContainerLeaf`   | Used to show any number of `Dockable` instance rendered by a `HeaderPane`.                                                                                              |

#### Controls

![controls](assets/controls.png)

Bento comes with a few custom controls that you will want to create a custom stylesheet for to best fit the intended
look and feel of your application.

An example reference sheet _(which is included in the dependency)_ can be found
in [`bento.css`](core/src/main/resources/bento.css).

| Control                     | Description                                                                                                                                       |
|-----------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| `Header`                    | Visual model of a `Dockable`.                                                                                                                     |
| `HeaderPane`                | Control that holds multiple `Header` children, and displays the currently selected `Header`'s associated `Dockable` content.                      |
| `Headers`                   | Child of `HeaderPane` that acts as a `HBox`/`VBox` holding multiple `Headers`.                                                                    |
| `ButtonHBar` / `ButtonVBar` | Child of `HeaderPane` used to show buttons for the `DockContainerLeaf` for things like context menus and selection of overflowing `Header` items. |

#### Dockables

The `Dockable` can be thought of as the model behind each of a `HeaderPane`'s `Header` _(Much like a `Tab` of
a `TabPane`)_.
It outlines capabilities like whether the `Header` can be draggable, where it can be dropped, what text/graphic to
display,
and the associated JavaFX `Node` to display when placed into a `DockContainerLeaf`.

### Example
![containers](assets/example.png)

In this example we create a layout structure that loosely models how an IDE is laid out.
There are tool-tabs on the left and bottom sides. The primary content like Java sources files
reside in the middle and occupy the most space. The tool tabs are intended to be smaller and not
automatically scale when we resize the window since we want the primary content to take up the available space when possible.

We'll first create a vertically split container and put tools like logging/terminal at the bottom.
The bottom section will be set to not resize with the parent for the reason mentioned previously.

The top of the vertical split will hold our primary docking leaf container and the remaining tools.
The tools will go on the left, and the main container on the right via a horizontally split container.
The first item in this horizontal split will show up on the left, so that's where we'll put the tools.
Then the second item will be our primary docking container.

Our primary docking container is a glorified tab-pane, and we'll fill it up with some dummy items as if we
were in the midst of working on some project. These tabs won't have any special properties,
but we'll want to make sure the tools have some additional values set.

All tool tabs will be constructed such that they are not closable and all belong to a shared
drag group called `TOOLS`. Since these tabs all have a shared group they can be dragged
amongst one another. However, the primary docking container tabs with our _"project files"_ cannot be
dragged into the areas housing our tools. If you try this out in IntelliJ you'll find it
follows the same behavior.

#### Construct the default layout
***BoxApp#constructDefaultDockContainerRootBranch()***
```java
DockContainerRootBranch branchRoot = builder.root("root");
DockContainerBranch branchWorkspace = builder.branch("workspace");
DockContainerLeaf leafWorkspaceTools = builder.leaf("workspace-tools");
DockContainerLeaf leafWorkspaceHeaders = builder.leaf("workspace-headers");
DockContainerLeaf leafTools = builder.leaf("misc-tools");

branchWorkspace.setPruneWhenEmpty(false);
leafWorkspaceTools.setPruneWhenEmpty(false);
leafTools.setPruneWhenEmpty(false);
leafTools.setPruneWhenEmpty(false);

// Add dummy menus to each.
dockContainerLeafMenuFactoryProvider.createDockContainerLeafMenuFactory(
  leafTools
).ifPresent(leafTools::setMenuFactory);

dockContainerLeafMenuFactoryProvider.createDockContainerLeafMenuFactory(
  leafWorkspaceHeaders
).ifPresent(leafWorkspaceHeaders::setMenuFactory);

dockContainerLeafMenuFactoryProvider.createDockContainerLeafMenuFactory(
  leafWorkspaceTools
).ifPresent(leafWorkspaceTools::setMenuFactory);

// These leaves shouldn't auto-expand. They are intended to be a set size.
SplitPane.setResizableWithParent(leafTools, false);
SplitPane.setResizableWithParent(leafWorkspaceTools, false);

// Root: Workspace on top, tools on bottom
// Workspace: Explorer on left, primary editor tabs on right
branchRoot.setOrientation(Orientation.VERTICAL);
branchWorkspace.setOrientation(Orientation.HORIZONTAL);
branchRoot.addContainers(branchWorkspace, leafTools);
branchWorkspace.addContainers(leafWorkspaceTools, leafWorkspaceHeaders);

// Changing tool header sides to be aligned with application's far edges (to facilitate better collapsing UX)
leafWorkspaceTools.setSide(Side.LEFT);
leafTools.setSide(Side.BOTTOM);

// Tools shouldn't allow splitting (mirroring IntelliJ behavior)
leafWorkspaceTools.setCanSplit(false);
leafTools.setCanSplit(false);

// Primary editor space should not prune when empty
leafWorkspaceHeaders.setPruneWhenEmpty(false);

// Set intended sizes for tools (leaf does not need to be a direct child, just some level down in the chain)
branchRoot.setContainerSizePx(leafTools, 200);
branchRoot.setContainerSizePx(leafWorkspaceTools, 300);

// Make the bottom collapsed by default
branchRoot.setContainerCollapsed(leafTools, true);

// Add dockables to leafWorkspaceTools
addDockable(WORKSPACE_DOCKABLE_ID, leafWorkspaceTools);
addDockable(BOOKMARKS_DOCKABLE_ID, leafWorkspaceTools);
addDockable(MODIFICATIONS_DOCKABLE_ID, leafWorkspaceTools);

// Add dockables to leafTools
addDockable(LOGGING_DOCKABLE_ID, leafTools);
addDockable(TERMINAL_DOCKABLE_ID, leafTools);
addDockable(PROBLEMS_DOCKABLE_ID, leafTools);

// Add dockables to leafWorkspaceHeaders
addDockable(CLASS_1_DOCKABLE_ID, leafWorkspaceHeaders);
addDockable(CLASS_2_DOCKABLE_ID, leafWorkspaceHeaders);
addDockable(CLASS_3_DOCKABLE_ID, leafWorkspaceHeaders);
addDockable(CLASS_4_DOCKABLE_ID, leafWorkspaceHeaders);
addDockable(CLASS_5_DOCKABLE_ID, leafWorkspaceHeaders);

return branchRoot;
```

#### Show it

```java
Scene scene = new Scene(branchRoot);
scene.getStylesheets().add("/bento.css");
stage.setScene(scene);
stage.setOnHidden(e -> System.exit(0));
stage.show();
````
For a more real-world example you can check out [Recaf](https://github.com/Col-E/Recaf/)

![containers](assets/example-recaf.png)

## Demo Application
The [basic-demo](./demos/basic-demo) module contains an example application that demonstrates using the [core](./core) framework to create a layout structure that loosely models how an IDE is laid out.  

To run the basic demo, use `./gradlew :demos:basic-demo:run`

For details, refer to `BoxApp.getDefaultLayout()`.
