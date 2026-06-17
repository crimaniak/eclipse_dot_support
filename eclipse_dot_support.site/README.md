# Eclipse DOT Support — Update Site

This project builds the P2 update site for the [Eclipse DOT Support](../eclipse_dot_support/README.md) feature.

## Installation

### Option 1: Command line + Eclipse

1. Run `mvn clean package` from the workspace root (or from this directory).
2. In Eclipse, open **Help > Install New Software**.
3. Click **Add > Local** and select `eclipse_dot_support.site/target/repository/`.
4. Select **DOT Support** and complete the installation wizard.

### Option 2: Maven from inside Eclipse (m2e)

Requires the [m2e](https://eclipse.dev/m2e/) plugin (bundled with Eclipse IDE for Java Developers).

1. Right-click the project (or the root parent pom) → **Run As > Maven build…**
2. Set Goals to `clean package` and click **Run**.
3. After the build finishes, follow steps 2–4 from Option 1.
