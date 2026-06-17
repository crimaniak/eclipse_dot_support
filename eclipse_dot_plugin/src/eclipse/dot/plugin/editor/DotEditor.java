package eclipse.dot.plugin.editor;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import eclipse.dot.plugin.renderer.GraphvizRenderer;
import eclipse.dot.plugin.view.DotGraphView;

public class DotEditor extends TextEditor {

    private ColorManager colorManager;
    private DotOutlinePage outlinePage;
    private final ILog log = Platform.getLog(getClass());

    private final IPartListener2 partListener = new IPartListener2() {
        @Override
        public void partActivated(IWorkbenchPartReference ref) {
            if (ref.getPart(false) == DotEditor.this) triggerRender();
        }
    };

    public DotEditor() {
        super();
        colorManager = new ColorManager();
        setSourceViewerConfiguration(new DotSourceViewerConfiguration(colorManager));
    }

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);
        log.info("DOT file opened: " + getEditorInput().getToolTipText());
        getSite().getPage().addPartListener(partListener);
        parent.getDisplay().asyncExec(this::triggerRender);
    }

    @Override
    public void doSave(IProgressMonitor progressMonitor) {
        super.doSave(progressMonitor);
        log.info("DOT file saved: " + getEditorInput().getToolTipText());
        triggerRender();
    }

    private void triggerRender() {
        try {
            IEditorInput input = getEditorInput();
            if (!(input instanceof IFileEditorInput)) {
                log.warn("DOT render skipped: input is not IFileEditorInput ("
                    + (input == null ? "null" : input.getClass().getName()) + ")");
                return;
            }
            IPath location = ((IFileEditorInput) input).getFile().getLocation();
            if (location == null) {
                log.warn("DOT render skipped: IFile.getLocation() returned null");
                return;
            }
            String filePath = location.toOSString();
            IWorkbenchPage page = getSite().getPage();

            new Thread(() -> {
                try {
                    String outputPath = GraphvizRenderer.render(filePath);
                    showInView(page, view -> view.setRenderedFile(outputPath));
                } catch (Throwable t) {
                    log.warn("DOT render failed: " + t);
                    String msg = t.getMessage() != null ? t.getMessage() : t.toString();
                    showInView(page, view -> view.setError(msg));
                }
            }, "dot-renderer").start();

        } catch (Throwable t) {
            log.warn("DOT triggerRender failed: " + t);
        }
    }

    @FunctionalInterface
    private interface ViewAction {
        void run(DotGraphView view);
    }

    private void showInView(IWorkbenchPage page, ViewAction action) {
        PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
            try {
                IViewPart view = page.showView(DotGraphView.VIEW_ID);
                if (view instanceof DotGraphView) {
                    action.run((DotGraphView) view);
                } else {
                    log.warn("DOT showView returned unexpected type: "
                        + (view == null ? "null" : view.getClass().getName()));
                }
            } catch (Throwable t) {
                log.warn("DOT showInView failed: " + t);
            }
        });
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (IContentOutlinePage.class.equals(adapter)) {
            if (outlinePage == null) {
                outlinePage = new DotOutlinePage(
                    getDocumentProvider().getDocument(getEditorInput()));
            }
            return adapter.cast(outlinePage);
        }
        return super.getAdapter(adapter);
    }

    @Override
    public void dispose() {
        getSite().getPage().removePartListener(partListener);
        colorManager.dispose();
        super.dispose();
    }
}
