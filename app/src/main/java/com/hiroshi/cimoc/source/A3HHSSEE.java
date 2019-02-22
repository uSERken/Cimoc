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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.Headers;
import okhttp3.Request;

/**
 * Created by Hiroshi on 2016/10/1.
 */
public class A3HHSSEE extends MangaParser {

    public static final int TYPE = 3;
    public static final String DEFAULT_TITLE = "汗汗漫画";
    private static final String IMAGE_PRE_URL = "http://104.237.58.244/dm";
    //    用于区分不同类型页面用于加载相应的解析
    private int type;

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    public A3HHSSEE(Source source) {
        init(source, new Category());
    }

    @Override
    public Request getCategoryRequest(String format, int page) {
        String url = StringUtils.format(format, page);
        if (url.contains("dfcomiclist_") || url.contains("top/") || url.contains("comicsearch")) {
            type = 1;
        } else {
            type = 2;
        }
        return new Request.Builder().url(url).build();
    }

    @Override
    public Request getSearchRequest(String keyword, int page) {
        if (page == 1) {
            String url = "http://bbssoo.com/comicsearch/s.aspx?s=".concat(keyword);
            return new Request.Builder().url(url).build();
        }
        return null;
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) {
        Node body = new Node(html);
        return new NodeIterator(body.list("div.main > ul.se-list > li")) {
            @Override
            protected Comic parse(Node node) {
                return comicAnalysis(node);
            }
        };
    }

    @Override
    public Request getInfoRequest(String cid) {
        String url = StringUtils.format("http://bbssoo.com/comic/%s", cid);
        return new Request.Builder().url(url).build();
    }

    @Override
    public void parseInfo(String html, Comic comic) {
        Node body = new Node(html);
        String mainElement = "div.main > div > div.pic ";
        String title = body.text(mainElement + "> div.con > h3");
        String cover = body.src(mainElement + "> img");
        String author = body.textWithSubstring(mainElement + "> div.con > p:eq(1)", 3);
        String update = body.textWithSubstring(mainElement + "> div.con > p:eq(5)", 5);
        String intro = body.textWithSubstring("div#detail_block > div.ilist > p", 0);
        boolean status = isFinish(body.text("div.main > div.con > p:eq(4)"));
        comic.setInfo(title, cover, update, intro, author, status);
    }

    @Override
    public List<Chapter> parseChapter(String html) {
        List<Chapter> list = new ArrayList<>();
        Node body = new Node(html);
        List<Node> listChapter = body.list("p#sort_div_p > a");
        for (Node node : listChapter) {
            String title = node.text();
            String path = node.href();
            if (path != null) {
                String[] ali = path.split("\\/");
                path = ali[ali.length - 1];
            }
            list.add(new Chapter(title.trim(), path));
        }
        return list;
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        String url = StringUtils.format("http://bbssoo.com/vols/%s", path);
        return new Request.Builder().url(url).build();
    }

    @Override
    public List<ImageUrl> parseImages(String html) {
        List<ImageUrl> list = new ArrayList<>();
        String imageListString = JsUtil.getScriptVal(html, "sFiles");
        String imageServer = JsUtil.getScriptVal(html, "sPath");
        if (Integer.valueOf(imageServer) < 10) {
            imageServer = "0" + imageServer;
        }
        String urlsString = unsuan1(imageListString);
        if (!StringUtils.isEmpty(urlsString)) {
            String[] urls = urlsString.split("\\|");
            for (int i = 0; i < urls.length; i++) {
                list.add(new ImageUrl(i + 1, IMAGE_PRE_URL + imageServer + urls[i], false));
            }
        }
        return list;
    }

    private static String unsuan1(String s) {
        String x = s.substring(s.length() - 1);
        int xi = "abcdefghijklmnopqrstuvwxyz".indexOf(x) + 1;
        String sk = s.substring(s.length() - xi - 12, s.length() - xi - 1);
        s = s.substring(0, s.length() - xi - 12);
        String k = sk.substring(0, sk.length() - 1);
        String f = sk.substring(sk.length() - 1);
        for (Integer i = 0; i < k.length(); i++) {
            String kk = k.substring(i, i + 1);
            s = s.replace(kk, i.toString());
        }
        String[] ss = s.split(f);
        Integer[] codes = new Integer[ss.length];
        for (int i = 0; i < ss.length; i++) {
            codes[i] = Integer.valueOf(ss[i]);
        }
        String urls = fromCharCode(codes);
        return urls;
    }

    private static String fromCharCode(Integer[] codePoints) {
        StringBuilder builder = new StringBuilder(codePoints.length);
        for (int codePoint : codePoints) {
            builder.append(Character.toChars(codePoint));
        }
        return builder.toString();
    }

//    @Override
//    public Request getLazyRequest(String url) {
//        return new Request.Builder().url(url).build();
//    }
//
//    @Override
//    public String parseLazy(String html, String url) {
//        Node body = new Node(html);
//        String server = body.attr("#hdDomain", "value");
//        if (server != null) {
//            server = server.split("\\|")[0];
//            String name = body.attr("#iBodyQ > img", "name");
//            String result = unsuan(name).substring(1);
//            return server.concat(result);
//        }
//        return null;
//    }

    @Override
    public Request getCheckRequest(String cid) {
        return getInfoRequest(cid);
    }

