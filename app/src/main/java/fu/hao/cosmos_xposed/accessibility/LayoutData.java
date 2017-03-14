package fu.hao.cosmos_xposed.accessibility;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Description:
 *
 * @author Hao Fu(haofu AT ucdavis.edu)
 * @since 3/13/2017
 */
public class LayoutData implements Serializable {
    private List<String> texts;
    private String pkg;

    public LayoutData() {
        texts = new ArrayList<>();
        pkg = "";
    }

    public void setPkg(String pkg) {
        this.pkg = pkg;
    }

    public String getPkg() {
        return pkg;
    }

    public List<String> getTexts() {
        return texts;
    }

    public void setTexts(List<String> texts) {
        this.texts = texts;
    }
}
