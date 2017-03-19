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
    static final String PROVIDER_NAME = "fu.hao.cosmos_xposed.utils.MyContentProvider";
    static final String URL = "content://" + PROVIDER_NAME + "/skholinguacp";
    public static final Uri LAYOUT_CONTENT_URI = Uri.parse(URL);
    static final String MODEL_URL = "content://" + PROVIDER_NAME + "/model";
    public static final Uri MODEL_CONTENT_URI = Uri.parse(MODEL_URL);

    static final String STR2VEC_URL = "content://" + PROVIDER_NAME + "/filter";
    public static final Uri STR2VEC_CONTENT_URI = Uri.parse(STR2VEC_URL);

    static final String EVENT_TYPE_URL = "content://" + PROVIDER_NAME + "/event_type";
    public static final Uri EVENT_TYPE_CONTENT_URI = Uri.parse(EVENT_TYPE_URL);

    static final String WHO_URL = "content://" + PROVIDER_NAME + "/who";
    public static final Uri WHO_CONTENT_URI = Uri.parse(WHO_URL);

    static final String LAYOUT_DATA_URL = "content://" + PROVIDER_NAME + "/layout";
    public static final Uri LAYOUT_DATA_CONTENT_URI = Uri.parse(LAYOUT_DATA_URL);

    private static final String NEW_INSTANCE_URL = "content://" + PROVIDER_NAME + "/new_instances";
    public static final Uri NEW_INSTANCE_CONTENT_URI = Uri.parse(NEW_INSTANCE_URL);
    public static final String INSTANCE_DATA = "instanceData";
    public static final String INSTANCE_LABEL = "instanceLabel";

    public static final String NAME = "name";
    static final int uriCode = 1;
    static final UriMatcher uriMatcher;
    private static HashMap<String, String> values;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, "skholinguacp", uriCode);
        uriMatcher.addURI(PROVIDER_NAME, "skholinguacp/*", uriCode);

        uriMatcher.addURI(PROVIDER_NAME, "model", uriCode);
        uriMatcher.addURI(PROVIDER_NAME, "model/*", uriCode);

        uriMatcher.addURI(PROVIDER_NAME, "filter", uriCode);
        uriMatcher.addURI(PROVIDER_NAME, "filter/*", uriCode);

        uriMatcher.addURI(PROVIDER_NAME, "event_type", uriCode);
        uriMatcher.addURI(PROVIDER_NAME, "event_type/*", uriCode);

        uriMatcher.addURI(PROVIDER_NAME, "who", uriCode);
        uriMatcher.addURI(PROVIDER_NAME, "who/*", uriCode);

        uriMatcher.addURI(PROVIDER_NAME, "layout", uriCode);
        uriMatcher.addURI(PROVIDER_NAME, "layout/*", uriCode);

        uriMatcher.addURI(PROVIDER_NAME, "new_instances", uriCode);
        uriMatcher.addURI(PROVIDER_NAME, "new_instances/*", uriCode);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count;
        switch (uriMatcher.match(uri)) {
            case uriCode:
                count = db.delete(TABLE_NAME, selection, selectionArgs);
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
            case uriCode:
                return "vnd.android.cursor.dir/cte";

            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long rowID = db.insert(TABLE_NAME, "", values);
        if (rowID > 0) {
            Uri _uri = ContentUris.withAppendedId(LAYOUT_CONTENT_URI, rowID);
            getContext().getContentResolver().notifyChange(_uri, null);
            return _uri;
        }
        throw new SQLException("Failed to add a record into " + uri);
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
        qb.setTables(TABLE_NAME);

        switch (uriMatcher.match(uri)) {
            case uriCode:
                qb.setProjectionMap(values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        if (sortOrder == null || sortOrder == "") {
            sortOrder = NAME;
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
            case uriCode:
                count = db.update(TABLE_NAME, values, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    private SQLiteDatabase db;
    static final String DATABASE_NAME = "db_contentprovider";
    static final String TABLE_NAME = "names";
    static final int DATABASE_VERSION = 1;
    static final String CREATE_DB_TABLE = " CREATE TABLE " + TABLE_NAME
            + " (id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + " name TEXT NOT NULL);";

    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_DB_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
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

        return ParcelFileDescriptor.open(privateFile, ParcelFileDescriptor.MODE_READ_ONLY);
    }
}