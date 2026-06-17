package eclipse.dot.plugin.view;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;

import org.eclipse.core.runtime.Platform;
import eclipse.dot.plugin.Activator;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

public class DotGraphView extends ViewPart {

    public static final String VIEW_ID = "eclipse.dot.plugin.view.DotGraphView";

    // -------------------------------------------------------------------------
    // HTML template
    // -------------------------------------------------------------------------

    // Navigation JS is loaded from nav_script.js at plugin start (see Activator).
    // Layout contract: #vp = full-viewport container, #ct = transformed content wrapper.
    private static String buildHtml(String bodyContent) {
        return "<!DOCTYPE html><html><head><style>" +
            "*{margin:0;padding:0;box-sizing:border-box}" +
            "html,body{width:100%;height:100%;overflow:hidden;background:#fff}" +
            "#vp{width:100%;height:100%;overflow:hidden;position:relative;" +
            "cursor:grab;user-select:none;-webkit-user-select:none}" +
            "#ct{position:absolute;top:0;left:0;transform-origin:0 0;will-change:transform}" +
            "#ct svg,#ct img{display:block}" +
            "</style></head><body>" +
            "<div id='vp'><div id='ct'>" + bodyContent + "</div></div>" +
            "<script>" + Activator.getNavScript() + "</script>" +
            "</body></html>";
    }

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private Composite container;
    private Browser browser;
    private Label imageLabel;
    private ImageData originalImageData;
    private boolean hasBrowser;

    private String lastRenderedFile;
    private double currentZoom = 1.0; // used only by the SWT-label fallback path

    // -------------------------------------------------------------------------
    // View lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void createPartControl(Composite parent) {
        container = new Composite(parent, SWT.NONE);
        container.setLayout(new FillLayout());

        try {
            browser = new Browser(container, SWT.NONE);
            browser.setText(buildHtml(
                "<p style='font-family:sans-serif;color:#888;font-size:14px;" +
                "position:absolute;top:50%;left:50%;transform:translate(-50%,-50%)'>" +
                "Save a .dot file to render the graph here.</p>"));
            hasBrowser = true;
        } catch (Throwable t) {
            Platform.getLog(getClass()).warn("DOT browser unavailable, using label fallback: " + t);
            hasBrowser = false;
            browser = null;
            container.setLayout(new GridLayout());
            imageLabel = new Label(container, SWT.CENTER | SWT.WRAP);
            imageLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            imageLabel.setText("Save a .dot file to render the graph here.");
        }

