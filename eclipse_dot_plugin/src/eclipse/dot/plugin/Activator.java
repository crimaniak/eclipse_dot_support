package eclipse.dot.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "eclipse_dot_plugin";

    private static Activator plugin;
    private static String navScript = "";

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        navScript = loadResource("/eclipse/dot/plugin/view/nav_script.js");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    public static Activator getDefault() {
        return plugin;
    }

    public static String getNavScript() {
        return navScript;
    }

    private String loadResource(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                Platform.getLog(getClass()).error("NAV_SCRIPT resource not found: " + path);
                return "";
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Platform.getLog(getClass()).error("Failed to load NAV_SCRIPT resource: " + path, e);
            return "";
        }
    }
}
