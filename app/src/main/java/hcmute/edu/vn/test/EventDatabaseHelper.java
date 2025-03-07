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
    private static final int DATABASE_VERSION = 5;  // Cập nhật version để tránh lỗi
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

    // Xóa sự kiện
    public void deleteEvent(long eventId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_EVENTS, "id = ?", new String[]{String.valueOf(eventId)});
        db.close();
    }

    // Thêm hoặc cập nhật sự kiện
    public void saveEvent(Long eventId, String date, String title, String description, String time) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("date", date);
        values.put("title", title);
        values.put("description", description);  // ✅ Đổi event -> description
        values.put("time", time.equals("Chưa chọn giờ") ? null : time);
        values.put("status", 0);
        Log.d("Database", "Lưu sự kiện: Title=" + title + ", Date=" + date + ", Description=" + description + ", Time=" + time);
        if (eventId == null) {
            db.insert(TABLE_EVENTS, null, values);
            Log.e("Database", "Đã thêm sự kiện.");
        } else {
            db.update(TABLE_EVENTS, values, "id = ?", new String[]{String.valueOf(eventId)});
            Log.e("Database", "Đã cập nhật sự kiện.");
        }
        db.close();
    }


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
            Log.e("Database", "Không tìm thấy sự kiện.");
        }
    }


    //
    private String getEventDate(long eventId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String date = null;

        Cursor cursor = db.query(TABLE_EVENTS, new String[]{"date"}, "id = ?", new String[]{String.valueOf(eventId)}, null, null, null);
        if (cursor.moveToFirst()) {
            date = cursor.getString(0);
        }
        cursor.close();
        return date;
    }



    // Lấy một sự kiện theo ID
    public Event getEventById(long eventId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Event event = null;

        Cursor cursor = db.query(TABLE_EVENTS, new String[]{"id", "date", "title", "description", "time", "status"},
                "id = ?", new String[]{String.valueOf(eventId)}, null, null, null);

        if (cursor.moveToFirst()) {
            event = new Event(
                    cursor.getLong(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4),
                    cursor.getInt(5)
            );
        }
        cursor.close();
        db.close();
        return event;
    }

    // Lấy danh sách sự kiện đã hoàn thành
    public List<Event> getCompletedEvents() {
        List<Event> eventList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_EVENTS + " WHERE status = 1", null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Event event = new Event();
                event.setId(cursor.getLong(0));
                event.setDate(cursor.getString(1));
                event.setTitle(cursor.getString(2));
                event.setDescription(cursor.getString(3));
                event.setTime(cursor.getString(4));
                event.setStatus(cursor.getInt(5));
                event.setCompletedDate(cursor.getString(6));
                event.setCompletedTime(cursor.getString(7));
                eventList.add(event);
            }
            cursor.close();
        }

        return eventList; // ✅ Luôn trả về danh sách, không bao giờ null
    }



    // Lấy danh sách sự kiện theo ngày
    public List<Event> getEventsByDate(String date) {
        List<Event> events = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Chỉ lấy những sự kiện có status = 0 (chưa hoàn thành)
        Cursor cursor = db.query(TABLE_EVENTS, null, "date = ? AND status = 0",
                new String[]{date}, null, null, null);

        Log.d("Database", "Truy vấn sự kiện cho ngày: " + date);

        while (cursor.moveToNext()) {
            long id = cursor.getLong(0);
            String eventDate = cursor.getString(1);
            String title = cursor.getString(2);
            String event = cursor.getString(3);
            String time = cursor.getString(4);
            int status = cursor.getInt(5);

            Log.d("Database", "Lấy sự kiện: ID=" + id + ", Date=" + eventDate + ", Title=" + title + ", Event=" + event + ", Time=" + time);

            events.add(new Event(id, eventDate, title, event, time, status));
        }

        cursor.close();
        db.close();
        return events;
    }



}
