package com.example.taskflow.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.taskflow.R;
import com.example.taskflow.database.TaskFileDatabase;
import com.example.taskflow.model.Task;
import com.example.taskflow.utils.FormatUtils;

import java.util.List;

/**
 * Tab 4 — Phân tích (Insights): thẻ tỷ lệ hoàn thành (vòng tròn %),
 * tổng quan công việc (Tổng / Hoàn thành / Còn lại),
 * biểu đồ năng suất trong tuần và gợi ý cho bạn.
 */
public class StatisticsFragment extends Fragment {

    private TaskFileDatabase db;
    private ProgressBar ringProgress;
    private TextView tvRatePercent;
    private TextView tvRateSub;
    private TextView tvTotalBig;
    private TextView tvDoneBig;
    private TextView tvTodoBig;
    private LinearLayout llChart;
    private TextView tvTip;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_statistics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = new TaskFileDatabase(requireContext());

        ringProgress = view.findViewById(R.id.ringProgress);
        tvRatePercent = view.findViewById(R.id.tvRatePercent);
        tvRateSub = view.findViewById(R.id.tvRateSub);
        tvTotalBig = view.findViewById(R.id.tvTotalBig);
        tvDoneBig = view.findViewById(R.id.tvDoneBig);
        tvTodoBig = view.findViewById(R.id.tvTodoBig);
        llChart = view.findViewById(R.id.llChart);
        tvTip = view.findViewById(R.id.tvTip);

        view.findViewById(R.id.tvAvatar).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ProfileActivity.class)));
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        List<Task> all = db.loadTasks();
        int total = all.size();
        int done = 0;
        int overdue = 0;
        long now = System.currentTimeMillis();
        for (Task t : all) {
            if (t.isDone()) {
                done++;
            } else if (t.getDueMillis() < now) {
                overdue++;
            }
        }
        int todo = total - done;
        int percent = total == 0 ? 0 : done * 100 / total;

        ringProgress.setProgress(percent);
        tvRatePercent.setText(percent + "%");
        tvRateSub.setText(getString(R.string.completion_rate_sub, done, total));
        tvTotalBig.setText(String.valueOf(total));
        tvDoneBig.setText(String.valueOf(done));
        tvTodoBig.setText(String.valueOf(todo));

        if (total == 0) {
            tvTip.setText(R.string.tip_empty);
        } else if (overdue > 0) {
            tvTip.setText(getString(R.string.tip_overdue, overdue));
        } else if (percent >= 80) {
            tvTip.setText(R.string.tip_great);
        } else {
            tvTip.setText(getString(R.string.tip_keep_going, percent));
        }

        buildChart(all);
    }

    /** Biểu đồ 7 ngày gần nhất: số nhiệm vụ hết hạn mỗi ngày. */
    private void buildChart(List<Task> all) {
        llChart.removeAllViews();
        long todayStart = FormatUtils.startOfDay(System.currentTimeMillis());
        int[] counts = new int[7];
        int max = 0;
        for (int i = 0; i < 7; i++) {
            long dayStart = todayStart - (6 - i) * 24L * 60 * 60 * 1000;
            long dayEnd = dayStart + 24L * 60 * 60 * 1000;
            int count = 0;
            for (Task t : all) {
                if (t.getDueMillis() >= dayStart && t.getDueMillis() < dayEnd) {
                    count++;
                }
            }
            counts[i] = count;
            max = Math.max(max, count);
        }

        int maxBarDp = 110;
        int minBarDp = 10;
        float density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < 7; i++) {
            LinearLayout column = new LinearLayout(requireContext());
            column.setOrientation(LinearLayout.VERTICAL);
            column.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            column.setLayoutParams(colParams);

            int barHeightDp = max == 0 ? minBarDp
                    : Math.max(minBarDp, counts[i] * maxBarDp / max);
            View bar = new View(requireContext());
            bar.setBackgroundResource(i == 6
                    ? R.drawable.bg_bar_active : R.drawable.bg_bar);
            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
                    (int) (22 * density), (int) (barHeightDp * density));
            bar.setLayoutParams(barParams);
            column.addView(bar);

            TextView label = new TextView(requireContext());
            label.setText(FormatUtils.weekdayShort(
                    todayStart - (6 - i) * 24L * 60 * 60 * 1000));
            label.setTextSize(12);
            label.setTextColor(ContextCompat.getColor(requireContext(),
                    R.color.text_secondary));
            label.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            labelParams.topMargin = (int) (6 * density);
            label.setLayoutParams(labelParams);
            column.addView(label);

            llChart.addView(column);
        }
    }
}
