package fu.hao.cosmos_xposed.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

public class MyContentProvider extends ContentProvider {
    private static final String PROVIDER_NAME = "fu.hao.cosmos_xposed.utils.MyContentProvider";
    private static final String URL = "content://" + PROVIDER_NAME + "/layout_xml";
    public static final Uri LAYOUT_CONTENT_URI = Uri.parse(URL);
    private static final String MODEL_URL = "content://" + PROVIDER_NAME + "/model";
    public static final Uri MODEL_CONTENT_URI = Uri.parse(MODEL_URL);

    private static final String STR2VEC_URL = "content://" + PROVIDER_NAME + "/filter";
    public static final Uri STR2VEC_CONTENT_URI = Uri.parse(STR2VEC_URL);

    private static final String EVENT_TYPE_URL = "content://" + PROVIDER_NAME + "/event_type";
    public static final Uri EVENT_TYPE_CONTENT_URI = Uri.parse(EVENT_TYPE_URL);

    private static final String WHO_URL = "content://" + PROVIDER_NAME + "/who";
    public static final Uri WHO_CONTENT_URI = Uri.parse(WHO_URL);

    private static final String LAYOUT_DATA_URL = "content://" + PROVIDER_NAME + "/layout";
    public static final Uri LAYOUT_DATA_CONTENT_URI = Uri.parse(LAYOUT_DATA_URL);

    private static final String NEW_INSTANCE_URL = "content://" + PROVIDER_NAME + "/new_instances";
    public static final Uri NEW_INSTANCE_CONTENT_URI = Uri.parse(NEW_INSTANCE_URL);

    private static final String PREDICTION_RES = "content://" + PROVIDER_NAME + "/prediction_res";
    public static final Uri PREDICTION_RES_URI = Uri.parse(PREDICTION_RES);

    public static final String LAYOUT_DATA = "name";
    private static final int LAYOUTS = 100;
    private static final int LAYOUTS_ID = 101;

    public static final String INSTANCE_INDEX = "instanceIndex";
    public static final String INSTANCE_DATA = "instanceData";
    public static final String INSTANCE_LABEL = "instanceLabel";
    private static final int INSTANCES = 200;
    private static final int INSTANCES_ID = 201;

    public static final String PREDICTIONS_INDEX = "predictionIndex";
    public static final String PREDICTIONS_DATA = "predictionData";
    private static final int PREDICTIONS = 300;
    private static final int PREDICTIONS_ID = 301;

    static final UriMatcher uriMatcher;
    private static HashMap<String, String> values;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        uriMatcher.addURI(PROVIDER_NAME, "layout_xml", LAYOUTS);
        uriMatcher.addURI(PROVIDER_NAME, "layout_xml/*", LAYOUTS_ID);

        uriMatcher.addURI(PROVIDER_NAME, "new_instances", INSTANCES);
        uriMatcher.addURI(PROVIDER_NAME, "new_instances/*", INSTANCES_ID);

