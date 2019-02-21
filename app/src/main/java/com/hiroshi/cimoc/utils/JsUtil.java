package com.hiroshi.cimoc.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsUtil {

    public static String getScriptVal(String html,String key){
        String val = null;
        Document doc = Jsoup.parse(html);
        Elements scripts = doc.select("script"); // Get the script part
        for(Element element : scripts){
            Pattern p = Pattern.compile("(?is)"+key+"=\"(.+?)\""); // Regex for the value of the key
            Matcher m = p.matcher(element.html()); // you have to use html here and NOT text! Text will drop the 'key' part
            while( m.find() ) {
                val = m.group(1);
            }
            if(!StringUtils.isEmpty(val))break;
        }
        return val;
    }

}
