package com.example.taskflow.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
 * Adapter cho timeline trong màn Lịch: mỗi dòng là giờ + chấm màu ưu tiên
 * + thẻ nhiệm vụ (tên, chip ưu tiên, trạng thái).
 */
public class CalendarTaskAdapter extends RecyclerView.Adapter<CalendarTaskAdapter.ViewHolder> {

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    private List<Task> tasks = new ArrayList<>();
    private final OnTaskClickListener listener;

    public CalendarTaskAdapter(OnTaskClickListener listener) {
        this.listener = listener;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks == null ? new ArrayList<>() : tasks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Task task = tasks.get(position);
        android.content.Context ctx = holder.itemView.getContext();

        holder.tvTime.setText(FormatUtils.formatTime(task.getDueMillis()));
        holder.tvTitle.setText(task.getTitle());

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
        holder.viewDot.setBackgroundResource(dotRes);
        holder.tvPriority.setBackgroundResource(chipRes);
        holder.tvPriority.setText(chipTextRes);
        holder.tvPriority.setTextColor(ContextCompat.getColor(ctx, chipColorRes));

        if (task.isDone()) {
            holder.tvStatus.setText(R.string.status_done);
            holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.success));
        } else {
            holder.tvStatus.setText(R.string.status_todo);
            holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.warning));
        }

        holder.itemView.setOnClickListener(v -> listener.onTaskClick(task));
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime;
        View viewDot;
        TextView tvTitle;
        TextView tvPriority;
        TextView tvStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvTime);
            viewDot = itemView.findViewById(R.id.viewDot);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvPriority = itemView.findViewById(R.id.tvPriority);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
