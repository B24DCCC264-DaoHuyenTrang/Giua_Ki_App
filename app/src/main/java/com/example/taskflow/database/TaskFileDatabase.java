package com.example.taskflow.database;

import android.content.Context;

import com.example.taskflow.model.Task;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Lớp lưu trữ dữ liệu công việc DẠNG FILE (theo yêu cầu của đề bài:
 * dùng SharedPreferences, File hoặc SQLite — app này chọn lưu file).
 *
 * - File: tasks.json trong bộ nhớ trong của app (context.getFilesDir()).
 * - Nội dung file là một JSONArray, mỗi phần tử là một Task (JSONObject).
 * - Toàn bộ dữ liệu công việc và trạng thái được đọc/ghi qua lớp này.
 * - Đóng/mở lại ứng dụng, dữ liệu vẫn giữ nguyên vì file nằm trên đĩa.
 *
 * Toàn bộ mã nguồn lưu trữ nằm gọn trong thư mục database/ này.
 */
public class TaskFileDatabase {

    private static final String FILE_NAME = "tasks.json";

    private final File file;

    public TaskFileDatabase(Context context) {
        file = new File(context.getFilesDir(), FILE_NAME);
    }

    /** File dữ liệu (dùng cho chức năng Sao lưu dữ liệu). */
    public File getFile() {
        return file;
    }

    /** Đọc toàn bộ công việc từ file. Chưa có file thì tạo dữ liệu mẫu. */
    public synchronized List<Task> loadTasks() {
        if (!file.exists()) {
            List<Task> seed = seedTasks();
            saveTasks(seed);
            return seed;
        }
        List<Task> tasks = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
             BufferedReader br = new BufferedReader(isr)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            if (sb.length() == 0) {
                return tasks;
            }
            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                tasks.add(Task.fromJson(arr.getJSONObject(i)));
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return tasks;
    }

    /** Ghi toàn bộ danh sách công việc ra file (ghi đè). */
    public synchronized boolean saveTasks(List<Task> tasks) {
        try {
            JSONArray arr = new JSONArray();
            for (Task task : tasks) {
                arr.put(task.toJson());
            }
            try (FileOutputStream fos = new FileOutputStream(file);
                 OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
                 BufferedWriter bw = new BufferedWriter(osw)) {
                bw.write(arr.toString());
            }
            return true;
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Thêm công việc mới (tự gán id tăng dần), trả về Task đã lưu. */
    public synchronized Task addTask(Task task) {
        List<Task> tasks = loadTasks();
        long maxId = 0;
        for (Task t : tasks) {
            maxId = Math.max(maxId, t.getId());
        }
        task.setId(maxId + 1);
        task.setCreatedAt(System.currentTimeMillis());
        tasks.add(task);
        saveTasks(tasks);
        return task;
    }

    /** Cập nhật công việc đã có. Trả về true nếu cập nhật thành công. */
    public synchronized boolean updateTask(Task task) {
        List<Task> tasks = loadTasks();
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getId() == task.getId()) {
                tasks.set(i, task);
                return saveTasks(tasks);
            }
        }
        return false;
    }

    /** Xóa công việc theo id. Trả về true nếu xóa thành công. */
    public synchronized boolean deleteTask(long id) {
        List<Task> tasks = loadTasks();
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getId() == id) {
                tasks.remove(i);
                return saveTasks(tasks);
            }
        }
        return false;
    }

    /** Lấy một công việc theo id (trả về null nếu không tìm thấy). */
    public synchronized Task getTaskById(long id) {
        for (Task task : loadTasks()) {
            if (task.getId() == id) {
                return task;
            }
        }
        return null;
    }

    /** Đổi trạng thái công việc (Chưa thực hiện <-> Đã hoàn thành). */
    public synchronized boolean toggleStatus(long id) {
        Task task = getTaskById(id);
        if (task == null) {
            return false;
        }
        task.setStatus(task.isDone() ? Task.STATUS_TODO : Task.STATUS_DONE);
        return updateTask(task);
    }

    /** Tổng số công việc. */
    public synchronized int countTotal() {
        return loadTasks().size();
    }

    /** Số công việc đã hoàn thành. */
    public synchronized int countDone() {
        int count = 0;
        for (Task task : loadTasks()) {
            if (task.isDone()) {
                count++;
            }
        }
        return count;
    }

    /** Số công việc chưa hoàn thành. */
    public synchronized int countTodo() {
        return countTotal() - countDone();
    }

    // ==================== Dữ liệu mẫu ====================

    /** 6 công việc mẫu, lấy theo ngày hiện tại (4 đã hoàn thành, 2 chưa). */
    private List<Task> seedTasks() {
        List<Task> tasks = new ArrayList<>();

        Task t1 = new Task("Hoàn thành báo cáo môn học",
                "Tổng hợp nội dung, hoàn thiện biểu đồ và kiểm tra lỗi.",
                at(0, 16, 0), Task.PRIORITY_HIGH, Task.STATUS_TODO);
        t1.setId(1);
        tasks.add(t1);

        Task t2 = new Task("Ôn tập cấu trúc dữ liệu",
                "Ôn lại cây, đồ thị và thuật toán sắp xếp.",
                at(0, 20, 0), Task.PRIORITY_MEDIUM, Task.STATUS_TODO);
        t2.setId(2);
        tasks.add(t2);

        Task t3 = new Task("Đọc tài liệu Android",
                "Đọc chương về RecyclerView và Fragment.",
                at(1, 9, 0), Task.PRIORITY_LOW, Task.STATUS_DONE);
        t3.setId(3);
        tasks.add(t3);

        Task t4 = new Task("Chuẩn bị slide thuyết trình",
                "Làm slide cho buổi thuyết trình nhóm.",
                at(2, 18, 0), Task.PRIORITY_HIGH, Task.STATUS_DONE);
        t4.setId(4);
        tasks.add(t4);

        Task t5 = new Task("Nộp bài tập lập trình",
                "Nộp bài tập Java trên hệ thống.",
                at(-1, 23, 0), Task.PRIORITY_MEDIUM, Task.STATUS_DONE);
        t5.setId(5);
        tasks.add(t5);

        Task t6 = new Task("Ôn tập tiếng Anh",
                "Ôn từ vựng và ngữ pháp Unit 5.",
                at(-1, 8, 0), Task.PRIORITY_LOW, Task.STATUS_DONE);
        t6.setId(6);
        tasks.add(t6);

        return tasks;
    }

    /** Epoch millis của (ngày hiện tại + dayOffset) lúc hour:minute. */
    private long at(int dayOffset, int hour, int minute) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, dayOffset);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}
