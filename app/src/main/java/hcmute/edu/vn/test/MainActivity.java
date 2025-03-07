package hcmute.edu.vn.test;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Calendar;
import java.util.List;

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
        } else {
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

        buttonSave.setOnClickListener(v -> {
            String title = editTextTitle.getText().toString().trim();
            String description = editTextEvent.getText().toString().trim();
            String time = textViewTime.getText().toString();

            if (title.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tiêu đề", Toast.LENGTH_SHORT).show();
                return;
            }

            dbHelper.saveEvent(eventId, selectedDate, title, description, time);
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


    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationView bottomNavigation = findViewById(R.id.nav_bottom);
        bottomNavigation.setSelectedItemId(R.id.icon_home);
    }
}
