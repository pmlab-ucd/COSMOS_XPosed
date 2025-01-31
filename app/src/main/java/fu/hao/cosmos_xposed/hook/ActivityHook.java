package fu.hao.cosmos_xposed.hook;

import android.app.Activity;
import android.content.ContentValues;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.yuyh.library.EasyGuide;
import com.yuyh.library.constant.Constants;
import com.yuyh.library.support.HShape;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import de.robv.android.xposed.XC_MethodHook;
import fu.hao.cosmos_xposed.MainActivity;
import fu.hao.cosmos_xposed.R;
import fu.hao.cosmos_xposed.accessibility.LayoutData;
import fu.hao.cosmos_xposed.utils.MyContentProvider;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static fu.hao.cosmos_xposed.utils.MyContentProvider.NEW_INSTANCE_CONTENT_URI;
import static fu.hao.cosmos_xposed.utils.MyContentProvider.PREDICTION_RES_URI;

/**
 * Description:
 *
 * @author Hao Fu(haofu AT ucdavis.edu)
 * @since 4/6/2017
 */
public class ActivityHook extends XC_MethodHook {
    private static String TAG = ActivityHook.class.getName();

    /* Assure latest read of write */
    private static volatile Activity _currentActivity = null;
    private static EasyGuide easyGuide;

    public static Activity getCurrentActivity() {
        return _currentActivity;
    }

    @Override
    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param)
            throws Throwable {
        _currentActivity = (Activity) param.getResult();
        Log.w(TAG, _currentActivity.toString());

    }

    public static void checkView(View view, Document doc, Element element, List<String> texts) {
        if (view == null) {
            return;
        }

        /*
        view.setClickable(true);
        Log.v(TAG, view.toString());
        if (view instanceof Button) {
            Button button = (Button) view;
            Log.w(TAG, button.getText().toString());
            button.setTextColor(Color.BLACK);
        }*/

        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View childNode = viewGroup.getChildAt(i);

                if (childNode == null) {
                    continue;
                }

                Element childElement = doc.createElement("node");
                element.appendChild(childElement);

                try {
                    childElement.setAttribute("id", childNode.getResources().getResourceName(childNode.getId()) == null ?
                            "" : childNode.getResources().getResourceName(childNode.getId()));//getNodeId(childNode));
                } catch (Exception e) {
                    childElement.setAttribute("id", "");
                }
                childElement.setAttribute("class", childNode.getClass().toString());
                //childElement.setAttribute("bounds", childNode.getBoundsInScreen());
                childElement.setAttribute("selected", childNode.isSelected() ? "true" : "false");
                //childElement.setAttribute("password", childNode.isPassword() ? "true" : "false");
                childElement.setAttribute("long-clickable", childNode.isLongClickable() ? "true" : "false");
                //childElement.setAttribute("scrollable", childNode.isScrollContainer() ? "true" : "false");
                childElement.setAttribute("focused", childNode.isFocused() ? "true" : "false");
                childElement.setAttribute("focusable", childNode.isFocusable() ? "true" : "false");
                childElement.setAttribute("enabled", childNode.isEnabled() ? "true" : "false");
                childElement.setAttribute("clickable", childNode.isClickable() ? "true" : "false");
                //childElement.setAttribute("checked", childNode. ? "true" : "false");
                childElement.setAttribute("checkable", childNode.isClickable() ? "true" : "false");
                childElement.setAttribute("content-desc", childNode.getContentDescription() == null ?
                        "" : childNode.getContentDescription().toString());
                childElement.setAttribute("package", childNode.getContext().getPackageName());

                if (childNode instanceof TextView) {
                    TextView textView = (TextView) childNode;
                    childElement.setAttribute("text", textView.getText() == null ? "" :
                            textView.getText().toString());
                    if (textView.getText() != null) {
                        texts.add(textView.getText().toString());
                    }
                }

                childElement.setAttribute("index", Integer.toString(i));

                checkView(childNode, doc, element, texts);
            }
        }
    }

    public static LayoutData toLayoutXML(Activity activity) {
        LayoutData layoutData = new LayoutData();
        layoutData.setPkg(activity.getPackageName());
        final ViewGroup viewGroup = (ViewGroup) ((ViewGroup) activity
                .findViewById(android.R.id.content)).getChildAt(0);

        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // root elements
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("hierarchy");
            doc.appendChild(rootElement);

            checkView(viewGroup, doc, rootElement, layoutData.getTexts());
        } catch (ParserConfigurationException pce){
            pce.printStackTrace();
        }

        return layoutData;
    }

    // 须在View绘制完成之后调用，否则可能无法准确显示
