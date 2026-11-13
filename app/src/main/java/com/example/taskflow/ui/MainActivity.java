package com.example.taskflow.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.taskflow.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * Màn hình chính: Bottom Navigation với 4 tab
 * (Tổng quan, Công việc, Lịch, Thống kê).
 * Nút + mở màn hình Tạo nhiệm vụ. Hồ sơ/Cài đặt mở bằng cách
 * nhấn avatar "K" ở góc phải các màn hình (ProfileActivity).
 */
public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private FloatingActionButton fabAdd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottomNav);
        fabAdd = findViewById(R.id.fabAdd);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                showFragment(new HomeFragment());
                fabAdd.setVisibility(View.VISIBLE);
                return true;
            } else if (id == R.id.nav_tasks) {
                showFragment(new TaskListFragment());
                fabAdd.setVisibility(View.VISIBLE);
                return true;
            } else if (id == R.id.nav_calendar) {
                showFragment(new CalendarFragment());
                fabAdd.setVisibility(View.GONE);
                return true;
            } else if (id == R.id.nav_stats) {
                showFragment(new StatisticsFragment());
                fabAdd.setVisibility(View.GONE);
                return true;
            }
            return false;
        });

        bottomNav.setSelectedItemId(R.id.nav_home);

        fabAdd.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AddEditTaskActivity.class)));
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    /** Chuyển sang tab Lịch (dùng khi nhấn "Xem lịch" ở màn Tổng quan). */
    public void switchToCalendarTab() {
        bottomNav.setSelectedItemId(R.id.nav_calendar);
    }
}
