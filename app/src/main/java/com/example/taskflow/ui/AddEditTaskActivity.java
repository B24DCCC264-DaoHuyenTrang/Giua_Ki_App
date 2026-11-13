package com.example.taskflow.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.taskflow.R;
import com.example.taskflow.database.TaskFileDatabase;
import com.example.taskflow.model.Task;
import com.example.taskflow.utils.FormatUtils;

import java.util.Calendar;

/**
 * Màn hình Tạo nhiệm vụ / Sửa nhiệm vụ (thiết kế Midnight: thẻ header Navy).
 * - Thêm mới: mở từ nút + hoặc từ màn Lịch (truyền EXTRA_DUE_DATE = ngày đã chọn).
 * - Chỉnh sửa: mở từ màn hình chi tiết, truyền id qua Intent.
 * Kiểm tra tên công việc không được bỏ trống (setError).
 */
public class AddEditTaskActivity extends AppCompatActivity {

    public static final String EXTRA_TASK_ID = "task_id";
    public static final String EXTRA_DUE_DATE = "due_date";

    private EditText edtTitle;
    private EditText edtDescription;
    private TextView tvDate;
    private TextView tvTime;
    private Spinner spinnerCategory;
    private Button btnPriorityLow;
    private Button btnPriorityMedium;
    private Button btnPriorityHigh;
    private Button btnStatusTodo;
    private Button btnStatusDone;
    private Button btnSave;

