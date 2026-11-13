package com.example.taskflow.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.taskflow.R;
import com.example.taskflow.database.TaskFileDatabase;
import com.example.taskflow.model.Task;
import com.example.taskflow.utils.FormatUtils;

/**
 * Màn hình Chi tiết nhiệm vụ (thiết kế Midnight: thẻ header Navy).
 * Nhận id qua Intent, đọc lại nhiệm vụ từ file để hiển thị.
 * Cho phép đánh dấu hoàn thành/chưa hoàn thành, chỉnh sửa, xóa nhiệm vụ.
 */
public class TaskDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TASK_ID = "task_id";

    private long taskId;
    private Task task;
    private TaskFileDatabase db;

    private TextView tvPriorityChip;
    private TextView tvTitle;
    private TextView tvDescription;
    private TextView tvDueValue;
    private TextView tvPriorityValue;
    private TextView tvStatusValue;
    private TextView tvCategoryValue;
    private Button btnToggleStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        taskId = getIntent().getLongExtra(EXTRA_TASK_ID, -1);
        if (taskId == -1) {
            finish();
            return;
        }

        db = new TaskFileDatabase(this);

        tvPriorityChip = findViewById(R.id.tvPriorityChip);
        tvTitle = findViewById(R.id.tvTitle);
        tvDescription = findViewById(R.id.tvDescription);
        tvDueValue = findViewById(R.id.tvDueValue);
        tvPriorityValue = findViewById(R.id.tvPriorityValue);
        tvStatusValue = findViewById(R.id.tvStatusValue);
        tvCategoryValue = findViewById(R.id.tvCategoryValue);
        btnToggleStatus = findViewById(R.id.btnToggleStatus);
        Button btnDelete = findViewById(R.id.btnDelete);
        TextView tvEdit = findViewById(R.id.tvEdit);

        btnToggleStatus.setOnClickListener(v -> {
            db.toggleStatus(taskId);
            loadTask();
            Toast.makeText(this, task.isDone()
                    ? R.string.toast_marked_done : R.string.toast_marked_todo,
                    Toast.LENGTH_SHORT).show();
        });

        btnDelete.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(R.string.delete_confirm_title)
                .setMessage(getString(R.string.delete_confirm_message, task.getTitle()))
                .setPositiveButton(R.string.delete_yes, (dialog, which) -> {
                    db.deleteTask(taskId);
                    Toast.makeText(this, R.string.toast_task_deleted,
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton(R.string.cancel, null)
                .show());

        tvEdit.setOnClickListener(v -> {
            Intent intent = new Intent(TaskDetailActivity.this,
                    AddEditTaskActivity.class);
            intent.putExtra(AddEditTaskActivity.EXTRA_TASK_ID, taskId);
            startActivity(intent);
        });

        loadTask();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTask();
    }

    /** Đọc lại nhiệm vụ từ file và cập nhật toàn bộ giao diện. */
    private void loadTask() {
        task = db.getTaskById(taskId);
        if (task == null) {
            Toast.makeText(this, R.string.toast_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvTitle.setText(task.getTitle());
        tvDescription.setText(task.getDescription().isEmpty()
                ? getString(R.string.no_description)
                : task.getDescription());
        tvDueValue.setText(FormatUtils.formatDateTime(task.getDueMillis()));
        tvCategoryValue.setText(task.getCategory());

        int chipRes;
        int chipTextRes;
        int chipColorRes;
        String priorityText;
        switch (task.getPriority()) {
            case Task.PRIORITY_HIGH:
                chipRes = R.drawable.bg_chip_high;
                chipTextRes = R.string.priority_high_label;
                chipColorRes = R.color.priority_high;
                priorityText = getString(R.string.priority_high);
                break;
            case Task.PRIORITY_LOW:
                chipRes = R.drawable.bg_chip_low;
                chipTextRes = R.string.priority_low_label;
                chipColorRes = R.color.priority_low;
                priorityText = getString(R.string.priority_low);
                break;
            default:
                chipRes = R.drawable.bg_chip_medium;
                chipTextRes = R.string.priority_medium_label;
                chipColorRes = R.color.priority_medium;
                priorityText = getString(R.string.priority_medium);
                break;
        }
        tvPriorityChip.setBackgroundResource(chipRes);
        tvPriorityChip.setText(chipTextRes);
        tvPriorityChip.setTextColor(ContextCompat.getColor(this, chipColorRes));
        tvPriorityValue.setText(priorityText);

        if (task.isDone()) {
            tvStatusValue.setText(R.string.status_done);
            tvStatusValue.setTextColor(ContextCompat.getColor(this, R.color.success));
            btnToggleStatus.setText(R.string.mark_todo);
            btnToggleStatus.setBackgroundResource(R.drawable.bg_button_neutral);
            btnToggleStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        } else {
            tvStatusValue.setText(R.string.status_todo);
            tvStatusValue.setTextColor(ContextCompat.getColor(this, R.color.warning));
            btnToggleStatus.setText(R.string.mark_done);
            btnToggleStatus.setBackgroundResource(R.drawable.bg_button_mint);
            btnToggleStatus.setTextColor(ContextCompat.getColor(this, R.color.white));
        }
    }
}
