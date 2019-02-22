package com.hiroshi.cimoc.source;

import android.util.Pair;

import com.hiroshi.cimoc.model.Chapter;
import com.hiroshi.cimoc.model.Comic;
import com.hiroshi.cimoc.model.ImageUrl;
import com.hiroshi.cimoc.model.Source;
import com.hiroshi.cimoc.parser.MangaCategory;
import com.hiroshi.cimoc.parser.MangaParser;
import com.hiroshi.cimoc.parser.NodeIterator;
import com.hiroshi.cimoc.parser.SearchIterator;
import com.hiroshi.cimoc.soup.Node;
import com.hiroshi.cimoc.utils.JsUtil;
import com.hiroshi.cimoc.utils.StringUtils;
import com.hiroshi.cimoc.utils.TimeUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import okhttp3.Headers;
import okhttp3.Request;

public class A0TuHaoMH extends MangaParser {

    public static final int TYPE = 0;

    public static final String DEFAULT_TITLE = "土豪漫画";

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    public A0TuHaoMH(Source source) {
        init(source, new Category());
    }


    @Override
    public Request getCategoryRequest(String format, int page) {
        String url = StringUtils.format(format, page);
        return new Request.Builder().url(url).build();
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws UnsupportedEncodingException {
        String url = "https://www.tohomh123.com/action/Search?keyword=".concat(keyword).concat("&").concat("page=").concat(String.valueOf(page));
        return new Request.Builder().url(url).get().addHeader("Referer", "https://www.tohomh123.com").build();
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) {
        Node body = new Node(html);
        return new NodeIterator(body.list("ul.mh-list > li > div.mh-item")) {
            @Override
            protected Comic parse(Node node) {
                return importComic(node);
            }
        };
    }

    @Override
    public Request getInfoRequest(String cid) {
        String url = "https://www.tohomh123.com/".concat(cid);
        return new Request.Builder().url(url).build();
    }

    @Override
    public void parseInfo(String html, Comic comic) throws UnsupportedEncodingException {
        Node body = new Node(html);
        String title = body.textWithSplit("div.banner_detail_form > div.info > h1", " ", 0);
        String cover = body.src("div.banner_detail_form > div.cover > img");
        String update = body.textWithSubstring("div.banner_detail_form > div.info > p.tip > span:eq(2)", 5);
        String author = body.textWithSubstring("div.banner_detail_form > div.info > p.subtitle", 3);
        String intro = body.text("div.banner_detail_form > div.info > p.content");
        boolean status = isFinish(body.text("div.banner_detail_form > div.info > p.tip > span:eq(0)"));
        comic.setInfo(title, cover, update, intro, author, status);
    }

    @Override
    public List<Comic> parseCategory(String html, int page) {
        List<Comic> list = new ArrayList<>();
        Node body = new Node(html);
        for (Node node : body.list("ul.mh-list > li > div.mh-item")) {
            list.add(importComic(node));
        }
        return list;
    }

    private static Comic importComic(Node node) {
        String cid = node.hrefWithSplit("div > h2.title > a", 0);
        String title = node.text("div > h2.title > a");
        String cover = StringUtils.match("\\((.*?)\\)", node.attr("p.mh-cover", "style"), 1);
        String author = node.textWithSubstring("p.author", 3);
        String update = TimeUtils.timeAnalysis(node.text("p.zl"));
        String updateTo = node.textWithSubstring("p.chapter", 2);
        return new Comic(TYPE, cid, title, cover, update, author, updateTo);
    }

    @Override
    public List<Chapter> parseChapter(String html) {
        List<Chapter> list = new ArrayList<>();
        Node body = new Node(html);
        List<Node> nodes = body.list("#chapterlistload > ul").get(0).list("li");
        addChapterItem(list, nodes);
        return list;
    }

    private void addChapterItem(List<Chapter> chapters, List<Node> nodes) {
        for (Node node : nodes) {
            String title = node.text();
            String[] herfs = node.firstElement("a").href().split("/");
            String path = herfs[herfs.length - 1];
            chapters.add(new Chapter(title, path));
        }
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        String url = "https://www.tohomh123.com/".concat(cid).concat("/").concat(path);
        return new Request.Builder().url(url).build();
    }

    @Override
    public List<ImageUrl> parseImages(String html) {
        List<ImageUrl> list = new LinkedList<>();
//        String imgDomain = JsUtil.getScriptVala(html, "imgDomain");
        String did = JsUtil.getScriptVal(html, "did");
        String sid = JsUtil.getScriptVal(html, "sid");
        String pcount = JsUtil.getScriptVal(html, "pcount");
        String p1 = JsUtil.getScriptVal(html, "pl");
        String format = "https://www.tohomh123.com/action/play/read?did=%s&sid=%s&iid=%s";
        list.add(new ImageUrl(1, p1, false));
        for (int i = 0; i < Integer.valueOf(pcount); ++i) {
            int count = i + 1;
            String url = StringUtils.format(format, did, sid, count + 1);
            list.add(new ImageUrl(count, url, true));
        }
        return list;
    }

    @Override
    public Request getLazyRequest(String url) {
        return new Request.Builder().url(url)
                .addHeader("Referer", "https://www.tohomh123.com")
                .build();
    }

    @Override
    public String parseLazy(String html, String url) {
        try {
            JSONObject object = new JSONObject(html);
            Object isError = object.get("IsError");
            String code = object.getString("Code");
            if (isError != null && !Boolean.valueOf(isError.toString()) && !StringUtils.isEmpty(code)) {
                return code;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static class Category extends MangaCategory {

        @Override
        public boolean isComposite() {
            return true;
        }

        @Override
        public String getFormat(String... args) {
            String path = args[CATEGORY_SUBJECT].concat("-").concat(args[CATEGORY_AREA]).concat("-").concat("---").concat(args[CATEGORY_ORDER])
                    .concat("-").concat(args[CATEGORY_PROGRESS]).concat("-").concat("%d.html").trim();
            path = path.replaceAll("\\s+", "-");
            return StringUtils.format("https://www.tohomh123.com/f-1-%s", path);
        }

        @Override
        protected List<Pair<String, String>> getSubject() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("全部", ""));
            list.add(Pair.create("热血", "1"));
            list.add(Pair.create("恋爱", "2"));
            list.add(Pair.create("校园", "3"));
            list.add(Pair.create("百合", "4"));
            list.add(Pair.create("耽美", "5"));
            list.add(Pair.create("冒险", "6"));
            list.add(Pair.create("后宫", "7"));
            list.add(Pair.create("仙侠", "8"));
            list.add(Pair.create("武侠", "9"));
            list.add(Pair.create("悬疑", "10"));
            list.add(Pair.create("推理", "11"));
            list.add(Pair.create("搞笑", "12"));
            list.add(Pair.create("奇幻", "13"));
            list.add(Pair.create("恐怖", "14"));
            list.add(Pair.create("玄幻", "15"));
            list.add(Pair.create("古风", "16"));
            list.add(Pair.create("萌系", "17"));
            list.add(Pair.create("日常", "18"));
            list.add(Pair.create("治愈", "19"));
            list.add(Pair.create("烧脑", "20"));
            list.add(Pair.create("穿越", "21"));
            list.add(Pair.create("都市", "22"));
            list.add(Pair.create("腹黑", "23"));
            return list;
        }

        @Override
        protected boolean hasArea() {
            return true;
        }

        @Override
        protected List<Pair<String, String>> getArea() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("全部", ""));
            list.add(Pair.create("国漫", "国漫"));
            list.add(Pair.create("日本", "日本"));
            list.add(Pair.create("欧美", "欧美"));
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
            list.add(Pair.create("连载", "1"));
            list.add(Pair.create("完结", "0"));
            return list;
        }

        @Override
        protected boolean hasOrder() {
            return true;
        }

        @Override
        protected List<Pair<String, String>> getOrder() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("更新时间", "updatetime"));
            list.add(Pair.create("热门人气", "hits"));
            list.add(Pair.create("新品上架", "addtime"));
            return list;
        }

    }

    @Override
    public Headers getHeader() {
        return Headers.of("Referer", "https://www.tohomh123.com");
    }
}
