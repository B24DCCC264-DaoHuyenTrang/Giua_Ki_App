package com.example.taskflow.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.taskflow.R;
import com.example.taskflow.database.TaskFileDatabase;
import com.example.taskflow.model.Task;
import com.example.taskflow.utils.FormatUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Tab 3 — Lịch: xem công việc theo ngày.
 * - Thanh tháng (Tháng 11, 2026) với nút < > đổi tháng.
 * - Dải tuần T2..CN của tuần chứa ngày đang chọn; ngày có việc hiện chấm tím.
 * - Timeline các việc trong ngày đã chọn, theo giờ.
 * - Nút "+ Thêm công việc" mở màn Tạo nhiệm vụ với ngày đã chọn sẵn.
 */
public class CalendarFragment extends Fragment {

    private TaskFileDatabase db;
    private CalendarTaskAdapter adapter;
    private TextView tvMonthYear;
    private TextView tvDayTitle;
    private TextView tvDayCount;
    private TextView tvEmpty;
    private LinearLayout llWeekStrip;

    private long selectedDate; // đầu ngày đang chọn

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = new TaskFileDatabase(requireContext());
        selectedDate = FormatUtils.startOfDay(System.currentTimeMillis());

        tvMonthYear = view.findViewById(R.id.tvMonthYear);
        tvDayTitle = view.findViewById(R.id.tvDayTitle);
        tvDayCount = view.findViewById(R.id.tvDayCount);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        llWeekStrip = view.findViewById(R.id.llWeekStrip);

        view.findViewById(R.id.tvAvatar).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ProfileActivity.class)));

        view.findViewById(R.id.btnPrevMonth).setOnClickListener(v -> shiftMonth(-1));
        view.findViewById(R.id.btnNextMonth).setOnClickListener(v -> shiftMonth(1));

        RecyclerView rvDayTasks = view.findViewById(R.id.rvDayTasks);
        rvDayTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvDayTasks.setNestedScrollingEnabled(false);
        adapter = new CalendarTaskAdapter(task -> {
            Intent intent = new Intent(requireContext(), TaskDetailActivity.class);
            intent.putExtra(TaskDetailActivity.EXTRA_TASK_ID, task.getId());
            startActivity(intent);
        });
        rvDayTasks.setAdapter(adapter);

        view.findViewById(R.id.btnAddTask).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddEditTaskActivity.class);
            intent.putExtra(AddEditTaskActivity.EXTRA_DUE_DATE, selectedDate);
            startActivity(intent);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    /** Đổi tháng: chọn ngày 1 của tháng trước/sau. */
    private void shiftMonth(int delta) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(selectedDate);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.add(Calendar.MONTH, delta);
        selectedDate = FormatUtils.startOfDay(c.getTimeInMillis());
        loadData();
    }

    private void loadData() {
        List<Task> tasks = db.loadTasks();

        tvMonthYear.setText(FormatUtils.formatMonthYear(selectedDate));
        tvDayTitle.setText(FormatUtils.formatDayHeader(selectedDate));

        List<Task> dayTasks = new ArrayList<>();
        int dayDone = 0;
        for (Task t : tasks) {
            if (FormatUtils.startOfDay(t.getDueMillis()) == selectedDate) {
                dayTasks.add(t);
                if (t.isDone()) {
                    dayDone++;
                }
            }
        }
        Collections.sort(dayTasks, new Comparator<Task>() {
            @Override
            public int compare(Task a, Task b) {
                return Long.compare(a.getDueMillis(), b.getDueMillis());
            }
        });
        tvDayCount.setText(getString(R.string.calendar_tasks_count,
                dayTasks.size(), dayDone));
        adapter.setTasks(dayTasks);
        tvEmpty.setVisibility(dayTasks.isEmpty() ? View.VISIBLE : View.GONE);

        buildWeekStrip(tasks);
    }

    /** Dải 7 ngày của tuần chứa ngày đang chọn. */
    private void buildWeekStrip(List<Task> tasks) {
        llWeekStrip.removeAllViews();
        long weekStart = FormatUtils.startOfWeek(selectedDate);
        float density = getResources().getDisplayMetrics().density;

        for (int i = 0; i < 7; i++) {
            final long day = weekStart + i * 24L * 60 * 60 * 1000;
            boolean isSelected = day == selectedDate;
            boolean isToday = FormatUtils.isToday(day);
            boolean hasTasks = false;
            for (Task t : tasks) {
                if (FormatUtils.startOfDay(t.getDueMillis()) == day) {
                    hasTasks = true;
                    break;
                }
            }

            LinearLayout column = new LinearLayout(requireContext());
            column.setOrientation(LinearLayout.VERTICAL);
            column.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            column.setLayoutParams(colParams);
            column.setClickable(true);
            column.setOnClickListener(v -> {
                selectedDate = day;
                loadData();
            });

            TextView tvWeekday = new TextView(requireContext());
            tvWeekday.setText(FormatUtils.weekdayShort(day));
            tvWeekday.setTextSize(12);
            tvWeekday.setTextColor(ContextCompat.getColor(requireContext(),
                    R.color.text_secondary));
            tvWeekday.setGravity(android.view.Gravity.CENTER);
            column.addView(tvWeekday);

            TextView tvDay = new TextView(requireContext());
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(day);
            tvDay.setText(String.valueOf(c.get(Calendar.DAY_OF_MONTH)));
            tvDay.setTextSize(16);
            tvDay.setGravity(android.view.Gravity.CENTER);
            if (isSelected) {
                tvDay.setBackgroundResource(R.drawable.bg_circle_avatar);
                tvDay.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            } else if (isToday) {
                tvDay.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
                tvDay.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                tvDay.setTextColor(ContextCompat.getColor(requireContext(),
                        R.color.text_primary));
            }
            int size = (int) (36 * density);
            LinearLayout.LayoutParams dayParams = new LinearLayout.LayoutParams(size, size);
            dayParams.topMargin = (int) (4 * density);
            tvDay.setLayoutParams(dayParams);
            tvDay.setGravity(android.view.Gravity.CENTER);
            column.addView(tvDay);

            View dot = new View(requireContext());
            int dotSize = (int) (6 * density);
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dotSize, dotSize);
            dotParams.topMargin = (int) (4 * density);
            dot.setLayoutParams(dotParams);
            dot.setBackgroundResource(R.drawable.bg_dot_calendar);
            dot.setVisibility(hasTasks ? View.VISIBLE : View.INVISIBLE);
            column.addView(dot);

            llWeekStrip.addView(column);
        }
    }
}