// offsetX:正数代表从屏幕左侧往右偏移距离，负数表示从屏幕右侧往左偏移距离。Constant.CENTER 表示居中
// offsetY:同理。正数由上到下，负数由下到上。Constant.CENTER 表示居中
    public static void show(Activity activity, View view, String pkg, String event,
                            final String instanceData, final String instanceIndex) {
        if (view != null) {
            if (easyGuide != null && easyGuide.isShowing())
                easyGuide.dismiss();

            LinearLayout mTipView = new LinearLayout(activity);
            mTipView.setGravity(Gravity.CENTER_HORIZONTAL);
            mTipView.setLayoutParams(new RelativeLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
            mTipView.setOrientation(LinearLayout.VERTICAL);


            //String message = "The highlighted view (" + view.getResources().getResourceName(view.getId()) +
            //      ") is trying to (ACCESS_LOCATION) after (performClick)!";

            String message = "The highlighted widget is accessing your location information after clicking. Will you allow or deny?";
            int textSize = 21;

            int padding = EasyGuide.dip2px(activity, 5);
            TextView tvMsg = new TextView(activity);
            tvMsg.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
            tvMsg.setPadding(padding, padding, padding, padding);
            tvMsg.setGravity(Gravity.CENTER);
            tvMsg.setText(message);
            tvMsg.setTextColor(Color.WHITE);
            tvMsg.setTextSize(textSize == -1 ? 12 : textSize);

            mTipView.addView(tvMsg);

            int[] loc = new int[2];
            view.getLocationOnScreen(loc);

            //View tipsView = createTipsView(activity);

            View allowView = createTipsView(activity, "Allow", Color.GREEN, 18);
            mTipView.addView(allowView);
            int[] allowViewLoc = new int[2];
            allowView.getLocationOnScreen(allowViewLoc);
            View denyView = createTipsView(activity, "Deny", Color.RED, 18);
            mTipView.addView(denyView);

            easyGuide = new EasyGuide.Builder(activity)
                    // 增加View高亮区域，可同时显示多个
                    .addHightArea(view, HShape.CIRCLE)
                    // 添加箭头指示
                    //.addIndicator(R.drawable.right_top, loc[0], loc[1] + view.getHeight())
                    // 复杂的提示布局，建议通过此方法，较容易控制
                    //.addView(createTipsView(activity), 0, loc[1] + view.getHeight(),
                    //new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    //       ViewGroup.LayoutParams.WRAP_CONTENT))
                    // 设置提示信息，默认居中。若需调整，可采用addView形式
                    .addView(mTipView, Constants.CENTER, Constants.CENTER, new RelativeLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
                    //.addView(allowView, Constants.CENTER, Constants.CENTER + view.getHeight())
                    //.addView(denyView, allowViewLoc[0], allowViewLoc[1] + allowView.getHeight())
                    //.addMessage(message, 21)
                    // 设置确定按钮，默认居中显示在Message下面
                /*.setPositiveButton("Allow", 20, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        easyGuide.dismiss();
                        Log.i("TAG", "dismiss");
                    }
                })*/
                    // 是否点击任意区域消失，默认true
                    .dismissAnyWhere(false)
                    // 若点击作用在高亮区域，是否执行高亮区域的点击事件，默认false
                    .performViewClick(true)
                    .build();

            easyGuide.show();
        } else {
            if (easyGuide != null && easyGuide.isShowing())
                easyGuide.dismiss();

            LinearLayout mTipView = new LinearLayout(activity);
            mTipView.setGravity(Gravity.CENTER_HORIZONTAL);
            mTipView.setLayoutParams(new RelativeLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
            mTipView.setOrientation(LinearLayout.VERTICAL);


            //String message = "The highlighted view (" + view.getResources().getResourceName(view.getId()) +
            //      ") is trying to (ACCESS_LOCATION) after (performClick)!";

            String message = pkg + " is accessing your location information after" + event + ". Will you allow or deny?";
            int textSize = 21;

            int padding = EasyGuide.dip2px(activity, 5);
            TextView tvMsg = new TextView(activity);
            tvMsg.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
            tvMsg.setPadding(padding, padding, padding, padding);
            tvMsg.setGravity(Gravity.CENTER);
            tvMsg.setText(message);
            tvMsg.setTextColor(Color.WHITE);
            tvMsg.setTextSize(textSize == -1 ? 12 : textSize);

            mTipView.addView(tvMsg);

            int[] loc = new int[2];
            //view.getLocationOnScreen(loc);

            //View tipsView = createTipsView(activity);

            View allowView = createTipsView(activity, "Allow", Color.GREEN, 18);
            allowView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ContentValues values = new ContentValues();
                    values.put(MyContentProvider.INSTANCE_INDEX, instanceIndex);
                    values.put(MyContentProvider.INSTANCE_LABEL, "0");
                    values.put(MyContentProvider.INSTANCE_DATA, instanceData);
                    //getContentResolver().delete(PREDICTION_RES_URI, null, null);
                    Uri uri = view.getContext().getContentResolver().
                            insert(NEW_INSTANCE_CONTENT_URI, values);
                    Log.i(TAG, "New instance stored: " + uri);
                    easyGuide.dismiss();
                }
            });
            mTipView.addView(allowView);
            int[] allowViewLoc = new int[2];
            allowView.getLocationOnScreen(allowViewLoc);
            View denyView = createTipsView(activity, "Deny", Color.RED, 18);
            denyView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ContentValues values = new ContentValues();
                    values.put(MyContentProvider.INSTANCE_INDEX, instanceIndex);
                    values.put(MyContentProvider.INSTANCE_LABEL, "1");
                    values.put(MyContentProvider.INSTANCE_DATA, instanceData);
                    //getContentResolver().delete(PREDICTION_RES_URI, null, null);
                    Uri uri = view.getContext().getContentResolver().
                            insert(NEW_INSTANCE_CONTENT_URI, values);
                    Log.i(TAG, "New instance stored: " + uri);
                    easyGuide.dismiss();
                }
            });
            mTipView.addView(denyView);

            easyGuide = new EasyGuide.Builder(activity)
                    // 增加View高亮区域，可同时显示多个
                    //.addHightArea(view, HShape.CIRCLE)
                    // 添加箭头指示
                    //.addIndicator(R.drawable.right_top, loc[0], loc[1] + view.getHeight())
                    // 复杂的提示布局，建议通过此方法，较容易控制
                    //.addView(createTipsView(activity), 0, loc[1] + view.getHeight(),
                    //new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    //       ViewGroup.LayoutParams.WRAP_CONTENT))
                    // 设置提示信息，默认居中。若需调整，可采用addView形式
                    .addView(mTipView, Constants.CENTER, Constants.CENTER, new RelativeLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
                    //.addView(allowView, Constants.CENTER, Constants.CENTER + view.getHeight())
                    //.addView(denyView, allowViewLoc[0], allowViewLoc[1] + allowView.getHeight())
                    //.addMessage(message, 21)
                    // 设置确定按钮，默认居中显示在Message下面
                /*.setPositiveButton("Allow", 20, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        easyGuide.dismiss();
                        Log.i("TAG", "dismiss");
                    }
                })*/
                    // 是否点击任意区域消失，默认true
                    .dismissAnyWhere(false)
                    // 若点击作用在高亮区域，是否执行高亮区域的点击事件，默认false
                    //.performViewClick(true)
                    .build();

            easyGuide.show();
        }
    }

    private static View createTipsView(Activity activity, String message, int textColor, int textSize) {
        //View view = LayoutInflater.from(activity).inflate(R.layout.tips_view, null);
        Button button = new Button(activity);
        button.setTextColor(textColor);
        button.setText(message);
        button.setTextSize(textSize);
        button.setLayoutParams(new ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (easyGuide != null) {
                    easyGuide.dismiss();
                }
            }
        });

        return button;
    }
}
