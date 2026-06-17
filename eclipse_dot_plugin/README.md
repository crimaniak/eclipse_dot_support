# Eclipse DOT Plugin

This plugin adds support for `*.dot` files rendered by [Graphviz](https://graphviz.org/).

## Features

- **Language support**: Syntax highlighting, auto-completion, and outline view for the DOT language.
- **Graph preview**: A dedicated window displays the rendered graph for the file currently open in the editor. The graph is re-rendered whenever a `.dot` file is opened or saved.

## Installation

1. Build the update site from the [eclipse_dot_support.site](../eclipse_dot_support.site/README.md) project:
   ```
   mvn clean package
   ```
2. In Eclipse, open **Help > Install New Software**.
3. Click **Add > Local** and select `eclipse_dot_support.site/target/repository/`.
4. Select **DOT Support** and complete the installation wizard.
5. Restart Eclipse when prompted.

## Requirements

Graphviz must be installed on your system. If Graphviz is not found, the graph preview feature is disabled.

## Configuration

Open **Preferences > DOT Support** to configure the plugin:

- **Graphviz path**: If the Graphviz executables are not on the system `PATH`, you can specify their location here.
- **Output format**: Choose the format used for the rendered image. SVG and PNG are fully supported; other formats may also work if they can be displayed in an Eclipse window.
