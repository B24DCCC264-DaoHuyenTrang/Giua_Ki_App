package com.example.taskflow.adapter;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.taskflow.R;
import com.example.taskflow.model.Task;
import com.example.taskflow.utils.FormatUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter hiển thị danh sách công việc lên RecyclerView.
 * Mỗi dòng công việc: chấm màu mức độ ưu tiên, tên, ngày hết hạn, chip ưu tiên,
 * trạng thái và CheckBox để đổi trạng thái ngay trên danh sách.
 * Hỗ trợ cả 2 dạng dữ liệu:
 * - Danh sách phẳng (màn Tổng quan): setTasks(List&lt;Task&gt;).
 * - Danh sách nhóm theo ngày (màn Kho công việc): setSections(...) với
 *   tiêu đề nhóm là String ("HÔM NAY · 13/11", "NGÀY MAI · 14/11", ...).
 */
public class TaskAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_TASK = 1;

    public interface OnTaskActionListener {
        void onTaskClick(Task task);

        void onTaskStatusToggle(Task task);
    }

    /** Danh sách hỗn hợp: phần tử là String (tiêu đề nhóm) hoặc Task. */
    private List<Object> items = new ArrayList<>();
    private final OnTaskActionListener listener;

    public TaskAdapter(OnTaskActionListener listener) {
        this.listener = listener;
    }

    /** Danh sách phẳng, không có tiêu đề nhóm. */
    public void setTasks(List<Task> tasks) {
        items = new ArrayList<>();
        if (tasks != null) {
            items.addAll(tasks);
        }
        notifyDataSetChanged();
    }

    /** Danh sách nhóm theo ngày: mỗi nhóm là tiêu đề + các Task trong nhóm. */
    public void setSections(List<Section> sections) {
        items = new ArrayList<>();
        if (sections != null) {
            for (Section section : sections) {
                if (section.tasks.isEmpty()) {
                    continue;
                }
                items.add(section.title);
                items.addAll(section.tasks);
            }
        }
        notifyDataSetChanged();
    }

    /** Một nhóm công việc trong cùng một ngày. */
    public static class Section {
        public final String title;
        public final List<Task> tasks;

        public Section(String title, List<Task> tasks) {
            this.title = title;
            this.tasks = tasks;
        }
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof String ? TYPE_HEADER : TYPE_TASK;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_task_header, parent, false);
            return new HeaderViewHolder(view);
        }
        View view = inflater.inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).tvSectionTitle.setText((String) items.get(position));
            return;
        }
        Task task = (Task) items.get(position);
        TaskViewHolder taskHolder = (TaskViewHolder) holder;
        android.content.Context ctx = holder.itemView.getContext();

        taskHolder.tvTitle.setText(task.getTitle());
        taskHolder.tvDue.setText(FormatUtils.dueLabel(task.getDueMillis()));

        // Gạch ngang tên khi đã hoàn thành.
        taskHolder.tvTitle.setPaintFlags(task.isDone()
                ? taskHolder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                : taskHolder.tvTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);

        // Chấm màu + chip mức độ ưu tiên.
        int dotRes;
        int chipRes;
        int chipTextRes;
        int chipColorRes;
        switch (task.getPriority()) {
            case Task.PRIORITY_HIGH:
                dotRes = R.drawable.bg_dot_high;
                chipRes = R.drawable.bg_chip_high;
                chipTextRes = R.string.priority_high;
                chipColorRes = R.color.priority_high;
                break;
            case Task.PRIORITY_LOW:
                dotRes = R.drawable.bg_dot_low;
                chipRes = R.drawable.bg_chip_low;
                chipTextRes = R.string.priority_low;
                chipColorRes = R.color.priority_low;
                break;
            default:
                dotRes = R.drawable.bg_dot_medium;
                chipRes = R.drawable.bg_chip_medium;
                chipTextRes = R.string.priority_medium;
                chipColorRes = R.color.priority_medium;
                break;
        }
        taskHolder.viewDot.setBackgroundResource(dotRes);
        taskHolder.tvPriority.setBackgroundResource(chipRes);
        taskHolder.tvPriority.setText(chipTextRes);
        taskHolder.tvPriority.setTextColor(ContextCompat.getColor(ctx, chipColorRes));

        // Trạng thái.
        if (task.isDone()) {
            taskHolder.tvStatus.setText(R.string.status_done);
            taskHolder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.success));
        } else {
            taskHolder.tvStatus.setText(R.string.status_todo);
            taskHolder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.warning));
        }

        // CheckBox đổi trạng thái ngay trên danh sách (tránh gọi listener khi tái sử dụng view).
        taskHolder.cbDone.setOnCheckedChangeListener(null);
        taskHolder.cbDone.setChecked(task.isDone());
        taskHolder.cbDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked != task.isDone()) {
                listener.onTaskStatusToggle(task);
            }
        });

        holder.itemView.setOnClickListener(v -> listener.onTaskClick(task));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvSectionTitle;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSectionTitle = itemView.findViewById(R.id.tvSectionTitle);
        }
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        View viewDot;
        TextView tvTitle;
        TextView tvDue;
        TextView tvPriority;
        TextView tvStatus;
        CheckBox cbDone;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            viewDot = itemView.findViewById(R.id.viewDot);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDue = itemView.findViewById(R.id.tvDue);
            tvPriority = itemView.findViewById(R.id.tvPriority);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            cbDone = itemView.findViewById(R.id.cbDone);
        }
    }
}