    private TaskFileDatabase db;
    private Task editingTask; // null khi thêm mới
    private final Calendar dueCalendar = Calendar.getInstance();
    private int selectedPriority = Task.PRIORITY_HIGH;
    private int selectedStatus = Task.STATUS_TODO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_task);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        TextView tvHeaderTitle = findViewById(R.id.tvHeaderTitle);

        db = new TaskFileDatabase(this);

        edtTitle = findViewById(R.id.edtTitle);
        edtDescription = findViewById(R.id.edtDescription);
        tvDate = findViewById(R.id.tvDate);
        tvTime = findViewById(R.id.tvTime);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnPriorityLow = findViewById(R.id.btnPriorityLow);
        btnPriorityMedium = findViewById(R.id.btnPriorityMedium);
        btnPriorityHigh = findViewById(R.id.btnPriorityHigh);
        btnStatusTodo = findViewById(R.id.btnStatusTodo);
        btnStatusDone = findViewById(R.id.btnStatusDone);
        btnSave = findViewById(R.id.btnSave);

        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(
                this, R.array.task_categories, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        long taskId = getIntent().getLongExtra(EXTRA_TASK_ID, -1);
        if (taskId != -1) {
            editingTask = db.getTaskById(taskId);
        }

        if (editingTask != null) {
            // Chế độ chỉnh sửa: điền sẵn dữ liệu cũ.
            tvHeaderTitle.setText(R.string.edit_task_title);
            edtTitle.setText(editingTask.getTitle());
            edtDescription.setText(editingTask.getDescription());
            dueCalendar.setTimeInMillis(editingTask.getDueMillis());
            selectedPriority = editingTask.getPriority();
            selectedStatus = editingTask.getStatus();
            setCategorySelection(editingTask.getCategory());
        } else {
            // Chế độ thêm mới: mặc định hôm nay 16:00, ưu tiên Cao, chưa thực hiện.
            tvHeaderTitle.setText(R.string.add_task_title);
            long dueDate = getIntent().getLongExtra(EXTRA_DUE_DATE, -1);
            if (dueDate != -1) {
                // Mở từ màn Lịch: giữ nguyên ngày đã chọn, giờ mặc định 16:00.
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(dueDate);
                dueCalendar.set(Calendar.YEAR, c.get(Calendar.YEAR));
                dueCalendar.set(Calendar.MONTH, c.get(Calendar.MONTH));
                dueCalendar.set(Calendar.DAY_OF_MONTH, c.get(Calendar.DAY_OF_MONTH));
            }
            dueCalendar.set(Calendar.HOUR_OF_DAY, 16);
            dueCalendar.set(Calendar.MINUTE, 0);
            dueCalendar.set(Calendar.SECOND, 0);
            dueCalendar.set(Calendar.MILLISECOND, 0);
        }

        updateDateTimeViews();
        updatePriorityButtons();
        updateStatusButtons();

        tvDate.setOnClickListener(v -> showDatePicker());
        tvTime.setOnClickListener(v -> showTimePicker());

        btnPriorityLow.setOnClickListener(v -> {
            selectedPriority = Task.PRIORITY_LOW;
            updatePriorityButtons();
        });
        btnPriorityMedium.setOnClickListener(v -> {
            selectedPriority = Task.PRIORITY_MEDIUM;
            updatePriorityButtons();
        });
        btnPriorityHigh.setOnClickListener(v -> {
            selectedPriority = Task.PRIORITY_HIGH;
            updatePriorityButtons();
        });
        btnStatusTodo.setOnClickListener(v -> {
            selectedStatus = Task.STATUS_TODO;
            updateStatusButtons();
        });
        btnStatusDone.setOnClickListener(v -> {
            selectedStatus = Task.STATUS_DONE;
            updateStatusButtons();
        });

        btnSave.setOnClickListener(v -> saveTask());
    }

    private void setCategorySelection(String category) {
        ArrayAdapter<CharSequence> adapter =
                (ArrayAdapter<CharSequence>) spinnerCategory.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).toString().equals(category)) {
                spinnerCategory.setSelection(i);
                return;
            }
        }
    }

    private void showDatePicker() {
        new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    dueCalendar.set(Calendar.YEAR, year);
                    dueCalendar.set(Calendar.MONTH, month);
                    dueCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateTimeViews();
                },
                dueCalendar.get(Calendar.YEAR),
                dueCalendar.get(Calendar.MONTH),
                dueCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    dueCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    dueCalendar.set(Calendar.MINUTE, minute);
                    updateDateTimeViews();
                },
                dueCalendar.get(Calendar.HOUR_OF_DAY),
                dueCalendar.get(Calendar.MINUTE),
                true).show();
    }

    private void updateDateTimeViews() {
        tvDate.setText(FormatUtils.formatDate(dueCalendar.getTimeInMillis()));
        tvTime.setText(FormatUtils.formatTime(dueCalendar.getTimeInMillis()));
    }

    private void updatePriorityButtons() {
        setToggleButton(btnPriorityLow, selectedPriority == Task.PRIORITY_LOW);
        setToggleButton(btnPriorityMedium, selectedPriority == Task.PRIORITY_MEDIUM);
        setToggleButton(btnPriorityHigh, selectedPriority == Task.PRIORITY_HIGH);
    }

    private void updateStatusButtons() {
        setToggleButton(btnStatusTodo, selectedStatus == Task.STATUS_TODO);
        setToggleButton(btnStatusDone, selectedStatus == Task.STATUS_DONE);
    }

    private void setToggleButton(Button button, boolean selected) {
        button.setBackgroundResource(selected
                ? R.drawable.bg_button_primary : R.drawable.bg_button_neutral);
        button.setTextColor(ContextCompat.getColor(this,
                selected ? R.color.white : R.color.text_secondary));
    }

    private void saveTask() {
        String title = edtTitle.getText().toString().trim();
        if (title.isEmpty()) {
            edtTitle.setError(getString(R.string.error_empty_title));
            edtTitle.requestFocus();
            return;
        }
        String description = edtDescription.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();

        if (editingTask != null) {
            editingTask.setTitle(title);
            editingTask.setDescription(description);
            editingTask.setDueMillis(dueCalendar.getTimeInMillis());
            editingTask.setPriority(selectedPriority);
            editingTask.setStatus(selectedStatus);
            editingTask.setCategory(category);
            db.updateTask(editingTask);
            Toast.makeText(this, R.string.toast_task_updated, Toast.LENGTH_SHORT).show();
        } else {
            Task task = new Task(title, description,
                    dueCalendar.getTimeInMillis(), selectedPriority, selectedStatus, category);
            db.addTask(task);
            Toast.makeText(this, R.string.toast_task_saved, Toast.LENGTH_SHORT).show();
        }
        setResult(RESULT_OK);
        finish();
    }
}
