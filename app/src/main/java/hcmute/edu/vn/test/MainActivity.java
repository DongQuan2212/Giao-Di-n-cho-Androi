package hcmute.edu.vn.test;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.BatteryManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private ListView eventListView;
    private ImageView btnAddEvent;
    private EventDatabaseHelper dbHelper;
    private String selectedDate;
    private ArrayAdapter<String> eventAdapter;
    private List<String> eventList = new ArrayList<>();
    private List<Long> eventIds = new ArrayList<>();
    private BatteryReceiver batteryReceiver;
    private IntentFilter batteryFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        // Khởi tạo BatteryReceiver
        batteryReceiver = new BatteryReceiver();
        batteryFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

        registerReceiver(batteryReceiver, batteryFilter);

        // Ánh xạ UI
        calendarView = findViewById(R.id.calendar_View);
        eventListView = findViewById(R.id.ListView_Event);
        btnAddEvent = findViewById(R.id.btn_back_main);
        BottomNavigationView bottomNavigation = findViewById(R.id.nav_bottom);
        dbHelper = new EventDatabaseHelper(this);

        // Khởi tạo Adapter cho ListView
        eventAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, eventList);
        eventListView.setAdapter(eventAdapter);

        // Xử lý sự kiện khi nhấn vào các mục trong thanh điều hướng
        bottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.icon_home) {
                return true; // Đang ở trang Home, không cần làm gì
            } else if (item.getItemId() == R.id.icon_info) {
                // Chuyển sang màn hình EventCompletedActivity
                Intent intent = new Intent(MainActivity.this, EventCompletedActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });

        // Lấy ngày hiện tại để làm mặc định
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        selectedDate = year + "-" + (month + 1) + "-" + day;

        loadEvents(); // Tải danh sách sự kiện

        // Xử lý khi chọn ngày trên CalendarView
        calendarView.setOnDateChangeListener((view, y, m, d) -> {
            selectedDate = y + "-" + (m + 1) + "-" + d;
            loadEvents(); // Tải lại danh sách sự kiện cho ngày mới
        });

        // Xử lý khi nhấn nút thêm sự kiện
        btnAddEvent.setOnClickListener(view -> showEventDialog(null, null, null, null));

        // Xử lý khi chọn sự kiện trong ListView để chỉnh sửa
        eventListView.setOnItemClickListener((parent, view, position, id) -> {
            long eventId = eventIds.get(position);
            editEvent(eventId);
        });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Hủy đăng ký BroadcastReceiver để tránh rò rỉ bộ nhớ
        unregisterReceiver(batteryReceiver);
    }

    public class BatteryReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1); // Lấy mức pin
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1); // Lấy trạng thái sạc

            boolean isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING) ||
                    (status == BatteryManager.BATTERY_STATUS_FULL);

            // Hiển thị thông tin pin
            Toast.makeText(context, "Pin: " + level + "% - " + (isCharging ? "Đang sạc" : "Không sạc"), Toast.LENGTH_SHORT).show();

            // Kiểm tra nếu pin yếu (dưới 15%) và không đang sạc thì hiển thị cảnh báo
            if (level <= 15 && !isCharging) {
                showLowBatteryDialog(context);
            }
        }
    }

    // Hiển thị cảnh báo pin yếu
    private void showLowBatteryDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("⚠️ Cảnh báo pin yếu");
        builder.setMessage("Mức pin của bạn đang thấp! Bạn có muốn bật chế độ tiết kiệm pin không?");

        builder.setPositiveButton("Bật tiết kiệm pin", (dialog, which) -> {
            // Mở cài đặt tiết kiệm pin của hệ thống
            Intent intent = new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS);
            startActivity(intent);
        });

        builder.setNegativeButton("Bỏ qua", (dialog, which) -> {
            dialog.dismiss(); // Đóng hộp thoại
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showEventDialog(Long eventId, String existingTitle, String existingText, String existingTime) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.activity_add_event);

        // Ánh xạ UI trong dialog
        EditText editTextTitle = dialog.findViewById(R.id.txt_title);
        EditText editTextEvent = dialog.findViewById(R.id.txt_description);
        Button buttonSave = dialog.findViewById(R.id.btn_save);
        Button buttonDelete = dialog.findViewById(R.id.btn_delete);
        Button buttonTime = dialog.findViewById(R.id.btn_setTime);
        Button buttonComplete = dialog.findViewById(R.id.btn_complete);
        TextView textViewTime = dialog.findViewById(R.id.txt_time);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // Làm trong suốt nền dialog
        }

        // Nếu chỉnh sửa sự kiện, điền dữ liệu cũ vào các ô nhập
        if (existingTitle != null) {
            editTextTitle.setText(existingTitle);
            editTextEvent.setText(existingText);
            textViewTime.setText(existingTime != null ? existingTime : "Chưa chọn giờ");
            buttonDelete.setVisibility(View.VISIBLE); // Hiện lên lại nút Xóa
            buttonComplete.setVisibility(View.VISIBLE); // hiện lên lại nút hoàn thành
        } else { // ngược lại khi ấn vào nút thêm thì ẩn
            buttonDelete.setVisibility(View.GONE);
            buttonComplete.setVisibility(View.GONE);
        }

        // Xử lý chọn thời gian
        buttonTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR);
            int minute = calendar.get(Calendar.MINUTE);
            boolean isPM = calendar.get(Calendar.AM_PM) == Calendar.PM;

            // tạo ra layout chọn time cho event
            TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, selectedHour, selectedMinute) -> {
                String amPm = (selectedHour >= 12) ? "PM" : "AM";
                int displayHour = (selectedHour == 0) ? 12 : (selectedHour > 12 ? selectedHour - 12 : selectedHour);
                String formattedTime = String.format("%02d:%02d %s", displayHour, selectedMinute, amPm);
                textViewTime.setText(formattedTime);
            }, hour, minute, false); // false để hiển thị AM/PM

            timePickerDialog.show();
        });


        // Xử lý lưu sự kiện
        buttonSave.setOnClickListener(v -> {
            String title = editTextTitle.getText().toString().trim();
            String description = editTextEvent.getText().toString().trim();
            String time = textViewTime.getText().toString();

            if (title.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tiêu đề", Toast.LENGTH_SHORT).show();
                return;
            }

            // lưu vào database
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("date", selectedDate);
            values.put("title", title);
            values.put("event", description);
            values.put("time", time.equals("Chưa chọn giờ") ? null : time);

            if (eventId == null) {
                db.insert("events", null, values);
                Toast.makeText(this, "Đã thêm sự kiện", Toast.LENGTH_SHORT).show();
            } else {
                db.update("events", values, "id = ?", new String[]{String.valueOf(eventId)});
                Toast.makeText(this, "Đã cập nhật sự kiện", Toast.LENGTH_SHORT).show();
            }

            db.close();
            dialog.dismiss();
            loadEvents(); // Tải lại danh sách sau khi thêm/chỉnh sửa
        });

        // Xử lý xóa sự kiện
        buttonDelete.setOnClickListener(v -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete("events", "id = ?", new String[]{String.valueOf(eventId)});
            db.close();
            Toast.makeText(this, "Đã xóa sự kiện", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            loadEvents(); // Cập nhật danh sách
        });

        // Xử lý đánh dấu hoàn thành
        buttonComplete.setOnClickListener(v -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("status", 1); // Đánh dấu hoàn thành

            db.update("events", values, "id = ?", new String[]{String.valueOf(eventId)});
            db.close();
            Toast.makeText(this, "Sự kiện đã hoàn thành", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            loadEvents(); // Cập nhật danh sách
        });
    }



    // Chỉnh sửa sự kiện
    private void editEvent(long eventId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase(); // lấy thông tin sự kiện từ database
        // Truy vấn id của sự kiện
        Cursor cursor = db.query("events", new String[]{"title", "event", "time"}, "id = ?", new String[]{String.valueOf(eventId)}, null, null, null);
        if (cursor.moveToFirst()) {
            showEventDialog(eventId, cursor.getString(0), cursor.getString(1), cursor.getString(2));
        }
        cursor.close();
        db.close();
    }

    // Tải danh sách sự kiện từ database
    private void loadEvents() {
        eventList.clear();
        eventIds.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("events", new String[]{"id", "title", "time"}, "date = ?", new String[]{selectedDate}, null, null, null);
        while (cursor.moveToNext()) {
            long id = cursor.getLong(0);
            String title = cursor.getString(1);
            String time = cursor.getString(2);
            eventList.add((time == null || time.isEmpty()) ? title : time + " - " + title);
            eventIds.add(id);
        }
        cursor.close();
        db.close();
        eventAdapter.notifyDataSetChanged();
    }

    // Lớp SQLiteOpenHelper để quản lý database
    // sau này sẽ tạo model cho event này sau
    private static class EventDatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "events.db";
        private static final int DATABASE_VERSION = 4;

        public EventDatabaseHelper(MainActivity context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE events (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT, title TEXT, event TEXT, time TEXT, status INTEGER DEFAULT 0)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < 4) {
                db.execSQL("ALTER TABLE events ADD COLUMN status INTEGER DEFAULT 0");
            }
        }
    }
}
