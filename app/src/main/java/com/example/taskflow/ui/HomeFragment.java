package com.example.taskflow.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.taskflow.R;
import com.example.taskflow.adapter.TaskAdapter;
import com.example.taskflow.database.TaskFileDatabase;
import com.example.taskflow.model.Task;
import com.example.taskflow.utils.FormatUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Tab 1 — Tổng quan (Home dashboard): lời chào theo buổi, thẻ hero Navy
 * "Tập trung vào điều quan trọng.", 3 ô thống kê (Hoàn tất / Đang làm / Quá hạn)
 * và lịch trình hôm nay.
 */
public class HomeFragment extends Fragment {

    private TaskFileDatabase db;
    private TaskAdapter adapter;
    private TextView tvGreeting;
    private TextView tvHeroDate;
    private TextView tvHeroFocus;
    private TextView tvHeroLeft;
    private TextView tvDoneCount;
    private TextView tvDoingCount;
    private TextView tvOverdueCount;
    private TextView tvEmptyToday;
    private Task focusTask;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = new TaskFileDatabase(requireContext());

        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvHeroDate = view.findViewById(R.id.tvHeroDate);
        tvHeroFocus = view.findViewById(R.id.tvHeroFocus);
        tvHeroLeft = view.findViewById(R.id.tvHeroLeft);
        tvDoneCount = view.findViewById(R.id.tvDoneCount);
        tvDoingCount = view.findViewById(R.id.tvDoingCount);
        tvOverdueCount = view.findViewById(R.id.tvOverdueCount);
        tvEmptyToday = view.findViewById(R.id.tvEmptyToday);

        view.findViewById(R.id.tvAvatar).setOnClickListener(v -> openProfile());

        view.findViewById(R.id.btnHeroGo).setOnClickListener(v -> {
            if (focusTask != null) {
                openDetail(focusTask);
            } else {
                startActivity(new Intent(requireContext(), AddEditTaskActivity.class));
            }
        });

        RecyclerView rvSchedule = view.findViewById(R.id.rvScheduleToday);
        rvSchedule.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSchedule.setNestedScrollingEnabled(false);
        adapter = new TaskAdapter(new TaskAdapter.OnTaskActionListener() {
            @Override
            public void onTaskClick(Task task) {
                openDetail(task);
            }

            @Override
            public void onTaskStatusToggle(Task task) {
                db.toggleStatus(task.getId());
                loadData();
            }
        });
        rvSchedule.setAdapter(adapter);

        view.findViewById(R.id.tvViewCalendar).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToCalendarTab();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        SharedPreferences prefs = requireContext()
                .getSharedPreferences("profile", android.content.Context.MODE_PRIVATE);
        String name = prefs.getString("name", "Khánh");

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int greetingRes;
        if (hour >= 5 && hour < 12) {
            greetingRes = R.string.greeting_morning;
        } else if (hour < 18) {
            greetingRes = R.string.greeting_afternoon;
        } else {
            greetingRes = R.string.greeting_evening;
        }
        tvGreeting.setText(getString(greetingRes, name));

        tvHeroDate.setText(FormatUtils.formatDayCaps(System.currentTimeMillis()));

        List<Task> tasks = db.loadTasks();
        long now = System.currentTimeMillis();
        int done = 0;
        int overdue = 0;
        List<Task> todayTodo = new ArrayList<>();
        List<Task> todayAll = new ArrayList<>();
        for (Task t : tasks) {
            if (t.isDone()) {
                done++;
            } else {
                if (t.getDueMillis() < now) {
                    overdue++;
                }
                if (FormatUtils.isToday(t.getDueMillis())) {
                    todayTodo.add(t);
                }
            }
            if (FormatUtils.isToday(t.getDueMillis())) {
                todayAll.add(t);
            }
        }
        int doing = tasks.size() - done;

        tvDoneCount.setText(String.valueOf(done));
        tvDoingCount.setText(String.valueOf(doing));
        tvOverdueCount.setText(String.valueOf(overdue));

        // Nhiệm vụ trọng tâm: ưu tiên cao nhất, hạn gần nhất trong hôm nay.
        Collections.sort(todayTodo, new Comparator<Task>() {
            @Override
            public int compare(Task a, Task b) {
                if (a.getPriority() != b.getPriority()) {
                    return b.getPriority() - a.getPriority();
                }
                return Long.compare(a.getDueMillis(), b.getDueMillis());
            }
        });
        focusTask = todayTodo.isEmpty() ? null : todayTodo.get(0);
        if (focusTask != null) {
            tvHeroFocus.setText(getString(R.string.hero_focus_next, focusTask.getTitle()));
            tvHeroLeft.setText(getString(R.string.hero_tasks_left, todayTodo.size()));
            tvHeroLeft.setVisibility(View.VISIBLE);
        } else {
            tvHeroFocus.setText(R.string.hero_all_done);
            tvHeroLeft.setVisibility(View.GONE);
        }

        // Lịch trình hôm nay: tất cả việc hết hạn hôm nay, theo giờ.
        Collections.sort(todayAll, new Comparator<Task>() {
            @Override
            public int compare(Task a, Task b) {
                return Long.compare(a.getDueMillis(), b.getDueMillis());
            }
        });
        adapter.setTasks(todayAll);
        tvEmptyToday.setVisibility(todayAll.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openDetail(Task task) {
        Intent intent = new Intent(requireContext(), TaskDetailActivity.class);
        intent.putExtra(TaskDetailActivity.EXTRA_TASK_ID, task.getId());
        startActivity(intent);
    }

    private void openProfile() {
        startActivity(new Intent(requireContext(), ProfileActivity.class));
    }
}