        createToolbarActions();
    }

    private void createToolbarActions() {
        IActionBars bars = getViewSite().getActionBars();
        IToolBarManager toolbar = bars.getToolBarManager();

        Action saveAction = new Action("Save") {
            @Override public void run() { saveFile(); }
        };
        saveAction.setToolTipText("Save rendered file to disk");
        saveAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
            .getImageDescriptor(ISharedImages.IMG_ETOOL_SAVEAS_EDIT));

        Action zoomInAction = new Action("+") {
            @Override public void run() { zoomBy(1.25); }
        };
        zoomInAction.setToolTipText("Zoom In");

        Action zoomOutAction = new Action("−") {
            @Override public void run() { zoomBy(1.0 / 1.25); }
        };
        zoomOutAction.setToolTipText("Zoom Out");

        Action zoomFitAction = new Action("Fit") {
            @Override public void run() { zoomFit(); }
        };
        zoomFitAction.setToolTipText("Zoom to Fit");

        toolbar.add(saveAction);
        toolbar.add(new Separator());
        toolbar.add(zoomInAction);
        toolbar.add(zoomOutAction);
        toolbar.add(zoomFitAction);
    }

    @Override
    public void setFocus() {
        if (hasBrowser && browser != null && !browser.isDisposed()) {
            browser.setFocus();
        } else if (imageLabel != null && !imageLabel.isDisposed()) {
            imageLabel.setFocus();
        }
    }

    @Override
    public void dispose() {
        if (imageLabel != null && !imageLabel.isDisposed()) {
            Image img = imageLabel.getImage();
            if (img != null && !img.isDisposed()) img.dispose();
        }
        super.dispose();
    }

    // -------------------------------------------------------------------------
    // Save
    // -------------------------------------------------------------------------

    private void saveFile() {
        if (lastRenderedFile == null) return;
        boolean isSvg = lastRenderedFile.endsWith(".svg");
        FileDialog dialog = new FileDialog(container.getShell(), SWT.SAVE);
        dialog.setFileName(new File(lastRenderedFile).getName());
        dialog.setFilterExtensions(isSvg
            ? new String[]{ "*.svg", "*.*" }
            : new String[]{ "*.png", "*.*" });
        dialog.setFilterNames(isSvg
            ? new String[]{ "SVG files (*.svg)", "All files (*.*)" }
            : new String[]{ "PNG files (*.png)", "All files (*.*)" });
        dialog.setOverwrite(true);
        String dest = dialog.open();
        if (dest != null) {
            try {
                Files.copy(Path.of(lastRenderedFile), Path.of(dest),
                    StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                setError("Save failed: " + e.getMessage());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Zoom — toolbar buttons delegate to JS (browser) or SWT image (fallback)
    // -------------------------------------------------------------------------

    private void zoomBy(double factor) {
        if (hasBrowser && browser != null && !browser.isDisposed()) {
            browser.execute("nav.zoomBy(" + factor + ");");
        } else if (imageLabel != null && !imageLabel.isDisposed() && originalImageData != null) {
            currentZoom = Math.max(0.05, Math.min(currentZoom * factor, 20.0));
            applyLabelZoom();
        }
    }

    private void zoomFit() {
        if (hasBrowser && browser != null && !browser.isDisposed()) {
            browser.execute("nav.zoomFit();");
        } else if (imageLabel != null && !imageLabel.isDisposed() && originalImageData != null) {
            int vw = imageLabel.getBounds().width;
            int vh = imageLabel.getBounds().height;
            if (vw > 0 && vh > 0) {
                currentZoom = Math.min(
                    (double) vw / originalImageData.width,
                    (double) vh / originalImageData.height);
                applyLabelZoom();
            }
        }
    }

    private void applyLabelZoom() {
        if (imageLabel == null || imageLabel.isDisposed() || originalImageData == null) return;
        int w = Math.max(1, (int)(originalImageData.width  * currentZoom));
        int h = Math.max(1, (int)(originalImageData.height * currentZoom));
        ImageData scaled = originalImageData.scaledTo(w, h);
        Image old = imageLabel.getImage();
        imageLabel.setImage(new Image(container.getDisplay(), scaled));
        if (old != null && !old.isDisposed()) old.dispose();
    }

    // -------------------------------------------------------------------------
    // Content display
    // -------------------------------------------------------------------------

    public void setRenderedFile(String filePath) {
        if (container == null || container.isDisposed()) return;
        lastRenderedFile = filePath;
        if (!hasBrowser) currentZoom = 1.0;
        if (filePath.endsWith(".svg")) {
            showSvg(filePath);
        } else {
            showPng(filePath);
        }
    }

    public void setError(String message) {
        if (container == null || container.isDisposed()) return;
        if (message == null || message.isEmpty())
            message = "(unknown error — check Window → Show View → Error Log)";
        if (hasBrowser && browser != null && !browser.isDisposed()) {
            browser.setText(buildHtml(
                "<pre style='font-family:monospace;color:#c00;padding:1em;white-space:pre-wrap'>"
                + escapeHtml(message) + "</pre>"));
        } else if (imageLabel != null && !imageLabel.isDisposed()) {
            imageLabel.setText(message);
        }
    }

    private void showSvg(String filePath) {
        if (hasBrowser && browser != null && !browser.isDisposed()) {
            try {
                browser.setText(buildHtml(Files.readString(Path.of(filePath))));
            } catch (Exception e) {
                setError("Error reading SVG: " + e.getMessage());
            }
        } else if (imageLabel != null && !imageLabel.isDisposed()) {
            imageLabel.setText("SVG: " + filePath);
        }
    }

    private void showPng(String filePath) {
        if (hasBrowser && browser != null && !browser.isDisposed()) {
            try {
                byte[] bytes = Files.readAllBytes(Path.of(filePath));
                String b64 = Base64.getEncoder().encodeToString(bytes);
                browser.setText(buildHtml("<img src='data:image/png;base64," + b64 + "'/>"));
            } catch (Exception e) {
                setError("Error reading PNG: " + e.getMessage());
            }
            return;
        }
        // SWT label fallback
        if (imageLabel == null || imageLabel.isDisposed()) return;
        File file = new File(filePath);
        if (!file.exists()) { imageLabel.setText("File not found: " + filePath); return; }
        try {
            ImageData[] data = new ImageLoader().load(filePath);
            if (data.length > 0) {
                originalImageData = data[0];
                Image old = imageLabel.getImage();
                imageLabel.setImage(new Image(container.getDisplay(), originalImageData));
                if (old != null && !old.isDisposed()) old.dispose();
            }
        } catch (Exception e) {
            imageLabel.setText("Error loading image: " + e.getMessage());
        }
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
