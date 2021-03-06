package com.hiroshi.cimoc.source;

import android.util.Pair;

import com.hiroshi.cimoc.model.Chapter;
import com.hiroshi.cimoc.model.Comic;
import com.hiroshi.cimoc.model.ImageUrl;
import com.hiroshi.cimoc.model.Source;
import com.hiroshi.cimoc.parser.JsonIterator;
import com.hiroshi.cimoc.parser.MangaCategory;
import com.hiroshi.cimoc.parser.MangaParser;
import com.hiroshi.cimoc.parser.SearchIterator;
import com.hiroshi.cimoc.soup.Node;
import com.hiroshi.cimoc.utils.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import okhttp3.Headers;
import okhttp3.Request;

/**
 * Created by Hiroshi on 2016/7/8.
 */
public class A8Dmzjv2 extends MangaParser {

    public static final int TYPE = 8;
    public static final String DEFAULT_TITLE = "动漫之家v2";

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    public A8Dmzjv2(Source source) {
        init(source, new Category());
    }

    @Override
    public Request getSearchRequest(String keyword, int page) {
        if (page == 1) {
            String url = StringUtils.format("http://v2.api.dmzj.com/search/show/0/%s/%d.json", keyword, page - 1);
            return new Request.Builder().url(url).build();
        }
        return null;
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) {
        try {
            return new JsonIterator(new JSONArray(html)) {
                @Override
                protected Comic parse(JSONObject object) {
                    try {
                        String cid = object.getString("id");
                        String title = object.getString("title");
                        String cover = object.getString("cover");
                        String author = object.optString("authors");
                        String updateTo = object.optString("last_name");
                        return new Comic(TYPE, cid, title, cover, null, author,updateTo);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            };
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Request getInfoRequest(String cid) {
        String url = StringUtils.format("http://m.dmzj.com/info/%s.html", cid);
        return new Request.Builder().url(url).build();
    }

    @Override
    public void parseInfo(String html, Comic comic) {
        Node body = new Node(html);
        String intro = body.textWithSubstring("p.txtDesc", 3);
        String title = body.attr("#Cover > img", "title");
        String cover = body.src("#Cover > img");
        String author = body.text("div.Introduct_Sub > div.sub_r > p:eq(0) > a");
        String update = body.textWithSubstring("div.Introduct_Sub > div.sub_r > p:eq(3) > span.date", 0, 10);
        boolean status = isFinish(body.text("div.Introduct_Sub > div.sub_r > p:eq(2) > a:eq(3)"));
        comic.setInfo(title, cover, update, intro, author, status);
    }

    @Override
    public List<Chapter> parseChapter(String html) {
        String jsonString = StringUtils.match("initIntroData\\((.*?)\\);", html, 1);
        List<Chapter> list = new LinkedList<>();
        if (jsonString != null) {
            try {
                JSONArray array = new JSONArray(jsonString);
                for (int i = 0; i != array.length(); ++i) {
                    JSONArray data = array.getJSONObject(i).getJSONArray("data");
                    for (int j = 0; j != data.length(); ++j) {
                        JSONObject object = data.getJSONObject(j);
                        String title = object.getString("chapter_name");
                        String path = object.getString("id");
                        list.add(new Chapter(title, path));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        String url = StringUtils.format("http://m.dmzj.com/view/%s/%s.html", cid, path);
        return new Request.Builder().url(url).build();
    }

    @Override
    public List<ImageUrl> parseImages(String html) {
        List<ImageUrl> list = new LinkedList<>();
        String jsonString = StringUtils.match("\"page_url\":(\\[.*?\\]),", html, 1);
        if (jsonString != null) {
            try {
                JSONArray array = new JSONArray(jsonString);
                for (int i = 0; i != array.length(); ++i) {
                    list.add(new ImageUrl(i + 1, array.getString(i), false));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    @Override
    public Request getCheckRequest(String cid) {
        return getInfoRequest(cid);
    }

    @Override
    public String parseCheck(String html) {
        return new Node(html).textWithSubstring("div.Introduct_Sub > div.sub_r > p:eq(3) > span.date", 0, 10);
    }

    @Override
    public List<Comic> parseCategory(String html, int page) {
        List<Comic> list = new LinkedList<>();
        try {
            JSONArray array = new JSONArray(html);
            for (int i = 0; i != array.length(); ++i) {
                try {
                    JSONObject object = array.getJSONObject(i);
                    String cid = object.getString("id");
                    String title = object.getString("title");
                    String cover = object.getString("cover");
                    Long time = object.has("last_updatetime") ? object.getLong("last_updatetime") * 1000 : null;
                    String update = time == null ? null : new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(time));
                    String author = object.optString("authors");
                    String updateTo = object.optString("last_name");
                    list.add(new Comic(TYPE, cid, title, cover, update, author,updateTo));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

    private static class Category extends MangaCategory {

        @Override
        public boolean isComposite() {
            return true;
        }

        @Override
        public String getFormat(String... args) {
            String path = args[CATEGORY_SUBJECT].concat(" ").concat(args[CATEGORY_READER]).concat(" ").concat(args[CATEGORY_PROGRESS])
                    .concat(" ").concat(args[CATEGORY_AREA]).trim();
            if (path.isEmpty()) {
                path = String.valueOf(0);
            } else {
                path = path.replaceAll("\\s+", "-");
            }
            return StringUtils.format("http://v2.api.dmzj.com/classify/%s/%s/%%d.json", path, args[CATEGORY_ORDER]);
        }

        @Override
        public List<Pair<String, String>> getSubject() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("全部", ""));
            list.add(Pair.create("冒险", "4"));
            list.add(Pair.create("百合", "3243"));
            list.add(Pair.create("生活", "3242"));
            list.add(Pair.create("四格", "17"));
            list.add(Pair.create("伪娘", "3244"));
            list.add(Pair.create("悬疑", "3245"));
            list.add(Pair.create("后宫", "3249"));
            list.add(Pair.create("热血", "3248"));
            list.add(Pair.create("耽美", "3246"));
            list.add(Pair.create("其他", "16"));
            list.add(Pair.create("恐怖", "14"));
            list.add(Pair.create("科幻", "7"));
            list.add(Pair.create("格斗", "6"));
            list.add(Pair.create("欢乐向", "5"));
            list.add(Pair.create("爱情", "8"));
            list.add(Pair.create("侦探", "9"));
            list.add(Pair.create("校园", "13"));
            list.add(Pair.create("神鬼", "12"));
            list.add(Pair.create("魔法", "11"));
            list.add(Pair.create("竞技", "10"));
            list.add(Pair.create("历史", "3250"));
            list.add(Pair.create("战争", "3251"));
            list.add(Pair.create("魔幻", "5806"));
            list.add(Pair.create("扶她", "5345"));
            list.add(Pair.create("东方", "5077"));
            list.add(Pair.create("奇幻", "5848"));
            list.add(Pair.create("轻小说", "6316"));
            list.add(Pair.create("仙侠", "7900"));
            list.add(Pair.create("搞笑", "7568"));
            list.add(Pair.create("颜艺", "6437"));
            list.add(Pair.create("性转换", "4518"));
            list.add(Pair.create("高清单行", "4459"));
            list.add(Pair.create("治愈", "3254"));
            list.add(Pair.create("宅系", "3253"));
            list.add(Pair.create("萌系", "3252"));
            list.add(Pair.create("励志", "3255"));
            list.add(Pair.create("节操", "6219"));
            list.add(Pair.create("职场", "3328"));
            list.add(Pair.create("西方魔幻", "3365"));
            list.add(Pair.create("音乐舞蹈", "3326"));
            list.add(Pair.create("机战", "3325"));
            return list;
        }

        @Override
        public boolean hasArea() {
            return true;
        }

        @Override
        public List<Pair<String, String>> getArea() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("全部", ""));
            list.add(Pair.create("日本", "2304"));
            list.add(Pair.create("韩国", "2305"));
            list.add(Pair.create("欧美", "2306"));
            list.add(Pair.create("港台", "2307"));
            list.add(Pair.create("内地", "2308"));
            list.add(Pair.create("其他", "8453"));
            return list;
        }

        @Override
        public boolean hasReader() {
            return true;
        }

        @Override
        public List<Pair<String, String>> getReader() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("全部", ""));
            list.add(Pair.create("少年", "3262"));
            list.add(Pair.create("少女", "3263"));
            list.add(Pair.create("青年", "3264"));
            return list;
        }

        @Override
        public boolean hasProgress() {
            return true;
        }

        @Override
        public List<Pair<String, String>> getProgress() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("全部", ""));
            list.add(Pair.create("连载", "2309"));
            list.add(Pair.create("完结", "2310"));
            return list;
        }

        @Override
        public boolean hasOrder() {
            return true;
        }

        @Override
        public List<Pair<String, String>> getOrder() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("更新", "1"));
            list.add(Pair.create("人气", "0"));
            return list;
        }

    }

    @Override
    public Headers getHeader() {
        return Headers.of("Referer", "http://images.dmzj.com/");
    }

}
