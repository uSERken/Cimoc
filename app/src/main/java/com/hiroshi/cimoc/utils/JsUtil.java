package com.hiroshi.cimoc.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;

public class JsUtil {

    public static String getScriptVal(String html, String key) {
        Map<String, Object> map = new HashMap<>();
        Document doc = Jsoup.parse(html);
        Elements scripts = doc.select("script"); // Get the script part
        for (Element element : scripts) {
            if (StringUtils.isEmpty(element.data())) continue;
            String[] data = element.data().toString().split("var");
            /*取得单个JS变量*/
            for (String variable : data) {
                /*过滤variable为空的数据*/
                if (variable.contains("=") && variable.contains(key)) {
                    /*取到满足条件的JS变量*/
                    String[] kvp = variable.split("=");
                    /*取得JS变量存入map*/
                    String valKey = kvp[0].trim();
                    if (!map.containsKey(valKey)) {
                        String varl = kvp[1]
                                .trim()
                                .replace("'", "")
                                .replace("\"", "")
                                .replace(";", "")
                                .replace("\r", "")
                                .replace("\n", "");
                        map.put(valKey, varl);
                    }
                }
            }
        }
        Object object = map.get(key);
        return object == null ? null : object.toString();
    }

}
