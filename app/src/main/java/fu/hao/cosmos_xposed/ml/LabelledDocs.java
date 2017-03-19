package fu.hao.cosmos_xposed.ml;

import java.util.ArrayList;
import java.util.List;

/**
 * Description:
 *
 * @author Hao Fu(haofu AT ucdavis.edu)
 * @since 3/18/2017
 */
public class LabelledDocs {
    List<LabelledDoc> labelledDocs = new ArrayList<>();

    public List<LabelledDoc> getLabelledDocs() {
        return labelledDocs;
    }
}
