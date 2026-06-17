package eclipse.dot.plugin.renderer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import eclipse.dot.plugin.Activator;
import eclipse.dot.plugin.preferences.DotPreferenceConstants;

public class GraphvizRenderer {

    private static final String CONSOLE_NAME = "DOT Renderer";

    public static String render(String dotFilePath) throws IOException, InterruptedException {
        String format = Activator.getDefault().getPreferenceStore()
            .getString(DotPreferenceConstants.OUTPUT_FORMAT);
        if (format == null || format.isEmpty()) {
            format = DotPreferenceConstants.FORMAT_SVG;
        }

        String outputPath = dotFilePath + "." + format;
        String dotExecutable = findDotExecutable();

        // Console is optional — never let it prevent rendering
        MessageConsoleStream out = null;
        try {
            MessageConsole console = getOrCreateConsole();
            if (console != null) {
                showConsole(console);
                out = console.newMessageStream();
            }
        } catch (Throwable t) {
            Platform.getLog(GraphvizRenderer.class).warn("DOT console unavailable: " + t);
        }

        try {
            String cmdLine = dotExecutable + " -T" + format
                + " \"" + dotFilePath + "\" -o \"" + outputPath + "\"";
            if (out != null) out.println("$ " + cmdLine);

            ProcessBuilder pb = new ProcessBuilder(
                dotExecutable, "-T" + format, dotFilePath, "-o", outputPath);
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            Process process = pb.start();

            String stderr;
            try (InputStream es = process.getErrorStream()) {
                stderr = new String(es.readAllBytes());
            }
            int exitCode = process.waitFor();

            if (!stderr.isBlank() && out != null) out.println(stderr.trim());

            if (exitCode == 0 && new File(outputPath).exists()) {
                if (out != null) out.println("Done: " + outputPath);
                return outputPath;
            }

            String msg = "Graphviz failed (exit " + exitCode + ")"
                + (stderr.isBlank() ? "" : ": " + stderr.trim());
            if (out != null) out.println(msg);
            throw new IOException(msg);

        } finally {
            if (out != null) try { out.close(); } catch (IOException ignore) {}
        }
    }

    /** Returns the resolved executable path, or null if graphviz cannot be found. */
    public static String getDetectedExecutablePath() {
        String prefPath = Activator.getDefault().getPreferenceStore()
            .getString(DotPreferenceConstants.GRAPHVIZ_PATH);
        if (prefPath != null && !prefPath.isEmpty()) {
            for (String name : new String[]{ "dot", "dot.exe" }) {
                File f = new File(prefPath, name);
                if (f.exists()) return f.getAbsolutePath();
            }
        }
        for (String whichCmd : new String[]{ "which", "where" }) {
            try {
                ProcessBuilder pb = new ProcessBuilder(whichCmd, "dot");
                pb.redirectErrorStream(true);
                Process p = pb.start();
                String result;
                try (InputStream is = p.getInputStream()) {
                    result = new String(is.readAllBytes()).trim();
                }
                if (p.waitFor() == 0 && !result.isEmpty()) return result;
            } catch (Exception ignore) {}
        }
        return null;
    }

    /** Returns the version string from the given dot executable, or null on failure. */
    public static String getVersion(String execPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(execPath, "-V");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String output;
            try (InputStream is = p.getInputStream()) {
                output = new String(is.readAllBytes()).trim();
            }
            p.waitFor();
            return output.isEmpty() ? null : output;
        } catch (Exception e) {
            return null;
        }
    }

    private static String findDotExecutable() {
        String prefPath = Activator.getDefault().getPreferenceStore()
            .getString(DotPreferenceConstants.GRAPHVIZ_PATH);
        if (prefPath != null && !prefPath.isEmpty()) {
            for (String name : new String[]{ "dot", "dot.exe" }) {
                File f = new File(prefPath, name);
                if (f.exists()) return f.getAbsolutePath();
            }
        }
        return "dot";
    }

    private static MessageConsole getOrCreateConsole() {
        try {
            ConsolePlugin cp = ConsolePlugin.getDefault();
            if (cp == null) return null;
            IConsoleManager cm = cp.getConsoleManager();
            for (IConsole c : cm.getConsoles()) {
                if (CONSOLE_NAME.equals(c.getName()) && c instanceof MessageConsole) {
                    return (MessageConsole) c;
                }
            }
            MessageConsole console = new MessageConsole(CONSOLE_NAME, null);
            cm.addConsoles(new IConsole[]{ console });
            return console;
        } catch (Throwable t) {
            return null;
        }
    }

    private static void showConsole(MessageConsole console) {
        PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
            try {
                IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                if (window == null) return;
                IWorkbenchPage page = window.getActivePage();
                if (page == null) return;
                IConsoleView view = (IConsoleView) page.showView(
                    IConsoleConstants.ID_CONSOLE_VIEW, null, IWorkbenchPage.VIEW_VISIBLE);
                view.display(console);
            } catch (Throwable t) {
                // console view not available
            }
        });
    }
}
