package fu.hao.cosmos_xposed.utils;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Description:
 *
 * @author Hao Fu(haofu AT ucdavis.edu)
 * @since 3/5/2017
 */
public class XMLParser {
    public static String TAG = XMLParser.class.getSimpleName();

    public static List<String> getTexts(File xmlFile) throws ParserConfigurationException, IOException, SAXException {
        List<String> texts = new ArrayList<>();
        DocumentBuilderFactory dbFactory
                = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(xmlFile);
        doc.getDocumentElement().normalize();
        XMLParser.printNode(doc.getDocumentElement(), " ");
        Log.w(TAG, "Root element :"
                + doc.getDocumentElement().getNodeName());
        NodeList nList = doc.getElementsByTagName("node");
        Log.w(TAG, "Len" + nList.getLength());
        Log.w(TAG, "----------------------------");
        for (int i = 0; i < nList.getLength(); i++) {
            Node node = nList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                String text = element.getAttribute("text");
                if (text.length() > 1) {
                    texts.add(text);
                }
            }
        }

        return texts;
    }

    public static void printNode(Node rootNode, String spacer) {
        if (rootNode.getNodeType() != Node.ELEMENT_NODE) {
            return;
        }
        Element element = (Element) rootNode;
        Log.w(TAG, spacer + rootNode.getNodeName() + " -> " + element.getAttribute("text"));
        NodeList nl = rootNode.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++)
            printNode(nl.item(i), spacer + "   ");
    }

}
