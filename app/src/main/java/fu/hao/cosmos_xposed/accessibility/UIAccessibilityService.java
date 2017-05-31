package fu.hao.cosmos_xposed.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.StringWriter;
import java.util.List;
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

import fu.hao.cosmos_xposed.utils.MyContentProvider;

import static fu.hao.cosmos_xposed.utils.MyContentProvider.EVENT_TYPE_CONTENT_URI;
import static fu.hao.cosmos_xposed.utils.MyContentProvider.LAYOUT_CONTENT_URI;
import static fu.hao.cosmos_xposed.utils.MyContentProvider.WHO_CONTENT_URI;

public class UIAccessibilityService extends android.accessibilityservice.AccessibilityService {
    private static final String TAG = UIAccessibilityService.class.getName();

    public static boolean toXml = false;

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

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // TODO Change to check event only if new window is created
        int eventType = event.getEventType();
        Log.i(TAG, "UI event detected!" + getEventText(eventType) + " from " + event.getPackageName());

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

        LayoutData layoutData = new LayoutData();
        layoutData.setPkg(event.getPackageName().toString());

        if (eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            String eventText = getEventText(eventType);
            ContentValues values = new ContentValues();
            values.put(MyContentProvider.LAYOUT_DATA, eventText);
            getContentResolver().delete(EVENT_TYPE_CONTENT_URI, null, null);
            Uri uri = getContentResolver().insert(EVENT_TYPE_CONTENT_URI, values);
            Log.w(TAG, "Event type " + eventText + " stored: " + uri);

            getContentResolver().delete(WHO_CONTENT_URI, null, null);
            // TODO Insert Relative position of the node at the layout
            values = new ContentValues();
            // Make sure we're running on JELLY_BEAN or higher to use getRid APIs
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                try {
                    values.put(MyContentProvider.LAYOUT_DATA, nodeInfo.getViewIdResourceName());
                    Log.w(TAG, "Rid: " + nodeInfo.getViewIdResourceName());
                    if (nodeInfo.getViewIdResourceName() == null || nodeInfo.getViewIdResourceName().isEmpty()) {
                        Log.w(TAG, "Empty rid!");
                    } else {
                        uri = getContentResolver().insert(WHO_CONTENT_URI, values);
                        Log.w(TAG, "Who stored: " + uri);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
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

            checkNodeInfo(rootNode, doc, rootElement, layoutData.getTexts());

            if (toXml) {
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
                    transformer.transform(source, result);
                    String strResult = writer.toString();
                    Log.i(TAG, "XML: " + strResult);

                    ContentValues values = new ContentValues();
                    values.put(MyContentProvider.LAYOUT_DATA, strResult);
                    getContentResolver().delete(LAYOUT_CONTENT_URI, null, null);
                    Uri uri = getContentResolver().insert(LAYOUT_CONTENT_URI, values);
                    Log.i(TAG, "Layout XML stored: " + uri);
                    //MainApplication.write2FileExternally("layout.xml", strResult);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                for (String text : layoutData.getTexts()) {
                    stringBuilder.append(text + ";");
                }
                Log.i(TAG, "Texts: " + stringBuilder.toString());

                //ContentValues values = new ContentValues();
                //values.put(MyContentProvider.LAYOUT_DATA, stringBuilder.toString());

                File cacheDir = getCacheDir();
                File outFile = new File(cacheDir, "layout.data");

                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(outFile);
                    ObjectOutputStream os = new ObjectOutputStream(fileOutputStream);
                    os.writeObject(layoutData);
                    os.close();
                    Log.i(TAG, "layoutData saved.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (ParserConfigurationException | TransformerException pce){
            pce.printStackTrace();
        }
    }

    private String getEventText(int eventType) {
        String eventText = null;
        switch (eventType) {
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                eventText = "TYPE_VIEW_CLICKED";
                break;
            case AccessibilityEvent.TYPE_VIEW_FOCUSED:
                eventText = "TYPE_VIEW_FOCUSED";
                break;
            case AccessibilityEvent.TYPE_VIEW_LONG_CLICKED:
                eventText = "TYPE_VIEW_LONG_CLICKED";
                break;
            case AccessibilityEvent.TYPE_VIEW_SELECTED:
                eventText = "TYPE_VIEW_SELECTED";
                break;
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                eventText = "TYPE_VIEW_TEXT_CHANGED";
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                eventText = "TYPE_WINDOW_STATE_CHANGED";
                break;
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                eventText = "TYPE_NOTIFICATION_STATE_CHANGED";
                break;
            case AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_END:
                eventText = "TYPE_TOUCH_EXPLORATION_GESTURE_END";
                break;
            case AccessibilityEvent.TYPE_ANNOUNCEMENT:
                eventText = "TYPE_ANNOUNCEMENT";
                break;
            case AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_START:
                eventText = "TYPE_TOUCH_EXPLORATION_GESTURE_START";
                break;
            case AccessibilityEvent.TYPE_VIEW_HOVER_ENTER:
                eventText = "TYPE_VIEW_HOVER_ENTER";
                break;
            case AccessibilityEvent.TYPE_VIEW_HOVER_EXIT:
                eventText = "TYPE_VIEW_HOVER_EXIT";
                break;
            case AccessibilityEvent.TYPE_VIEW_SCROLLED:
                eventText = "TYPE_VIEW_SCROLLED";
                break;
            case AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED:
                eventText = "TYPE_VIEW_TEXT_SELECTION_CHANGED";
                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                eventText = "TYPE_WINDOW_CONTENT_CHANGED";
                break;
        }

        return eventText;
    }

    private ActivityInfo tryGetActivity(ComponentName componentName) {
        try {
            return getPackageManager().getActivityInfo(componentName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void checkNodeInfo(AccessibilityNodeInfo nodeInfo, Document doc, Element element, List<String> texts) {
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
            if (childNode.getText() != null) {
                texts.add(childNode.getText().toString());
            }
            childElement.setAttribute("index", Integer.toString(i));

            checkNodeInfo(childNode, doc, childElement, texts);
        }
    }

    @Override
    public void onInterrupt() {
        // TODO Auto-generated method stub
    }



}
