package com.hiroshi.cimoc.manager;

import android.util.SparseArray;

import com.hiroshi.cimoc.component.AppGetter;
import com.hiroshi.cimoc.model.Source;
import com.hiroshi.cimoc.model.SourceDao;
import com.hiroshi.cimoc.model.SourceDao.Properties;
import com.hiroshi.cimoc.parser.Parser;
import com.hiroshi.cimoc.source.DM5;
import com.hiroshi.cimoc.source.Dmzj;
import com.hiroshi.cimoc.source.Dmzjv2;
import com.hiroshi.cimoc.source.HHSSEE;
import com.hiroshi.cimoc.source.IKanman;
import com.hiroshi.cimoc.source.ZLocality;
import com.hiroshi.cimoc.source.MH57;
import com.hiroshi.cimoc.source.ZNull;
import com.hiroshi.cimoc.source.PuFei;
import com.hiroshi.cimoc.source.U17;

import java.util.List;

import okhttp3.Headers;
import rx.Observable;

/**
 * Created by Hiroshi on 2016/8/11.
 */
public class SourceManager {

    private static SourceManager mInstance;

    private SourceDao mSourceDao;
    private SparseArray<Parser> mParserArray = new SparseArray<>();

    private SourceManager(AppGetter getter) {
        mSourceDao = getter.getAppInstance().getDaoSession().getSourceDao();
    }

    public Observable<List<Source>> list() {
        return mSourceDao.queryBuilder()
                .orderAsc(Properties.Type)
                .rx()
                .list();
    }

    public Observable<List<Source>> listEnableInRx() {
        return mSourceDao.queryBuilder()
                .where(Properties.Enable.eq(true))
                .orderAsc(Properties.Type)
                .rx()
                .list();
    }

    public List<Source> listEnable() {
        return mSourceDao.queryBuilder()
                .where(Properties.Enable.eq(true))
                .orderAsc(Properties.Type)
                .list();
    }

    public Source load(int type) {
        return mSourceDao.queryBuilder()
                .where(Properties.Type.eq(type))
                .unique();
    }

    public long insert(Source source) {
        return mSourceDao.insert(source);
    }

    public void update(Source source) {
        mSourceDao.update(source);
    }

    public Parser getParser(int type) {
        Parser parser = mParserArray.get(type);
        if (parser == null) {
            Source source = load(type);
            switch (type) {
                case IKanman.TYPE:
                    parser = new IKanman(source);
                    break;
                case Dmzj.TYPE:
                    parser = new Dmzj(source);
                    break;
                case U17.TYPE:
                    parser = new U17(source);
                    break;
                case DM5.TYPE:
                    parser = new DM5(source);
                    break;
                case HHSSEE.TYPE:
                    parser = new HHSSEE(source);
                    break;
                case MH57.TYPE:
                    parser = new MH57(source);
                    break;
                case Dmzjv2.TYPE:
                    parser = new Dmzjv2(source);
                    break;
                case ZLocality.TYPE:
                    parser = new ZLocality();
                    break;
                case PuFei.TYPE:
                    parser = new PuFei(source);
                    break;
                default:
                    parser = new ZNull();
                    break;
            }
            mParserArray.put(type, parser);
        }
        return parser;
    }

    public class TitleGetter {

        public String getTitle(int type) {
            return getParser(type).getTitle();
        }

    }

    public class HeaderGetter {

        public Headers getHeader(int type) {
            return getParser(type).getHeader();
        }

    }

    public static SourceManager getInstance(AppGetter getter) {
        if (mInstance == null) {
            synchronized (SourceManager.class) {
                if (mInstance == null) {
                    mInstance = new SourceManager(getter);
                }
            }
        }
        return mInstance;
    }

}
