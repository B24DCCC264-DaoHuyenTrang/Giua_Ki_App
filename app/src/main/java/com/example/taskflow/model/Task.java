package com.example.taskflow.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Lớp Task biểu diễn một công việc cá nhân.
 * Khớp với một đối tượng JSON trong file tasks.json
 * (xem database/TaskFileDatabase).
 */
public class Task {

    // Mức độ ưu tiên
    public static final int PRIORITY_LOW = 0;
    public static final int PRIORITY_MEDIUM = 1;
    public static final int PRIORITY_HIGH = 2;

    // Trạng thái (theo đề bài: chỉ 2 trạng thái)
    public static final int STATUS_TODO = 0;
    public static final int STATUS_DONE = 1;

    private long id;              // Mã công việc (tự tăng)
    private String title;         // Tên công việc
    private String description;   // Mô tả
    private long dueMillis;       // Ngày + giờ hết hạn (epoch millis)
    private int priority;         // 0: Thấp, 1: Trung bình, 2: Cao
    private int status;           // 0: Chưa thực hiện, 1: Đã hoàn thành
    private long createdAt;       // Thời điểm tạo (epoch millis)
    private String category;      // Danh mục (Học tập, Công việc, Cá nhân, Khác)

    public Task() {
    }

    public Task(String title, String description, long dueMillis,
                int priority, int status) {
        this(title, description, dueMillis, priority, status, "Học tập");
    }

    public Task(String title, String description, long dueMillis,
                int priority, int status, String category) {
        this.title = title;
        this.description = description;
        this.dueMillis = dueMillis;
        this.priority = priority;
        this.status = status;
        this.category = category;
        this.createdAt = System.currentTimeMillis();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getDueMillis() {
        return dueMillis;
    }

    public void setDueMillis(long dueMillis) {
        this.dueMillis = dueMillis;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getCategory() {
        return category == null ? "Học tập" : category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isDone() {
        return status == STATUS_DONE;
    }

    /** Chuyển Task thành JSONObject để ghi ra file. */
    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("title", title);
        obj.put("description", description == null ? "" : description);
        obj.put("dueMillis", dueMillis);
        obj.put("priority", priority);
        obj.put("status", status);
        obj.put("createdAt", createdAt);
        obj.put("category", getCategory());
        return obj;
    }

    /** Đọc Task từ JSONObject trong file. */
    public static Task fromJson(JSONObject obj) throws JSONException {
        Task task = new Task();
        task.id = obj.optLong("id", 0);
        task.title = obj.optString("title", "");
        task.description = obj.optString("description", "");
        task.dueMillis = obj.optLong("dueMillis", 0);
        task.priority = obj.optInt("priority", PRIORITY_MEDIUM);
        task.status = obj.optInt("status", STATUS_TODO);
        task.createdAt = obj.optLong("createdAt", 0);
        task.category = obj.optString("category", "Học tập");
        return task;
    }
}
