package eclipse.dot.plugin.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

public class DotCompletionProcessor implements IContentAssistProcessor {

    private static final String[] KEYWORDS = {
        "graph", "digraph", "subgraph", "node", "edge", "strict"
    };

    private static final String[] ATTRIBUTES = {
        "label", "color", "shape", "style", "fontname", "fontsize",
        "fillcolor", "fontcolor", "width", "height", "rankdir", "rank",
        "bgcolor", "penwidth", "arrowhead", "arrowtail", "dir",
        "nodesep", "ranksep", "concentrate", "splines", "overlap",
        "margin", "compound", "URL", "tooltip", "href", "fixedsize",
        "peripheries", "minlen", "weight", "constraint", "lhead", "ltail"
    };

    private static final String[] SHAPES = {
        "box", "circle", "ellipse", "diamond", "polygon", "record",
        "plaintext", "point", "triangle", "invtriangle", "doublecircle",
        "doubleoctagon", "tripleoctagon", "invhouse", "house", "hexagon",
        "octagon", "parallelogram", "trapezium", "cylinder", "note",
        "tab", "folder", "box3d", "component", "Mrecord"
    };

    private static final String[] STYLES = {
        "solid", "dashed", "dotted", "bold", "invis", "filled",
        "strikethrough", "diagonals", "rounded", "tapered", "wedge"
    };

    @Override
    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
        String text = viewer.getDocument().get();
        int start = offset;
        while (start > 0 && isWordPart(text.charAt(start - 1))) {
            start--;
        }
        String prefix = text.substring(start, offset);

        List<ICompletionProposal> proposals = new ArrayList<>();
        addProposals(proposals, KEYWORDS, prefix, offset, start);
        addProposals(proposals, ATTRIBUTES, prefix, offset, start);
        addProposals(proposals, SHAPES, prefix, offset, start);
        addProposals(proposals, STYLES, prefix, offset, start);

        return proposals.toArray(new ICompletionProposal[0]);
    }

    private void addProposals(List<ICompletionProposal> proposals, String[] words,
            String prefix, int offset, int start) {
        for (String word : words) {
            if (word.startsWith(prefix)) {
                proposals.add(new CompletionProposal(
                    word, start, offset - start, word.length()
                ));
            }
        }
    }

    private boolean isWordPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    @Override
    public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
        return null;
    }

    @Override
    public char[] getCompletionProposalAutoActivationCharacters() {
        return null;
    }

    @Override
    public char[] getContextInformationAutoActivationCharacters() {
        return null;
    }

    @Override
    public String getErrorMessage() {
        return null;
    }

    @Override
    public IContextInformationValidator getContextInformationValidator() {
        return null;
    }
}
