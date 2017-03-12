package fu.hao.cosmos_xposed.utils;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
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

    public static NodeList getNodeList(File xmlFile) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbFactory
                = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(xmlFile);
        doc.getDocumentElement().normalize();
        XMLParser.printNode(doc.getDocumentElement(), " ");
        Log.w(TAG, "Root element :"
                + doc.getDocumentElement().getNodeName());
        return doc.getElementsByTagName("node");
    }

    public static NodeList getNodeList(String xmlData) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbFactory
                = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        InputSource inputSource = new InputSource(new StringReader(xmlData));
        Document doc = dBuilder.parse(inputSource);
        doc.getDocumentElement().normalize();
        XMLParser.printNode(doc.getDocumentElement(), " ");
        Log.w(TAG, "Root element :"
                + doc.getDocumentElement().getNodeName());
        return doc.getElementsByTagName("node");
    }

    public static List<String> getTexts(NodeList nList) {
        List<String> texts = new ArrayList<>();
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

    public static List<String> getPkg(NodeList nList) {
        List<String> texts = new ArrayList<>();
        Log.w(TAG, "Len" + nList.getLength());
        Log.w(TAG, "----------------------------");
        for (int i = 0; i < nList.getLength(); i++) {
            Node node = nList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                String text = element.getAttribute("package");
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
