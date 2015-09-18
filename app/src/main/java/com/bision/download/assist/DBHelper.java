package com.bision.download.assist;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.bision.download.bean.ChunkInfo;
import com.bision.download.core.Request;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by oeager on 2015/2/27.
 */
public class DBHelper extends SQLiteOpenHelper {


    public static final String DB_NAME = "download.db";

    public static final int DB_VERSION = 1;

    public static final String TABLE_REQUEST = "table_request";

    public static final String TABLE_CHUNK = "table_chunk";

    public static final String COLUMN_ID = "_id";

    public static final String COLUMN_PATH = "file_path";

    public static final String LINK_URL = "link_url";

    public static final String COLUMN_COUNT = "chunk_count";

    public static final String COLUMN_REQUEST_ID = "request_id";

    public static final String COLUMN_CHUNK_INDEX = "chunk_index";

    public static final String COLUMN_START_POS = "start_pos";

    public static final String COLUMN_END_POS = "end_pos";

    public static final String COLUMN_AMOUNT = "amount";


    private static final String CREATE_TASK_TABLE = "CREATE TABLE "
            + TABLE_REQUEST + "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + LINK_URL + " TEXT NOT NULL DEFAULT '' ,"
            + COLUMN_PATH + " TEXT NOT NULL DEFAULT '' ," + COLUMN_COUNT
            + " INTEGER NOT NULL DEFAULT 1 )";


    private static final String CREATE_THREAD_TABLE = "CREATE TABLE "
            + TABLE_CHUNK + "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_REQUEST_ID + " INTEGER NOT NULL ," + COLUMN_CHUNK_INDEX
            + " INTEGER NOT NULL ," + COLUMN_START_POS
            + " INTEGER NOT NULL ," + COLUMN_END_POS
            + " INTEGER NOT NULL ," + COLUMN_AMOUNT
            + " INTEGER NOT NULL DEFAULT 0)";

    private static final String UPDATE_AMOUNT = "UPDATE " + TABLE_CHUNK
            + " SET " + COLUMN_AMOUNT + " = " + COLUMN_AMOUNT
            + " + ? WHERE " + COLUMN_ID + " = ? ";

    public DBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory,
                    int version) {
        super(context, name, factory, version);
    }

    public DBHelper(Context context) {

        this(context, DB_NAME, null, DB_VERSION);
    }

    /**
     * 创建库
     */
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(CREATE_TASK_TABLE);
        db.execSQL(CREATE_THREAD_TABLE);


    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        if (newVersion > oldVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_REQUEST);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHUNK);
            db.execSQL(CREATE_TASK_TABLE);
            db.execSQL(CREATE_THREAD_TABLE);
        }

    }

    public void insertChunkInfo(int request_id, List<ChunkInfo> cks) {
        if (request_id > 0 && cks != null) {
            SQLiteDatabase db = getWritableDatabase();
            int size = cks.size();
            for (int i = 0; i < size; i++) {
                ChunkInfo ck = cks.get(i);
                ContentValues cv = new ContentValues();
                cv.put(COLUMN_REQUEST_ID, request_id);
                cv.put(COLUMN_CHUNK_INDEX, ck.index);
                cv.put(COLUMN_START_POS, ck.startPos);
                cv.put(COLUMN_END_POS, ck.endPos);
                cv.put(COLUMN_AMOUNT, ck.amount);
                ck.id= db.insert(TABLE_CHUNK, null, cv);
            }

        } else {
            throw  new IllegalArgumentException("request id = "+request_id+",cks is "+cks==null?"null":"not null");

        }


    }

    public int insertRequestInfo(Request r) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(LINK_URL, r.getLinkUrl());
        cv.put(COLUMN_PATH, r.getFilePath());
        cv.put(COLUMN_COUNT, r.getChunkCount());
        return (int) db.insert(TABLE_REQUEST, null, cv);

    }

    public void updateChunk(long chunkId, int amount) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(UPDATE_AMOUNT, new Object[]{amount, chunkId});
    }

    public int getMatchRequestId(Request r) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_REQUEST, new String[]{COLUMN_ID}, LINK_URL + " = ? and " + COLUMN_PATH + " = ? and " + COLUMN_COUNT + " = ?", new String[]{r.getLinkUrl(), r.getFilePath(), String.valueOf(r.getChunkCount())}, null, null, null);
        if (c != null && c.moveToFirst()) {
            int id = c.getInt(0);
            c.close();
            return id;
        }
        return -1;
    }

    public List<ChunkInfo> getChunksById(int requestId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c2 = db.query(TABLE_CHUNK, new String[]{COLUMN_ID,COLUMN_CHUNK_INDEX, COLUMN_START_POS, COLUMN_END_POS, COLUMN_AMOUNT}, COLUMN_REQUEST_ID + " = ?", new String[]{String.valueOf(requestId)}, null, null, null);
        if (c2 != null) {
            List<ChunkInfo> chunks = new ArrayList<>();
            while (c2.moveToNext()) {
                ChunkInfo info = new ChunkInfo();
                info.id = c2.getInt(0);
                info.index = c2.getInt(1);
                info.startPos = c2.getInt(2);
                info.endPos = c2.getInt(3);
                info.amount = c2.getInt(4);
                chunks.add(info);
            }
            c2.close();
            return chunks;
        }
        return null;

    }

    public void deleteOldData(String linkUrl, String filePath) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.query(TABLE_REQUEST, new String[]{COLUMN_ID}, LINK_URL + " = ? and " + COLUMN_PATH + " = ?", new String[]{linkUrl, filePath}, null, null, null, null);
        if (c != null && c.moveToFirst()) {
            int id = c.getInt(0);
            c.close();
            db.delete(TABLE_REQUEST, COLUMN_ID + "= ? ", new String[]{String.valueOf(id)});
            db.delete(TABLE_CHUNK, COLUMN_REQUEST_ID + " = ?", new String[]{String.valueOf(id)});
        }

    }

}
