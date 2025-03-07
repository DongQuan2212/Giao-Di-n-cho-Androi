package hcmute.edu.vn.test;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "events.db";
    private static final int DATABASE_VERSION = 6; // ✅ Tăng version nếu có thay đổi schema
    private static final String TABLE_EVENTS = "events";

    public EventDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_EVENTS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "date TEXT, " +
                "title TEXT, " +
                "description TEXT, " +
                "time TEXT, " +
                "status INTEGER DEFAULT 0, " +
                "completed_date TEXT, " +
                "completed_time TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE " + TABLE_EVENTS + " ADD COLUMN completed_time TEXT");
        }
    }

    // ✅ Xóa sự kiện
    public void deleteEvent(long eventId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_EVENTS, "id = ?", new String[]{String.valueOf(eventId)});
    }

    // ✅ Thêm hoặc cập nhật sự kiện
    public long saveEvent(String date, String title, String description, String time) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("date", date);
        values.put("title", title);
        values.put("description", description);
        values.put("time", time);

        long eventId = db.insert("events", null, values); // Lưu sự kiện vào database
        db.close();
        return eventId; // Trả về ID của sự kiện vừa lưu
    }

    //  Cập nhật sự kiện theo ID
    public void updateEvent(long eventId, String date, String title, String description, String time) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("date", date);
        values.put("title", title);
        values.put("description", description);
        values.put("time", time);

        int rows = db.update(TABLE_EVENTS, values, "id = ?", new String[]{String.valueOf(eventId)});
        if (rows > 0) {
            Log.d("Database", "Sự kiện ID " + eventId + " đã được cập nhật.");
        } else {
            Log.e("Database", "Không tìm thấy sự kiện để cập nhật.");
        }
    }



    // Đánh dấu sự kiện hoàn thành
    public void markEventAsCompleted(long eventId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String currentTime = sdf.format(new Date());

        values.put("status", 1);
        values.put("completed_date", getEventDate(eventId));
        values.put("completed_time", currentTime);

        int rows = db.update(TABLE_EVENTS, values, "id = ?", new String[]{String.valueOf(eventId)});
        if (rows > 0) {
            Log.d("Database", "Sự kiện ID " + eventId + " đã hoàn thành lúc " + currentTime);
        } else {
            Log.e("Database", "Không tìm thấy sự kiện để đánh dấu hoàn thành.");
        }
    }

    // ✅ Lấy ngày của sự kiện theo ID
    private String getEventDate(long eventId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String date = null;
        Cursor cursor = db.query(TABLE_EVENTS, new String[]{"date"}, "id = ?", new String[]{String.valueOf(eventId)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            date = cursor.getString(0);
            cursor.close();
        }
        return date;
    }

    // ✅ Lấy một sự kiện theo ID
    public Event getEventById(long eventId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Event event = null;
        Cursor cursor = db.query(TABLE_EVENTS, new String[]{"id", "date", "title", "description", "time", "status"},
                "id = ?", new String[]{String.valueOf(eventId)}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            event = new Event(
                    cursor.getLong(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4),
                    cursor.getInt(5)
            );
            cursor.close();
        }
        return event;
    }

    // ✅ Lấy danh sách sự kiện đã hoàn thành
    public List<Event> getCompletedEvents() {
        List<Event> eventList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_EVENTS + " WHERE status = 1", null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                Event event = new Event(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getInt(5),
                        cursor.getString(6),
                        cursor.getString(7)
                );
                eventList.add(event);
            }
            cursor.close();
        }
        return eventList;
    }

    // ✅ Lấy danh sách sự kiện theo ngày
    public List<Event> getEventsByDate(String date) {
        List<Event> events = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_EVENTS, null, "date = ? AND status = 0",
                new String[]{date}, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                events.add(new Event(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getInt(5)
                ));
            }
            cursor.close();
        }
        return events;
    }
}