        uriMatcher.addURI(PROVIDER_NAME, "prediction_res", PREDICTIONS);
        uriMatcher.addURI(PROVIDER_NAME, "prediction_res/*", PREDICTIONS_ID);
    }

    private SQLiteDatabase db;
    private static final String DATABASE_NAME = "db_contentprovider";
    private static final String LAYOUT_TABLE = "layout";
    private static final String INSTANCE_TABLE = "instances";
    private static final String PREDICTIONS_TABLE = "predictions";
    private static final int DATABASE_VERSION = 1;
    private static final String CREATE_LAYOUT_TABLE = "CREATE TABLE " + LAYOUT_TABLE + "("
            //+ " (id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + LAYOUT_DATA + " TEXT NOT NULL" + ");";
    private static final String CREATE_INSTANCE_TABLE = "CREATE TABLE " + INSTANCE_TABLE + "("
            //+ " (id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + INSTANCE_INDEX + " TEXT NOT NULL,"
            + INSTANCE_DATA + " TEXT NOT NULL,"
            + INSTANCE_LABEL + " TEXT NOT NULL"
            + ");";

    private static final String CREATE_PREDICTIONS_TABLE = "CREATE TABLE " + PREDICTIONS_TABLE + "("
            //+ " (id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + PREDICTIONS_INDEX + " TEXT NOT NULL,"
            + PREDICTIONS_DATA + " TEXT NOT NULL"
            + ");";

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count;
        switch (uriMatcher.match(uri)) {
            case LAYOUTS:
                count = db.delete(LAYOUT_TABLE, selection, selectionArgs);
                break;
            case INSTANCES:
                count = db.delete(INSTANCE_TABLE, selection, selectionArgs);
                break;
            case PREDICTIONS:
                count = db.delete(PREDICTIONS_TABLE, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case LAYOUTS:
                return "vnd.android.cursor.dir/cte";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Uri _uri = null;
        switch (uriMatcher.match(uri)) {
            case LAYOUTS:
                long ID1 = db.insert(LAYOUT_TABLE, "", values);
                if (ID1 > 0) {
                    _uri = ContentUris.withAppendedId(LAYOUT_CONTENT_URI, ID1);
                    getContext().getContentResolver().notifyChange(_uri, null);
                    return _uri;
                }
                throw new SQLException("Failed to add a record into " + uri);
            case INSTANCES:
                long ID2 = db.insert(INSTANCE_TABLE, "", values);
                if (ID2 > 0) {
                    _uri = ContentUris.withAppendedId(NEW_INSTANCE_CONTENT_URI, ID2);
                    getContext().getContentResolver().notifyChange(_uri, null);
                    return _uri;
                }
                throw new SQLException("Failed to add a record into " + uri);
            case PREDICTIONS:
                long ID3 = db.insert(PREDICTIONS_TABLE, "", values);
                if (ID3 > 0) {
                    _uri = ContentUris.withAppendedId(PREDICTION_RES_URI, ID3);
                    getContext().getContentResolver().notifyChange(_uri, null);
                    return _uri;
                }
                throw new SQLException("Failed to add a record into " + uri);
            default:
                throw new SQLException("Failed to insert row into " + uri);
        }
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        db = dbHelper.getWritableDatabase();
        if (db != null) {
            return true;
        }
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (uriMatcher.match(uri)) {
            case LAYOUTS:
                qb.setTables(LAYOUT_TABLE);
                qb.setProjectionMap(values);
                if (sortOrder == null || sortOrder == "") {
                    sortOrder = LAYOUT_DATA;
                }
                break;
            case INSTANCES:
                qb.setTables(INSTANCE_TABLE);
                qb.setProjectionMap(values);
                if (sortOrder == null || sortOrder == "") {
                    sortOrder = INSTANCE_INDEX;
                }
                break;
            case PREDICTIONS:
                qb.setTables(PREDICTIONS_TABLE);
                qb.setProjectionMap(values);
                if (sortOrder == null || sortOrder == "") {
                    sortOrder = PREDICTIONS_INDEX;
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        Cursor c = qb.query(db, projection, selection, selectionArgs, null,
                null, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        int count = 0;
        switch (uriMatcher.match(uri)) {
            case LAYOUTS:
                count = db.update(LAYOUT_TABLE, values, selection, selectionArgs);
                break;
            case INSTANCES:
                count = db.update(INSTANCE_TABLE, values, selection, selectionArgs);
                break;
            case PREDICTIONS:
                count = db.update(PREDICTIONS_TABLE, values, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }



    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_LAYOUT_TABLE);
            db.execSQL(CREATE_INSTANCE_TABLE);
            db.execSQL(CREATE_PREDICTIONS_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + LAYOUT_TABLE);
            db.execSQL("DROP TABLE IF EXISTS " + INSTANCE_TABLE);
            db.execSQL("DROP TABLE IF EXISTS " + PREDICTIONS_TABLE);
            onCreate(db);
        }
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File cacheDir = getContext().getCacheDir();
        File privateFile = null;
        if (uri.equals(MODEL_CONTENT_URI)) {
            privateFile = new File(cacheDir, "weka.model");
        } else if (uri.equals(STR2VEC_CONTENT_URI)) {
            privateFile = new File(cacheDir, "weka.filter");
        } else if (uri.equals(LAYOUT_DATA_CONTENT_URI)) {
            privateFile = new File(cacheDir, "layout.data");
        } else if (uri.equals(NEW_INSTANCE_CONTENT_URI)) {
            privateFile = new File(cacheDir, "");
        }

        return ParcelFileDescriptor.open(privateFile, ParcelFileDescriptor.MODE_READ_WRITE);
    }
}