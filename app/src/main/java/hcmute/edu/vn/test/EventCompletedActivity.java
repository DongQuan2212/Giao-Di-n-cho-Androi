package hcmute.edu.vn.test;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class EventCompletedActivity extends AppCompatActivity {
    private ListView completedEventListView;
    private EventDatabaseHelper dbHelper;
    private CompletedEventAdapter eventAdapter;
    private List<Event> completedEvents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completed_events);

        completedEventListView = findViewById(R.id.completedEventListView);
        ImageView btnBack = findViewById(R.id.btn_back_main);

        dbHelper = new EventDatabaseHelper(this);
        loadCompletedEvents();

        btnBack.setOnClickListener(v -> finish());
    }

    private void loadCompletedEvents() {
        completedEvents = dbHelper.getCompletedEvents();
        eventAdapter = new CompletedEventAdapter(this, completedEvents, dbHelper);
        completedEventListView.setAdapter(eventAdapter);
    }
}
