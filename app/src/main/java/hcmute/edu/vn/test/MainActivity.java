package hcmute.edu.vn.test;

import android.app.AlarmManager;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private CalendarView calendarView;
    private ListView eventListView;
    private ImageView btnAddEvent;
    private EventDatabaseHelper dbHelper;
    private String selectedDate;
    private EventAdapter eventAdapter;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createNotificationChannel();
        checkNotificationPermission();

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }

        // Ánh xạ View
        calendarView = findViewById(R.id.calendar_View);
        eventListView = findViewById(R.id.ListView_Event);
        btnAddEvent = findViewById(R.id.btn_back_main);
        BottomNavigationView bottomNavigation = findViewById(R.id.nav_bottom);
        dbHelper = new EventDatabaseHelper(this);

        // Lấy ngày hiện tại
        Calendar calendar = Calendar.getInstance();
        selectedDate = calendar.get(Calendar.YEAR) + "-" + (calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.DAY_OF_MONTH);

        // Load sự kiện
        loadEvents();

        // Xử lý chọn ngày trên CalendarView
        calendarView.setOnDateChangeListener((view, y, m, d) -> {
            selectedDate = y + "-" + (m + 1) + "-" + d;
            loadEvents();
        });

        // Xử lý khi nhấn nút thêm sự kiện
        btnAddEvent.setOnClickListener(view -> showEventDialog(null, null, null, null));

        // Xử lý khi chọn sự kiện trong danh sách
        eventListView.setOnItemClickListener((parent, view, position, id) -> {
            Event event = (Event) eventAdapter.getItem(position);
            editEvent(event.getId());
        });


        // Xử lý Bottom Navigation
        bottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.icon_home) {
                return true;
            } else if (item.getItemId() == R.id.icon_info) {
                startActivity(new Intent(MainActivity.this, EventCompletedActivity.class));
                return true;
            }
            return false;
        });
    }



    private void showEventDialog(Long eventId, String existingTitle, String existingText, String existingTime) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.activity_add_event);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        EditText editTextTitle = dialog.findViewById(R.id.txt_title);
        EditText editTextEvent = dialog.findViewById(R.id.txt_description);
        View view1 = dialog.findViewById(R.id.kc1);
        View view2 = dialog.findViewById(R.id.kc2);
        Button buttonSave = dialog.findViewById(R.id.btn_save);
        Button buttonDelete = dialog.findViewById(R.id.btn_delete);
        Button buttonComplete = dialog.findViewById(R.id.btn_complete);
        Button buttonTime = dialog.findViewById(R.id.btn_setTime);
        TextView textViewTime = dialog.findViewById(R.id.txt_time);

        dialog.show();

        if (existingTitle != null) {
            editTextTitle.setText(existingTitle);
            editTextEvent.setText(existingText);
            textViewTime.setText(existingTime != null ? existingTime : "Chưa chọn giờ");
            buttonDelete.setVisibility(View.VISIBLE);
            buttonComplete.setVisibility(View.VISIBLE);
            view1.setVisibility(View.VISIBLE);
            view2.setVisibility(View.VISIBLE);
        } else {
            view1.setVisibility(View.GONE);
            view2.setVisibility(View.GONE);
            buttonDelete.setVisibility(View.GONE);
            buttonComplete.setVisibility(View.GONE);
        }

        buttonTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);
            new TimePickerDialog(this, (view, selectedHour, selectedMinute) -> {
                String amPm = (selectedHour >= 12) ? "PM" : "AM";
                int displayHour = (selectedHour == 0) ? 12 : (selectedHour > 12 ? selectedHour - 12 : selectedHour);
                textViewTime.setText(String.format("%02d:%02d %s", displayHour, selectedMinute, amPm));
            }, hour, minute, false).show();
        });

        final Long[] eventIdWrapper = {eventId}; // Dùng mảng để chứa eventId

        buttonSave.setOnClickListener(v -> {
            String title = editTextTitle.getText().toString().trim();
            String description = editTextEvent.getText().toString().trim();
            String time = textViewTime.getText().toString();

            if (title.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tiêu đề", Toast.LENGTH_SHORT).show();
                return;
            }

            long timeInMillis = getTimeInMillis(selectedDate, time);
            if (timeInMillis == -1 || timeInMillis <= System.currentTimeMillis()) {
                Toast.makeText(this, "Vui lòng chọn thời gian hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }

            if (eventIdWrapper[0] == null) { // Thêm sự kiện mới
                eventIdWrapper[0] = dbHelper.saveEvent(selectedDate, title, description, time);
                if (eventIdWrapper[0] == -1) {
                    Toast.makeText(this, "Lỗi khi lưu sự kiện!", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else { // Cập nhật sự kiện cũ
                dbHelper.updateEvent(eventIdWrapper[0], selectedDate, title, description, time);
            }

            // Tạo đối tượng Event
            Event newEvent = new Event(eventIdWrapper[0], selectedDate, title, description, time, 0);

            // Đặt báo thức
            setEventReminder(newEvent);

            dialog.dismiss();
            loadEvents();
        });







        buttonComplete.setOnClickListener(v -> {
            if (eventId != null) {
                dbHelper.markEventAsCompleted(eventId);
                dialog.dismiss();
                loadEvents();
                Toast.makeText(this, "Sự kiện đã được đánh dấu hoàn thành!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Sự kiện không hợp lệ", Toast.LENGTH_SHORT).show();
            }
        });

        buttonDelete.setOnClickListener(v -> {
            if (eventId != null) {
                dbHelper.deleteEvent(eventId);
                dialog.dismiss();
                loadEvents();
            } else {
                Toast.makeText(this, "Sự kiện không hợp lệ", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void editEvent(long eventId) {
        Event event = dbHelper.getEventById(eventId);
        if (event != null) {
            showEventDialog(eventId, event.getTitle(), event.getDescription(), event.getTime());
        }
    }

    private void loadEvents() {
        List<Event> eventList = dbHelper.getEventsByDate(selectedDate);
        eventAdapter = new EventAdapter(this, eventList);
        eventListView.setAdapter(eventAdapter);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("event_channel", "Lịch Nhắc Nhở", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Thông báo sự kiện");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    private void setEventReminder(Event event) {
        long timeInMillis = getTimeInMillis(event.getDate(), event.getTime());

        if (timeInMillis <= System.currentTimeMillis()) {
            Log.e("setEventReminder", "Thời gian đã qua, không đặt báo thức!");
            return;
        }

        Intent intent = new Intent(this, EventReminderReceiver.class);
        intent.putExtra("event_id", event.getId());
        intent.putExtra("event_title", event.getTitle());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, (int) event.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
            }
            Log.d("setEventReminder", "Báo thức đặt thành công cho sự kiện: " + event.getTitle());
        }
    }



    public void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Chỉ yêu cầu trên Android 13+
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Permission", "POST_NOTIFICATIONS granted!");
            } else {
                Log.e("Permission", "POST_NOTIFICATIONS denied!");
            }
        }
    }
    private long getTimeInMillis(String date, String time) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());
            Date dateTime = sdf.parse(date + " " + time);
            return dateTime != null ? dateTime.getTime() : -1;
        } catch (ParseException e) {
            e.printStackTrace();
            return -1;
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationView bottomNavigation = findViewById(R.id.nav_bottom);
        bottomNavigation.setSelectedItemId(R.id.icon_home);
    }
}
