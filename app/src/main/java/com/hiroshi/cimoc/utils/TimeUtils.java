package com.hiroshi.cimoc.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class TimeUtils {

    public static String timeAnalysis(String update){
        if (update != null) {
            Calendar calendar = Calendar.getInstance();
            if (update.contains("今天") || update.contains("分钟前")) {
                update = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
            } else if (update.contains("昨天")) {
                calendar.add(Calendar.DATE, -1);
                update = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
            } else if (update.contains("前天")) {
                calendar.add(Calendar.DATE, -2);
                update = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
            } else {
                String result = StringUtils.match("\\d+-\\d+-\\d+", update, 0);
                if (result == null) {
                    String[] rs = StringUtils.match("(\\d+)月(\\d+)号", update, 1, 2);
                    if (rs != null) {
                        result = calendar.get(Calendar.YEAR) + "-" + rs[0] + "-" + rs[1];
                    }
                }
                update = result;
            }
        }
        return update;
    }

}
