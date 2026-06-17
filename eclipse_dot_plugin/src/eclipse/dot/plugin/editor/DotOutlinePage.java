package eclipse.dot.plugin.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

public class DotOutlinePage extends ContentOutlinePage {

    private IDocument document;

    public DotOutlinePage(IDocument document) {
        this.document = document;
    }

    public void setDocument(IDocument document) {
        this.document = document;
        refresh();
    }

    @Override
    public void createControl(Composite parent) {
        super.createControl(parent);

        getTreeViewer().setContentProvider(new ITreeContentProvider() {
            @Override
            public Object[] getElements(Object input) {
                if (input instanceof List) return ((List<?>) input).toArray();
                return new Object[0];
            }
            @Override
            public Object[] getChildren(Object element) {
                if (element instanceof DotNode) return ((DotNode) element).children.toArray();
                return new Object[0];
            }
            @Override
            public Object getParent(Object element) { return null; }
            @Override
            public boolean hasChildren(Object element) {
                return element instanceof DotNode && !((DotNode) element).children.isEmpty();
            }
        });

        getTreeViewer().setLabelProvider(new LabelProvider() {
            @Override
            public String getText(Object element) {
                return element.toString();
            }
        });

        refresh();
    }

    public void refresh() {
        if (getTreeViewer() == null || getTreeViewer().getControl().isDisposed()) return;
        if (document == null) return;
        getTreeViewer().setInput(parse(document.get()));
        getTreeViewer().expandAll();
    }

    private List<DotNode> parse(String content) {
        List<DotNode> result = new ArrayList<>();
        Pattern graphPattern = Pattern.compile(
            "\\b(strict\\s+)?(di)?graph\\s*(\\w+)?\\s*\\{", Pattern.CASE_INSENSITIVE);
        Matcher m = graphPattern.matcher(content);
        while (m.find()) {
            String type = (m.group(1) != null ? "strict " : "")
                        + (m.group(2) != null ? "digraph" : "graph");
            String name = m.group(3) != null ? " " + m.group(3) : "";
            DotNode graphNode = new DotNode(type + name);
            parseBody(content, m.end(), graphNode);
            result.add(graphNode);
        }
        return result;
    }

    private void parseBody(String content, int start, DotNode parent) {
        int depth = 1, i = start;
        while (i < content.length() && depth > 0) {
            char c = content.charAt(i++);
            if (c == '{') depth++;
            else if (c == '}') depth--;
        }
        String body = content.substring(start, i - 1);

        // Subgraphs — collect their spans so we can exclude them from the flat scans below
        Pattern subPat = Pattern.compile("\\bsubgraph\\s*(\\w+)?\\s*\\{", Pattern.CASE_INSENSITIVE);
        Matcher sm = subPat.matcher(body);
        List<int[]> subSpans = new ArrayList<>();
        while (sm.find()) {
            String name = sm.group(1) != null ? sm.group(1) : "(anonymous)";
            DotNode sub = new DotNode("subgraph " + name);
            parseBody(body, sm.end(), sub);
            parent.children.add(sub);
            // record span of the entire subgraph block (keyword start to closing brace)
            int subBodyStart = sm.end();
            int d = 1, j = subBodyStart;
            while (j < body.length() && d > 0) {
                char ch = body.charAt(j++);
                if (ch == '{') d++; else if (ch == '}') d--;
            }
            subSpans.add(new int[]{ sm.start(), j });
        }

        // Build a version of body with subgraph blocks blanked out so that edge/node
        // patterns below only see the direct members of this scope.
        String flatBody = body;
        if (!subSpans.isEmpty()) {
            char[] buf = body.toCharArray();
            for (int[] span : subSpans) {
                for (int k = span[0]; k < span[1] && k < buf.length; k++) buf[k] = ' ';
            }
            flatBody = new String(buf);
        }

        // Edges
        Pattern edgePat = Pattern.compile("(\\w+|\"[^\"]+\")\\s*(?:->|--)\\s*(\\w+|\"[^\"]+\")");
        Matcher em = edgePat.matcher(flatBody);
        while (em.find()) {
            parent.children.add(new DotNode(em.group(1) + " \u2192 " + em.group(2)));
        }

        // Standalone node declarations (word followed by [ or ;, not a keyword)
        Pattern nodePat = Pattern.compile("^\\s*(\\w+)\\s*[\\[;]", Pattern.MULTILINE);
        Matcher nm = nodePat.matcher(flatBody);
        while (nm.find()) {
            String n = nm.group(1);
            if (!n.matches("graph|digraph|subgraph|node|edge|strict")) {
                parent.children.add(new DotNode("node: " + n));
            }
        }
    }

    static class DotNode {
        final String label;
        final List<DotNode> children = new ArrayList<>();
        DotNode(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }
}
