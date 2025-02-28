package hcmute.edu.vn.test;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class EventCompletedActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completed_events);
        ImageView btnBack = findViewById(R.id.btn_back_main);
        btnBack.setOnClickListener(v -> finish());
    }
}
