package com.timecurrency.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class TransactionDbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "TimeCurrency.db";

    public static final String TABLE_NAME = "transactions";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_DELTA = "delta";
    public static final String COLUMN_TOTAL_SNAPSHOT = "total_snapshot";

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY," +
                    COLUMN_TIMESTAMP + " INTEGER," +
                    COLUMN_DELTA + " INTEGER," +
                    COLUMN_TOTAL_SNAPSHOT + " INTEGER)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + TABLE_NAME;

    public TransactionDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
    
    public static void logTransaction(Context context, int delta, int newTotal) {
        try (TransactionDbHelper dbHelper = new TransactionDbHelper(context)) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_TIMESTAMP, System.currentTimeMillis());
            values.put(COLUMN_DELTA, delta);
            values.put(COLUMN_TOTAL_SNAPSHOT, newTotal);
            db.insert(TABLE_NAME, null, values);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static Cursor getAllTransactions(Context context) {
        TransactionDbHelper dbHelper = new TransactionDbHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        // Sort by timestamp DESC
        return db.query(TABLE_NAME, null, null, null, null, null, COLUMN_TIMESTAMP + " DESC");
    }
}