package fu.hao.cosmos_xposed;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

import fu.hao.cosmos_xposed.ml.SelfAdaptiveLearning;
import fu.hao.cosmos_xposed.ml.WekaUtils;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.HoeffdingTree;
import weka.filters.unsupervised.attribute.StringToWordVector;

public class MainActivity extends AppCompatActivity {
    private static String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        /*
        try {
            FilteredClassifier filteredClassifier = WekaUtils.loadClassifier(getAssets().open("weka/weka.model"));
            Log.w(TAG, filteredClassifier.getRevision());
            List<String> unlabelled = new ArrayList<>();
            unlabelled.add("hello world");

            StringToWordVector stringToWordVector = WekaUtils.loadStr2WordVec(getAssets().open("weka/weka.filter"));
            List<String> res = WekaUtils.predict(unlabelled, stringToWordVector, filteredClassifier, null);
            for (String subres : res) {
                Log.w(TAG, subres);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        } */


        /*SelfAdaptiveLearning.storeNewInstance(getContentResolver(), "T", "Heblle lsds");
        try {
            WekaUtils.init(getContentResolver());
            SelfAdaptiveLearning.doIt(getContentResolver(), (HoeffdingTree) WekaUtils.getWekaModel());
        } catch (Exception exception) {
                exception.printStackTrace();
           }*/
        }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
