package eclipse.dot.plugin.preferences;

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import eclipse.dot.plugin.Activator;
import eclipse.dot.plugin.renderer.GraphvizRenderer;

public class DotPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public DotPreferencePage() {
        super(GRID);
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
        setDescription("Configure DOT/Graphviz support settings.");
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(1, false));
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        createInfoSection(container);

        Label separator = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Control fieldArea = super.createContents(container);
        if (fieldArea != null) {
            fieldArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        }

        return container;
    }

    private void createInfoSection(Composite parent) {
        Composite info = new Composite(parent, SWT.NONE);
        info.setLayout(new GridLayout(2, false));
        info.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        addRow(info, "Plugin status:", "Active");

        String execPath = GraphvizRenderer.getDetectedExecutablePath();
        if (execPath != null) {
            addRow(info, "Graphviz executable:", execPath);
            String version = GraphvizRenderer.getVersion(execPath);
            addRow(info, "Graphviz version:", version != null ? version : "(unknown)");
        } else {
            addRow(info, "Graphviz:", "Not found — install Graphviz or set the path below");
        }
    }

    private void addRow(Composite parent, String key, String value) {
        Label keyLabel = new Label(parent, SWT.NONE);
        keyLabel.setText(key);
        Label valueLabel = new Label(parent, SWT.NONE);
        valueLabel.setText(value);
        valueLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    }

    @Override
    protected void createFieldEditors() {
        addField(new DirectoryFieldEditor(
            DotPreferenceConstants.GRAPHVIZ_PATH,
            "Graphviz installation directory:",
            getFieldEditorParent()
        ));
        addField(new RadioGroupFieldEditor(
            DotPreferenceConstants.OUTPUT_FORMAT,
            "Output format:",
            2,
            new String[][] {
                { "SVG", DotPreferenceConstants.FORMAT_SVG },
                { "PNG", DotPreferenceConstants.FORMAT_PNG }
            },
            getFieldEditorParent()
        ));
    }

    @Override
    public void init(IWorkbench workbench) {
    }
}
