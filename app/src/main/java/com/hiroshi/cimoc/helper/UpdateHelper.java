package com.hiroshi.cimoc.helper;

import com.hiroshi.cimoc.manager.PreferenceManager;
import com.hiroshi.cimoc.model.Comic;
import com.hiroshi.cimoc.model.ComicDao;
import com.hiroshi.cimoc.model.DaoSession;
import com.hiroshi.cimoc.model.Source;
import com.hiroshi.cimoc.source.A2DM5;
import com.hiroshi.cimoc.source.A7Dmzj;
import com.hiroshi.cimoc.source.A1PuFei;
import com.hiroshi.cimoc.source.A8Dmzjv2;
import com.hiroshi.cimoc.source.A3HHSSEE;
import com.hiroshi.cimoc.source.A4IKanman;
import com.hiroshi.cimoc.source.A5MH57;
import com.hiroshi.cimoc.source.A0TuHaoMH;
import com.hiroshi.cimoc.source.A6U17;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hiroshi on 2017/1/18.
 */

public class UpdateHelper {

    // 1.5.0.0
    private static final int VERSION = 105000;

    private static final int START_VERSION = 105000;

    public static void update(PreferenceManager manager, final DaoSession session) {
        int version = manager.getInt(PreferenceManager.PREF_APP_VERSION, 0);
        if(version < START_VERSION && VERSION == START_VERSION){
            initSource(session);
            manager.putInt(PreferenceManager.PREF_APP_VERSION, VERSION);
        }else if(version < VERSION){
            //            switch (version) {
//                case 10404000:
//                updateTable(session);
//                    break;
//                default:
//                        break;
//            }
            manager.putInt(PreferenceManager.PREF_APP_VERSION, VERSION);
        }
//        if (version != VERSION) {
//            switch (version) {
//                case 0:
//                    initSource(session);
//                    break;
//                case 10404000:
//                case 10404001:
//                case 10404002:
//                case 10404003:
//                case 10405000:
//                    session.getSourceDao().insert(A8Dmzjv2.getDefaultSource());
//                case 10406000:
//                case 10407000:
//                case 10408000:
//                    deleteDownloadFromLocal(session);
//                case 10408001:
//                case 10408002:
//                case 10408003:
//                case 10408004:
//                case 10408005:
//                case 10408006:
//                case 10408007:
//                    // 删除 Chuiyao
//                    session.getDatabase().execSQL("DELETE FROM SOURCE WHERE \"TYPE\" = 9");
//                    // session.getSourceDao().insert(A1PuFei.getDefaultSource());
//            }
//            manager.putInt(PreferenceManager.PREF_APP_VERSION, VERSION);
//        }

    }

    private static void updateTable(final DaoSession session){
//        session.getComicDao().
    }

    /**
     * app: 1.4.8.0 -> 1.4.8.1
     * 删除本地漫画中 download 字段的值
     */
    private static void deleteDownloadFromLocal(final DaoSession session) {
        session.runInTx(new Runnable() {
            @Override
            public void run() {
                ComicDao dao = session.getComicDao();
                List<Comic> list = dao.queryBuilder().where(ComicDao.Properties.Local.eq(true)).list();
                if (!list.isEmpty()) {
                    for (Comic comic : list) {
                        comic.setDownload(null);
                    }
                    dao.updateInTx(list);
                }
            }
        });
    }

    /**
     * 初始化图源
     */
    private static void initSource(DaoSession session) {
        session.getSourceDao().deleteAll();
        List<Source> list = new ArrayList<>();
        list.add(A0TuHaoMH.getDefaultSource());
        list.add(A1PuFei.getDefaultSource());
        list.add(A2DM5.getDefaultSource());
        list.add(A3HHSSEE.getDefaultSource());
        list.add(A4IKanman.getDefaultSource());
        list.add(A5MH57.getDefaultSource());
        list.add(A7Dmzj.getDefaultSource());
        list.add(A8Dmzjv2.getDefaultSource());
        session.getSourceDao().insertOrReplaceInTx(list);
    }

}
