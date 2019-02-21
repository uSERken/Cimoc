package com.hiroshi.cimoc.helper;

import com.hiroshi.cimoc.manager.PreferenceManager;
import com.hiroshi.cimoc.model.Comic;
import com.hiroshi.cimoc.model.ComicDao;
import com.hiroshi.cimoc.model.DaoSession;
import com.hiroshi.cimoc.model.Source;
import com.hiroshi.cimoc.source.DM5;
import com.hiroshi.cimoc.source.Dmzj;
import com.hiroshi.cimoc.source.Dmzjv2;
import com.hiroshi.cimoc.source.HHSSEE;
import com.hiroshi.cimoc.source.IKanman;
import com.hiroshi.cimoc.source.MH57;
import com.hiroshi.cimoc.source.PuFei;
import com.hiroshi.cimoc.source.U17;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hiroshi on 2017/1/18.
 */

public class UpdateHelper {

    // 1.05.00.000
    private static final int VERSION = 10500000;

    public static void update(PreferenceManager manager, final DaoSession session) {
        int version = manager.getInt(PreferenceManager.PREF_APP_VERSION, 0);
        System.out.println("上一版本: "+version);
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
//                    session.getSourceDao().insert(Dmzjv2.getDefaultSource());
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
//                    // session.getSourceDao().insert(PuFei.getDefaultSource());
//            }
//            manager.putInt(PreferenceManager.PREF_APP_VERSION, VERSION);
//        }
        if(version < VERSION){
            switch (version) {
                case 10404000:
                    updateTable(session);
                    break;
                    default:
                        break;
            }
            manager.putInt(PreferenceManager.PREF_APP_VERSION, VERSION);
        }
        updateSource(session);
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
    private static void updateSource(DaoSession session) {
        session.getSourceDao().deleteAll();
        List<Source> list = new ArrayList<>();
        list.add(IKanman.getDefaultSource());
        list.add(Dmzj.getDefaultSource());
        list.add(U17.getDefaultSource());
        list.add(DM5.getDefaultSource());
        list.add(HHSSEE.getDefaultSource());
        list.add(MH57.getDefaultSource());
        list.add(Dmzjv2.getDefaultSource());
        list.add(PuFei.getDefaultSource());
        session.getSourceDao().insertOrReplaceInTx(list);
    }

}
