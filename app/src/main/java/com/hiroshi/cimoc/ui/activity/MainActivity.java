package com.hiroshi.cimoc.ui.activity;

import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.hiroshi.cimoc.CimocApplication;
import com.hiroshi.cimoc.R;
import com.hiroshi.cimoc.presenter.BasePresenter;
import com.hiroshi.cimoc.presenter.MainPresenter;
import com.hiroshi.cimoc.utils.PreferenceMaster;

import butterknife.BindView;

/**
 * Created by Hiroshi on 2016/7/1.
 */
public class MainActivity extends BaseActivity {

    @BindView(R.id.main_drawer_layout) DrawerLayout mDrawerLayout;
    @BindView(R.id.main_navigation_view) NavigationView mNavigationView;
    @BindView(R.id.main_fragment_container) FrameLayout mFrameLayout;
    @BindView(R.id.main_progress_bar) ProgressBar mProgressBar;

    private MainPresenter mPresenter;
    private long mExitTime;

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else if (System.currentTimeMillis() - mExitTime > 2000) {
            showSnackbar("再按一次退出程序");
            mExitTime = System.currentTimeMillis();
        } else {
            finish();
        }
    }

    @Override
    protected void initView() {
        mExitTime = 0;
        ActionBarDrawerToggle drawerToggle =
                new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, 0, 0) {
                    @Override
                    public void onDrawerOpened(View drawerView) {
                        super.onDrawerOpened(drawerView);
                    }
                    @Override
                    public void onDrawerClosed(View drawerView) {
                        super.onDrawerClosed(drawerView);
                        mPresenter.transFragment();
                    }
                };
        drawerToggle.syncState();
        mDrawerLayout.setDrawerListener(drawerToggle);
        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                return mPresenter.onNavigationItemSelected(item);
            }
        });
    }

    @Override
    protected void initPresenter() {
        int home = CimocApplication.getPreferences().getInt(PreferenceMaster.PREF_HOME, PreferenceMaster.HOME_CIMOC);
        mPresenter = new MainPresenter(this, PreferenceMaster.getHomeId(home));
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_main;
    }

    @Override
    protected View getLayoutView() {
        return mDrawerLayout;
    }

    @Override
    protected String getDefaultTitle() {
        int home = CimocApplication.getPreferences().getInt(PreferenceMaster.PREF_HOME, PreferenceMaster.HOME_CIMOC);
        return getResources().getStringArray(R.array.home_items)[home];
    }

    @Override
    protected BasePresenter getPresenter() {
        return mPresenter;
    }

    public void closeDrawer() {
        mDrawerLayout.closeDrawer(GravityCompat.START);
    }

    public void setToolbarTitle(String title) {
        mToolbar.setTitle(title);
    }

    public void setCheckedItem(int id) {
        mNavigationView.setCheckedItem(id);
    }

    public void nightlyOn() {
        maskView.setBackgroundColor(getResources().getColor(R.color.trans_black));
    }

    public void nightlyOff() {
        maskView.setBackgroundColor(getResources().getColor(R.color.trans_white));
    }

    public void showProgressBar() {
        mFrameLayout.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);
    }

    public void hideProgressBar() {
        mFrameLayout.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.GONE);
    }

}
