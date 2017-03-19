package fu.hao.cosmos_xposed.ml;

/**
 * Description:
 *
 * @author Hao Fu(haofu AT ucdavis.edu)
 * @since 3/18/2017
 */
public class LabelledDoc {
    private String label;
    private String doc = null;

    LabelledDoc(String label, String doc) {
        this.label = label;
        doc = doc.replace(",", "");
        this.doc = doc;
    }

    public String getLabel() {
        return label;
    }

    public String getDoc() {
        return doc;
    }
}
