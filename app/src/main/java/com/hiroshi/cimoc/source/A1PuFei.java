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
import com.hiroshi.cimoc.utils.DecryptionUtils;
import com.hiroshi.cimoc.utils.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by FEILONG on 2017/12/21.
 */

public class A1PuFei extends MangaParser {

    public static final int TYPE = 1;
    public static final String DEFAULT_TITLE = "扑飞漫画";

    private Boolean isSearchType = false;

    private static final String searchUrl = "http://m.pufei.net/e/search/result/?searchid=";

    private OkHttpClient searchClient = new OkHttpClient.Builder()
            .connectTimeout(3000, TimeUnit.MILLISECONDS)
            .readTimeout(3000, TimeUnit.MILLISECONDS)
            .build();

    private Map<String, String> searchCache = new ConcurrentHashMap<>();

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    public A1PuFei(Source source) {
        init(source, new A1PuFei.Category());
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws UnsupportedEncodingException {
        isSearchType = true;
        String preUrl = searchUrl(keyword).concat("&page=").concat(String.valueOf(page));
        return new Request.Builder().url(preUrl).build();
    }

    private String searchUrl(String search) throws UnsupportedEncodingException {
        String result = searchCache.get(search);
        if (result != null) {
            return result;
        }
        String url = StringUtils.format("http://m.pufei.net/e/search/?searchget=1&tbname=mh&show=title,player,playadmin,bieming,pinyin,playadmin&tempid=4&keyboard=%s",
                URLEncoder.encode(search, "GB2312"));
        Request request = new Request.Builder().url(url).build();
        Response response = null;
        try {
            response = searchClient.newCall(request).execute();
            if (response.isSuccessful()) {
                HttpUrl httpUrl = response.request().url();
                String searchId = httpUrl.queryParameter("searchid");
                result = searchUrl + searchId;
                searchCache.put(search, result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return result;
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) {
        Node body = new Node(html);
        return new NodeIterator(body.list("ul#detail > li")) {
            @Override
            protected Comic parse(Node node) {
                Node hrefNode = node.firstElement("a");
                String cid = hrefNode.hrefWithSplit(1);
                String title = hrefNode.text("h3");
                String cover = hrefNode.attr("div > img", "data-src");
                String author = node.text("dl:eq(2) > dd");
                String update = node.text("dl:eq(4) > dd");
                String last = node.text("dl:eq(3) > dd");
                Comic comic = new Comic(TYPE, cid, title, cover, update, author);
                comic.setUpdateTo(last);
                return comic;
            }
        };
    }

    @Override
    public Request getCategoryRequest(String format, int page) {
        if (page > 1) {
            format = format.concat("/index_%d.html");
        }
//        包含manhua为更新和人气，责searchtype 为false
        if (format.indexOf("manhua") > -1) {
            isSearchType = false;
        } else {
            isSearchType = true;
        }
        String url = StringUtils.format(format, page);
        return new Request.Builder().url(url).build();
    }

    @Override
    public Request getInfoRequest(String cid) {
        String url = "http://m.pufei.net/manhua/".concat(cid);
        return new Request.Builder().url(url).build();
    }

    @Override
    public void parseInfo(String html, Comic comic) throws UnsupportedEncodingException {
        Node body = new Node(html);
        String title = body.text("div.main-bar > h1");
        String cover = body.src("div.book-detail > div.cont-list > div.thumb > img");
        String update = body.text("div.book-detail > div.cont-list > dl:eq(2) > dd");
        String author = body.text("div.book-detail > div.cont-list > dl:eq(3) > dd");
        String intro = body.text("#bookIntro");
        boolean status = isFinish(body.text("div.book-detail > div.cont-list > div.thumb > i"));
        comic.setInfo(title, cover, update, intro, author, status);

    }

    @Override
    public List<Chapter> parseChapter(String html) {
        List<Chapter> list = new LinkedList<>();
        for (Node node : new Node(html).list("#chapterList2 > ul > li > a")) {
            String title = node.attr("title");
            String path = node.hrefWithSplit(2);
            list.add(new Chapter(title, path));
        }
        return list;
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        String url = StringUtils.format("http://m.pufei.net/manhua/%s/%s.html", cid, path);
        return new Request.Builder().url(url).build();
    }

    @Override
    public List<ImageUrl> parseImages(String html) {
        List<ImageUrl> list = new LinkedList<>();
        String str = StringUtils.match("cp=\"(.*?)\"", html, 1);
        if (str != null) {
            try {
                str = DecryptionUtils.evalDecrypt(DecryptionUtils.base64Decrypt(str));
                String[] array = str.split(",");
                for (int i = 0; i != array.length; ++i) {
                    list.add(new ImageUrl(i + 1, "http://f.pufei.net/" + array[i], false));
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
        return new Node(html).text("div.book-detail > div.cont-list > dl:eq(2) > dd");
    }

    @Override
    public List<Comic> parseCategory(String html, int page) {
        List<Comic> list = new LinkedList<>();
        Node body = new Node(html);
        for (Node node : body.list("ul#detail > li")) {
            Node hrefNode = node.firstElement("a");
            String cid = hrefNode.hrefWithSplit(1);
            String title = hrefNode.text("h3");
            String cover = hrefNode.attr("div > img", "data-src");
            String author = node.text("dl:eq(2) > dd");
            String update = null;
            String last = null;
            if (isSearchType) {
                update = node.text("dl:eq(5) > dd");
                last = node.text("dl:eq(4) > dd");
            } else {
                update = node.text("dl:eq(4) > dd");
                last = node.text("dl:eq(3) > dd");
            }
            list.add(new Comic(TYPE, cid, title, cover, update, author,last));
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
            String url = "http://m.pufei.net/";
            if ("".equals(args[CATEGORY_SUBJECT])) {
                url = url.concat("manhua/");
                if ("update".equals(args[CATEGORY_ORDER])) {
                    url = url.concat("update.html");
                } else if ("paihang".equals(args[CATEGORY_ORDER])) {
                    url = url.concat("paihang.html");
                }
            } else {
                url = url.concat(args[CATEGORY_SUBJECT]);
            }
            return url;
        }

        @Override
        protected List<Pair<String, String>> getSubject() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("全部", ""));
            list.add(Pair.create("少年热血", "shaonianrexue"));
            list.add(Pair.create("武侠格斗", "wuxiagedou"));
            list.add(Pair.create("科幻魔幻", "kehuan"));
            list.add(Pair.create("竞技体育", "jingjitiyu"));
            list.add(Pair.create("爆笑喜剧", "gaoxiaoxiju"));
            list.add(Pair.create("侦探推理", "zhentantuili"));
            list.add(Pair.create("恐怖灵异", "kongbulingyi"));
            list.add(Pair.create("少女爱情", "shaonvaiqing"));
            return list;
        }

        @Override
        protected boolean hasOrder() {
            return true;
        }

        @Override
        protected List<Pair<String, String>> getOrder() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("最近更新", "update"));
            list.add(Pair.create("漫画排行", "paihang"));
            return list;
        }

    }

    @Override
    public Headers getHeader() {
        return Headers.of("Referer", "http://m.pufei.net");
    }

}