    @Override
    public String parseCheck(String html) {
        String update = new Node(html).textWithSubstring("#about_kit > ul > li:eq(4)", 3);
        if (update != null) {
            String[] args = update.split("\\D");
            update = StringUtils.format("%4d-%02d-%02d", Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]));
        }
        return update;
    }

    private String unsuan(String str) {
        int num = str.length() - str.charAt(str.length() - 1) + 'a';
        String code = str.substring(num - 13, num - 2);
        String cut = code.substring(code.length() - 1);
        str = str.substring(0, num - 13);
        code = code.substring(0, code.length() - 1);
        for (int i = 0; i < code.length(); i++) {
            str = str.replace(code.charAt(i), (char) ('0' + i));
        }
        StringBuilder builder = new StringBuilder();
        String[] array = str.split(cut);
        for (int i = 0; i != array.length; ++i) {
            builder.append((char) Integer.parseInt(array[i]));
        }
        return builder.toString();
    }

    @Override
    public List<Comic> parseCategory(String html, int page) {
        Node body = new Node(html);
        if (type == 1) {
            return listAnalysis(body);
        } else if (type == 2) {
            return subjectAnalysis(body);
        }
        return Collections.emptyList();
    }

    private List<Comic> subjectAnalysis(Node body) {
        List<Node> nodes = body.list("div.main > ul.se-list");
        if (nodes != null) return listAnalysis(nodes.get(0));
        return Collections.emptyList();
    }

    private List<Comic> listAnalysis(Node body) {
        List<Comic> list = new ArrayList<>();
        for (Node node : body.list("li")) {
            list.add(comicAnalysis(node));
        }
        return list;
    }

    private Comic comicAnalysis(Node node) {
        List<Node> aNodes = node.list("a");
        Node titleNode = aNodes.get(0);
        String cid = titleNode.href();
        if (cid != null) {
            String[] ali = cid.split("\\/");
            cid = ali[ali.length - 1];
        }
        String title = titleNode.firstElement("div").text("h3");
        String cover = titleNode.src("img");
        String author = null;
        String update = null;
        String updateTo = null;
        if (aNodes.size() > 1) {
            updateTo = aNodes.get(1).text("span.h");
            author = titleNode.firstElement("div").text("p:eq(1)");
            update = titleNode.firstElement("div").text("span");
        }
        if (updateTo != null) {
            updateTo = updateTo.substring(1, updateTo.length() - 1);
        }
        return new Comic(TYPE, cid, title, cover, update, author, updateTo);
    }

    private static class Category extends MangaCategory {
        @Override
        public String getFormat(String... args) {
            if (!"".equals(args[CATEGORY_SUBJECT])) {
                return StringUtils.format("http://bbssoo.com/lists/%s/%%d", args[CATEGORY_SUBJECT]);
            } else if (!"".equals(args[CATEGORY_PROGRESS])) {
                return StringUtils.format("http://bbssoo.com/lianwan/%s/%%d", args[CATEGORY_PROGRESS]);
            } else if (!"".equals(args[CATEGORY_ORDER])) {
                return StringUtils.format("http://bbssoo.com/top/%s-%%d.htm", args[CATEGORY_ORDER]);
            } else if (!"".equals(args[CATEGORY_READER])) {
                return StringUtils.format("http://bbssoo.com/duzhequn/%s/%%d", args[CATEGORY_PROGRESS]);
            } else {
                return "http://bbssoo.com/dfcomiclist_%d.htm";
            }
        }

        @Override
        protected List<Pair<String, String>> getSubject() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("全部", ""));
            list.add(Pair.create("萌系", "1"));
            list.add(Pair.create("搞笑", "2"));
            list.add(Pair.create("格斗", "3"));
            list.add(Pair.create("科幻", "4"));
            list.add(Pair.create("剧情", "5"));
            list.add(Pair.create("侦探", "6"));
            list.add(Pair.create("竞技", "7"));
            list.add(Pair.create("魔法", "8"));
            list.add(Pair.create("神鬼", "9"));
            list.add(Pair.create("校园", "10"));
            list.add(Pair.create("惊栗", "11"));
            list.add(Pair.create("厨艺", "12"));
            list.add(Pair.create("伪娘", "13"));
            list.add(Pair.create("冒险", "15"));
            list.add(Pair.create("小说", "19"));
            list.add(Pair.create("耽美", "21"));
            list.add(Pair.create("经典", "22"));
            list.add(Pair.create("亲情", "25"));
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
            list.add(Pair.create("完结", "2"));
            return list;
        }

        @Override
        protected boolean hasReader() {
            return true;
        }

        protected List<Pair<String, String>> getReader() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("全部", ""));
            list.add(Pair.create("少年最爱", "1"));
            list.add(Pair.create("少女漫画", "2"));
            list.add(Pair.create("青年漫画", "3"));
            list.add(Pair.create("耽美漫画", "4"));
            return list;
        }

        @Override
        protected boolean hasOrder() {
            return true;
        }

        @Override
        protected List<Pair<String, String>> getOrder() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("全部", ""));
            list.add(Pair.create("今日最热", "a"));
            list.add(Pair.create("最多人看", "b"));
            list.add(Pair.create("最受好评", "c"));
            return list;
        }

    }

    /***
     * 防盗链
     * @return
     */
    @Override
    public Headers getHeader() {
        return Headers.of("Referer", "http://bbssoo.com/");
    }

}
