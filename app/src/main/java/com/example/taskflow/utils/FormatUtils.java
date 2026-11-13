package com.example.taskflow.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Các hàm định dạng ngày/giờ dùng chung cho toàn app (tiếng Việt).
 */
public final class FormatUtils {

    private static final Locale VI = new Locale("vi", "VN");

    private FormatUtils() {
        // Không cho khởi tạo.
    }

    /** 13/11/2026 */
    public static String formatDate(long millis) {
        return new SimpleDateFormat("dd/MM/yyyy", VI).format(new Date(millis));
    }

    /** 16:00 */
    public static String formatTime(long millis) {
        return new SimpleDateFormat("HH:mm", VI).format(new Date(millis));
    }

    /** 13/11/2026 - 16:00 */
    public static String formatDateTime(long millis) {
        return new SimpleDateFormat("dd/MM/yyyy - HH:mm", VI).format(new Date(millis));
    }

    /** Thứ Sáu, 13 tháng 11 */
    public static String formatDayHeader(long millis) {
        return new SimpleDateFormat("EEEE, dd 'tháng' MM", VI).format(new Date(millis));
    }

    /** "Hôm nay - 16:00" / "Ngày mai - 09:00" / "13/11/2026 - 16:00" */
    public static String dueLabel(long millis) {
        if (isToday(millis)) {
            return "Hôm nay - " + formatTime(millis);
        }
        if (isTomorrow(millis)) {
            return "Ngày mai - " + formatTime(millis);
        }
        return formatDateTime(millis);
    }

    public static boolean isToday(long millis) {
        Calendar a = Calendar.getInstance();
        Calendar b = Calendar.getInstance();
        b.setTimeInMillis(millis);
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    public static boolean isTomorrow(long millis) {
        Calendar a = Calendar.getInstance();
        a.add(Calendar.DAY_OF_YEAR, 1);
        Calendar b = Calendar.getInstance();
        b.setTimeInMillis(millis);
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    /** Đầu ngày (00:00) của ngày chứa millis. */
    public static long startOfDay(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /** Thứ trong tuần viết tắt: T2, T3, T4, T5, T6, T7, CN. */
    public static String weekdayShort(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        switch (c.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY:
                return "T2";
            case Calendar.TUESDAY:
                return "T3";
            case Calendar.WEDNESDAY:
                return "T4";
            case Calendar.THURSDAY:
                return "T5";
            case Calendar.FRIDAY:
                return "T6";
            case Calendar.SATURDAY:
                return "T7";
            default:
                return "CN";
        }
    }

    /** Tháng 11, 2026 */
    public static String formatMonthYear(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        return "Tháng " + (c.get(Calendar.MONTH) + 1) + ", " + c.get(Calendar.YEAR);
    }

    /** Đầu tuần (thứ Hai 00:00) của tuần chứa millis. */
    public static long startOfWeek(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
        int daysSinceMonday = dayOfWeek == Calendar.SUNDAY ? 6 : dayOfWeek - Calendar.MONDAY;
        c.add(Calendar.DAY_OF_YEAR, -daysSinceMonday);
        return startOfDay(c.getTimeInMillis());
    }

    /** "THỨ SÁU · 13 THÁNG 11" — dùng cho thẻ hero ở màn Tổng quan. */
    public static String formatDayCaps(long millis) {
        return new SimpleDateFormat("EEEE · dd 'tháng' MM", VI)
                .format(new Date(millis)).toUpperCase(VI);
    }
}
