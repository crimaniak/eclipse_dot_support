# Eclipse DOT Support

This Eclipse feature packages the plugin from the [eclipse_dot_plugin](../eclipse_dot_plugin/README.md) directory.

## Features

### DOT Language Editor

The plugin registers itself as the default editor for `.dot` and `.gv` files and provides:

- **Syntax highlighting** — keywords, attributes, strings, and comments are colored distinctly.
- **Auto-completion** — context-aware suggestions for keywords (`graph`, `digraph`, `subgraph`, `node`, `edge`, `strict`), common attributes (`label`, `color`, `shape`, `style`, `rankdir`, and 40+ more), node shapes (20+ options), and edge styles.
- **Outline view** — hierarchical tree of graphs, subgraphs, nodes, and edges, updated as you edit.

### Graph Preview

A dedicated **DOT Graph View** window renders the graph and updates it whenever a `.dot` file is opened or saved.

- Rendered output can be displayed as **SVG** (with interactive pan/zoom) or **PNG**.
- **Zoom controls**: Zoom In, Zoom Out, and Fit to Window.
- **Save to disk**: export the rendered image as SVG or PNG.
- Falls back to a simple image label on systems without a browser widget.

### Graphviz Integration

Rendering is performed by the `dot` executable from the [Graphviz](https://graphviz.org/) package.

- Automatically detects Graphviz on the system `PATH`.
- Shows detected executable path and version in the Preferences page.
- Optional console view displaying the render commands and their output.

## Configuration

Open **Preferences > DOT Support** to configure:

- **Graphviz path** — specify the Graphviz installation directory if it is not on the system `PATH`.
- **Output format** — choose SVG or PNG for the rendered preview.
