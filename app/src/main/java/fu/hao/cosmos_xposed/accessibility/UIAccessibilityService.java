package fu.hao.cosmos_xposed.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import fu.hao.cosmos_xposed.MainApplication;
import fu.hao.cosmos_xposed.hook.Main;
import fu.hao.cosmos_xposed.utils.MyContentProvider;

import static fu.hao.cosmos_xposed.utils.MyContentProvider.CONTENT_URI;

public class UIAccessibilityService extends android.accessibilityservice.AccessibilityService {
    private static final String TAG = UIAccessibilityService.class.getName();

    @Override
    protected void onServiceConnected() {
        Log.i(TAG, "config success!");
        AccessibilityServiceInfo accessibilityServiceInfo = new AccessibilityServiceInfo();
        // accessibilityServiceInfo.packageNames = PACKAGES;
        accessibilityServiceInfo.eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED |
                AccessibilityEvent.TYPE_VIEW_FOCUSED | AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        accessibilityServiceInfo.feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN;
        accessibilityServiceInfo.notificationTimeout = 100;
        accessibilityServiceInfo.flags = AccessibilityServiceInfo.DEFAULT |
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS |
                AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY |
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS |
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        setServiceInfo(accessibilityServiceInfo);
    }

    /**
     * 获取节点对象唯一的id，通过正则表达式匹配
     * AccessibilityNodeInfo@后的十六进制数字
     *
     * @param node AccessibilityNodeInfo对象
     * @return id字符串
     */
    private String getNodeId(AccessibilityNodeInfo node) {
        /* 用正则表达式匹配节点Object */
        Pattern objHashPattern = Pattern.compile("(?<=@)[0-9|a-z]+(?=;)");
        Matcher objHashMatcher = objHashPattern.matcher(node.toString());

        // AccessibilityNodeInfo必然有且只有一次匹配，因此不再作判断
        objHashMatcher.find();

        return objHashMatcher.group(0);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // TODO Change to check event only if new window is created
        int eventType = event.getEventType();
        String eventText = "";
        Log.i(TAG, "UI event detected!");

        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (event.getPackageName() != null && event.getClassName() != null) {
                ComponentName componentName = new ComponentName(
                        event.getPackageName().toString(),
                        event.getClassName().toString()
                );

                ActivityInfo activityInfo = tryGetActivity(componentName);
                boolean isActivity = activityInfo != null;
                if (isActivity) {
                    Log.i("CurrentActivity", componentName.flattenToShortString());
                }
            }
        }

        AccessibilityNodeInfo nodeInfo = event.getSource();
        if (nodeInfo == null) {
            return;
        }

        Log.i(TAG, "Examine current page...");
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // root elements
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("hierarchy");
            doc.appendChild(rootElement);

            checkNodeInfo(rootNode, doc, rootElement);

            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            // FIXME Correct the path
            //StreamResult result = new StreamResult(
            //new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
            // "/COSMOS/" + "test.xml"));

            // Output to console for testing
            /*
            StreamResult result = new StreamResult(System.out);

            transformer.transform(source, result);
            Log.w(TAG, result.getOutputStream().toString());*/


            try {
                StringWriter writer = new StringWriter();
                StreamResult result = new StreamResult(writer);
                // TransformerFactory tFactory = TransformerFactory.newInstance();
                // Transformer transformer = tFactory.newTransformer();
                transformer.transform(source,result);
                String strResult = writer.toString();
                Log.w(TAG, "XML: " + strResult);

                ContentValues values = new ContentValues();
                values.put(MyContentProvider.name, strResult);
                getContentResolver().delete(CONTENT_URI, null, null);
                Uri uri = getContentResolver().insert(CONTENT_URI, values);
                Log.w(TAG, "Layout XML stored: " + uri);
                //MainApplication.write2FileExternally("layout.xml", strResult);
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (ParserConfigurationException | TransformerException pce){
            pce.printStackTrace();
        }
    }

    private ActivityInfo tryGetActivity(ComponentName componentName) {
        try {
            return getPackageManager().getActivityInfo(componentName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void checkNodeInfo(AccessibilityNodeInfo nodeInfo, Document doc, Element element) {
        if (nodeInfo == null) {
            return;
        }

        for (int i = 0; i < nodeInfo.getChildCount(); i++) {
            AccessibilityNodeInfo childNode = nodeInfo.getChild(i);

            if (childNode == null) {
                continue;
            }

            Element childElement = doc.createElement("node");
            element.appendChild(childElement);

            // Make sure we're running on JELLY_BEAN or higher to use getRid APIs
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                try {
                    childElement.setAttribute("id", childNode.getViewIdResourceName() == null ?
                            "" : childNode.getViewIdResourceName());//getNodeId(childNode));
                } catch (Exception e) {
                   e.printStackTrace();
                }
            }
            childElement.setAttribute("class", childNode.getClassName().toString());
            //childElement.setAttribute("bounds", childNode.getBoundsInScreen());
            childElement.setAttribute("selected", childNode.isSelected() ? "true" : "false");
            childElement.setAttribute("password", childNode.isPassword() ? "true" : "false");
            childElement.setAttribute("long-clickable", childNode.isLongClickable() ? "true" : "false");
            childElement.setAttribute("scrollable", childNode.isScrollable() ? "true" : "false");
            childElement.setAttribute("focused", childNode.isFocused() ? "true" : "false");
            childElement.setAttribute("focusable", childNode.isFocusable() ? "true" : "false");
            childElement.setAttribute("enabled", childNode.isEnabled() ? "true" : "false");
            childElement.setAttribute("clickable", childNode.isClickable() ? "true" : "false");
            childElement.setAttribute("checked", childNode.isChecked() ? "true" : "false");
            childElement.setAttribute("checkable", childNode.isClickable() ? "true" : "false");
            childElement.setAttribute("content-desc", childNode.getContentDescription() == null ?
                    "" : childNode.getContentDescription().toString());
            childElement.setAttribute("package", childNode.getPackageName().toString());
            childElement.setAttribute("text", childNode.getText() == null ? "" :
                    childNode.getText().toString());
            childElement.setAttribute("index", Integer.toString(i));

            checkNodeInfo(childNode, doc, childElement);
        }
    }

    @Override
    public void onInterrupt() {
        // TODO Auto-generated method stub
    }



}
