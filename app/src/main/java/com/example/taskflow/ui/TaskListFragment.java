package com.example.taskflow.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
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
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Tab 2 — Kho công việc: tìm kiếm theo tên/mô tả, lọc nhanh
 * (Tất cả / Đang làm / Hoàn tất), lọc theo mức độ ưu tiên,
 * sắp xếp theo hạn gần nhất và nhóm theo ngày (HÔM NAY / NGÀY MAI / ...).
 */
public class TaskListFragment extends Fragment {

    private static final int FILTER_ALL = 0;
    private static final int FILTER_DOING = 1;
    private static final int FILTER_DONE = 2;

    private TaskFileDatabase db;
    private TaskAdapter adapter;
    private List<Task> allTasks = new ArrayList<>();

    private EditText edtSearch;
    private Chip chipAll;
    private TextView tvEmpty;
    private int statusFilter = FILTER_ALL;
    private int priorityFilter = -1; // -1: tất cả, 0/1/2: Thấp/Trung bình/Cao

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_task_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = new TaskFileDatabase(requireContext());

        edtSearch = view.findViewById(R.id.edtSearch);
        chipAll = view.findViewById(R.id.chipAll);
        tvEmpty = view.findViewById(R.id.tvEmpty);

        view.findViewById(R.id.tvAvatar).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ProfileActivity.class)));

        RecyclerView rvTasks = view.findViewById(R.id.rvTasks);
        rvTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TaskAdapter(new TaskAdapter.OnTaskActionListener() {
            @Override
            public void onTaskClick(Task task) {
                Intent intent = new Intent(requireContext(), TaskDetailActivity.class);
                intent.putExtra(TaskDetailActivity.EXTRA_TASK_ID, task.getId());
                startActivity(intent);
            }

            @Override
            public void onTaskStatusToggle(Task task) {
                db.toggleStatus(task.getId());
                loadData();
            }
        });
        rvTasks.setAdapter(adapter);

        // Tìm kiếm theo tên hoặc mô tả.
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Lọc nhanh theo trạng thái.
        ChipGroup chipGroup = view.findViewById(R.id.chipGroupStatus);
        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipDoing) {
                statusFilter = FILTER_DOING;
            } else if (checkedId == R.id.chipDone) {
                statusFilter = FILTER_DONE;
            } else {
                statusFilter = FILTER_ALL;
            }
            applyFilter();
        });

        // Lọc theo mức độ ưu tiên.
        Spinner spinnerPriority = view.findViewById(R.id.spinnerPriority);
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.priority_filter,
                android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriority.setAdapter(spinnerAdapter);
        spinnerPriority.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                // 0: Tất cả, 1: Cao, 2: Trung bình, 3: Thấp
                switch (position) {
                    case 1:
                        priorityFilter = Task.PRIORITY_HIGH;
                        break;
                    case 2:
                        priorityFilter = Task.PRIORITY_MEDIUM;
                        break;
                    case 3:
                        priorityFilter = Task.PRIORITY_LOW;
                        break;
                    default:
                        priorityFilter = -1;
                        break;
                }
                applyFilter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        allTasks = db.loadTasks();
        chipAll.setText(getString(R.string.filter_all, allTasks.size()));
        applyFilter();
    }

    private void applyFilter() {
        String query = edtSearch.getText().toString().trim()
                .toLowerCase(Locale.getDefault());
        List<Task> filtered = new ArrayList<>();
        for (Task task : allTasks) {
            if (!query.isEmpty()
                    && !task.getTitle().toLowerCase(Locale.getDefault()).contains(query)
                    && !task.getDescription().toLowerCase(Locale.getDefault()).contains(query)) {
                continue;
            }
            if (statusFilter == FILTER_DOING && task.isDone()) {
                continue;
            }
            if (statusFilter == FILTER_DONE && !task.isDone()) {
                continue;
            }
            if (priorityFilter != -1 && task.getPriority() != priorityFilter) {
                continue;
            }
            filtered.add(task);
        }

        // Sắp xếp theo hạn gần nhất, rồi nhóm theo ngày.
        Collections.sort(filtered, new Comparator<Task>() {
            @Override
            public int compare(Task a, Task b) {
                return Long.compare(a.getDueMillis(), b.getDueMillis());
            }
        });
        List<TaskAdapter.Section> sections = new ArrayList<>();
        long currentDay = -1;
        List<Task> currentGroup = null;
        for (Task task : filtered) {
            long day = FormatUtils.startOfDay(task.getDueMillis());
            if (day != currentDay) {
                currentDay = day;
                currentGroup = new ArrayList<>();
                sections.add(new TaskAdapter.Section(sectionTitle(day), currentGroup));
            }
            currentGroup.add(task);
        }

        adapter.setSections(sections);
        tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** Tiêu đề nhóm: "HÔM NAY · 13/11" / "NGÀY MAI · 14/11" / "THỨ BẢY, 15 THÁNG 11". */
    private String sectionTitle(long dayStart) {
        if (FormatUtils.isToday(dayStart)) {
            return "HÔM NAY · " + FormatUtils.formatDate(dayStart).substring(0, 5);
        }
        if (FormatUtils.isTomorrow(dayStart)) {
            return "NGÀY MAI · " + FormatUtils.formatDate(dayStart).substring(0, 5);
        }
        return FormatUtils.formatDayHeader(dayStart).toUpperCase(Locale.forLanguageTag("vi-VN"));
    }
}
