package eclipse.dot.plugin.editor;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;

public class DotScanner extends RuleBasedScanner {

    public DotScanner(ColorManager manager) {
        IToken keyword = new Token(new TextAttribute(
            manager.getColor(new RGB(127, 0, 85)), null, SWT.BOLD));
        IToken comment = new Token(new TextAttribute(
            manager.getColor(new RGB(63, 127, 95))));
        IToken string = new Token(new TextAttribute(
            manager.getColor(new RGB(42, 0, 255))));
        IToken attribute = new Token(new TextAttribute(
            manager.getColor(new RGB(128, 0, 0))));

        // The default token must be non-undefined so that unmatched words (e.g.
        // "Category_id") are consumed whole. Without it, WordRule unreads the
        // full word on a miss and the scanner re-enters mid-word, causing the
        // "id" suffix to be falsely highlighted as an attribute.
        WordRule wordRule = new WordRule(new IWordDetector() {
            @Override
            public boolean isWordStart(char c) {
                return Character.isLetter(c) || c == '_';
            }
            @Override
            public boolean isWordPart(char c) {
                return Character.isLetterOrDigit(c) || c == '_';
            }
        }, new Token(null));

        String[] keywords = { "graph", "digraph", "subgraph", "node", "edge", "strict" };
        for (String kw : keywords) {
            wordRule.addWord(kw, keyword);
        }

        String[] attrs = {
            "label", "color", "shape", "style", "fontname", "fontsize",
            "fillcolor", "fontcolor", "width", "height", "rankdir", "rank",
            "bgcolor", "penwidth", "arrowhead", "arrowtail", "dir",
            "compound", "concentrate", "splines", "overlap", "sep",
            "nodesep", "ranksep", "margin", "pad", "URL", "tooltip",
            "href", "target", "id", "comment", "ordering", "outputorder",
            "charset", "center", "decorate", "headlabel", "taillabel",
            "labeldistance", "labelangle", "samehead", "sametail",
            "minlen", "weight", "constraint", "lhead", "ltail",
            "peripheries", "distortion", "skew", "sides", "orientation",
            "fixedsize", "image", "imagescale", "pos", "pin"
        };
        for (String attr : attrs) {
            wordRule.addWord(attr, attribute);
        }

        setRules(new IRule[] {
            new SingleLineRule("//", null, comment, (char) 0, true),
            new SingleLineRule("#", null, comment, (char) 0, true),
            new MultiLineRule("/*", "*/", comment),
            new SingleLineRule("\"", "\"", string, '\\'),
            wordRule
        });
    }
}
