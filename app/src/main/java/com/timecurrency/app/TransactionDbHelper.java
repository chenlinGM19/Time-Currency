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
    
    /**
     * Imports a transaction. Prevents duplicates by checking if a transaction with the 
     * exact same timestamp already exists.
     */
    public static boolean importTransaction(Context context, long timestamp, int delta) {
        try (TransactionDbHelper dbHelper = new TransactionDbHelper(context)) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            
            // Check for duplicate based on timestamp
            Cursor cursor = db.query(TABLE_NAME, new String[]{COLUMN_ID}, 
                    COLUMN_TIMESTAMP + " = ?", 
                    new String[]{String.valueOf(timestamp)}, null, null, null);
            
            boolean exists = cursor.getCount() > 0;
            cursor.close();
            
            if (!exists) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_TIMESTAMP, timestamp);
                values.put(COLUMN_DELTA, delta);
                // We don't necessarily know the total snapshot during import until we recalculate all,
                // so we can leave it 0 or approximate it. 
                // However, the most important part is the delta and timestamp.
                values.put(COLUMN_TOTAL_SNAPSHOT, 0); 
                db.insert(TABLE_NAME, null, values);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public static int calculateTotalBalance(Context context) {
        int total = 0;
        try (TransactionDbHelper dbHelper = new TransactionDbHelper(context)) {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT SUM(" + COLUMN_DELTA + ") FROM " + TABLE_NAME, null);
            if (cursor.moveToFirst()) {
                total = cursor.getInt(0);
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return total;
    }
    
    public static int calculateDailyBalance(Context context, long startTimeMillis) {
        int total = 0;
        try (TransactionDbHelper dbHelper = new TransactionDbHelper(context)) {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT SUM(" + COLUMN_DELTA + ") FROM " + TABLE_NAME + 
                    " WHERE " + COLUMN_TIMESTAMP + " >= ?", new String[]{String.valueOf(startTimeMillis)});
            if (cursor.moveToFirst()) {
                total = cursor.getInt(0);
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return total;
    }
    
    public static Cursor getAllTransactions(Context context) {
        TransactionDbHelper dbHelper = new TransactionDbHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        // Sort by timestamp DESC
        return db.query(TABLE_NAME, null, null, null, null, null, COLUMN_TIMESTAMP + " DESC");
    }
}