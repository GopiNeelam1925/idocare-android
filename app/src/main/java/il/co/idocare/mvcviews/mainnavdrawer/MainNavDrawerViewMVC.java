package il.co.idocare.mvcviews.mainnavdrawer;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import il.co.idocare.R;
import il.co.idocare.controllers.listadapters.NavigationDrawerListAdapter;
import il.co.idocare.datamodels.functional.NavigationDrawerEntry;
import il.co.idocare.datamodels.functional.UserItem;
import il.co.idocare.mvcviews.AbstractViewMVC;
import il.co.idocare.mvcviews.ViewMVC;
import il.co.idocare.mvcviews.navdrawerheader.NavDrawerHeaderViewMvc;
import il.co.idocare.mvcviews.navdrawerheader.NavDrawerHeaderViewMvcImpl;

/**
 * This MVC view represents application's main screen which contains NavigationDrawer and a single
 * FrameLayout in which app's screens will be presented
 */
public class MainNavDrawerViewMVC
        extends AbstractViewMVC<MainNavDrawerViewMVC.MainNavDrawerViewMVCListener>
        implements ViewMVC {



    public interface MainNavDrawerViewMVCListener {
        /**
         * Will be called when Navigation Drawer's visibility state changes
         * @param isVisible whether the drawer is visible now
         */
        void onDrawerVisibilityStateChanged(boolean isVisible);

        /**
         * Will be called when "navigate up" button is clicked
         */
        void onNavigationClick();

        /**
         * Will be called when entry from nav drawer is being chosen
         */
        void onDrawerEntryChosen(String entryName);
    }


    @NonNull
    private final AppCompatActivity mActivity;

    private Toolbar mToolbar;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mActionBarDrawerToggle;

    private NavigationDrawerListAdapter mNavDrawerAdapter;

    private MainNavDrawerViewMVCListener mListener;

    private FrameLayout mFrameHeader;
    private NavDrawerHeaderViewMvc mNavDrawerHeaderViewMvc;

    public MainNavDrawerViewMVC(@NonNull LayoutInflater inflater,
                                @Nullable ViewGroup container,
                                @NonNull AppCompatActivity activity) {
        mActivity = activity;
        setRootView(inflater.inflate(R.layout.layout_main_nav_drawer, container));


        init();
    }


    private void init() {
        initToolbar();
        initNavDrawer();
        initHeader();
    }

    private void initHeader() {
        mFrameHeader = (FrameLayout) getRootView().findViewById(R.id.frame_nav_drawer_header);

        mNavDrawerHeaderViewMvc = new NavDrawerHeaderViewMvcImpl(
                LayoutInflater.from(getRootView().getContext()), null);
        mFrameHeader.addView(mNavDrawerHeaderViewMvc.getRootView());

    }

    private void initToolbar() {
        mToolbar = (Toolbar) getRootView().findViewById(R.id.toolbar);
        mActivity.setSupportActionBar(mToolbar);
    }

    /**
     * Initiate the navigation drawer
     */
    private void initNavDrawer() {

        mDrawerLayout = (DrawerLayout) getRootView().findViewById(R.id.drawer_layout);

        mActionBarDrawerToggle = new ActionBarDrawerToggle(
                mActivity,
                mDrawerLayout,
                mToolbar,
                R.string.drawer_open,
                R.string.drawer_close) {

            private boolean mIsDrawerVisibleLast = false;

            @Override
            public void onDrawerSlide(View view, float v) {

                // Only update when drawer's visibility actually changed
                if (mIsDrawerVisibleLast != isDrawerVisible()) {
                    mIsDrawerVisibleLast = !mIsDrawerVisibleLast;
                    if (mListener != null)
                        mListener.onDrawerVisibilityStateChanged(mIsDrawerVisibleLast);
                }
            }
        };

        mActionBarDrawerToggle.setDrawerIndicatorEnabled(true);

        mActionBarDrawerToggle.setToolbarNavigationClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) mListener.onNavigationClick();
            }
        });

        // This is required because of a bug. More info:
        //http://stackoverflow.com/questions/26549008/missing-up-navigation-icon-after-switching-from-ics-actionbar-to-lollipop-toolbar
        if (mActivity.getDrawerToggleDelegate() != null) mActionBarDrawerToggle
                .setHomeAsUpIndicator(mActivity.getDrawerToggleDelegate().getThemeUpIndicator());

        mDrawerLayout.setDrawerListener(mActionBarDrawerToggle);

        initDrawerListView();
    }

    private void initDrawerListView() {

        final ListView drawerList = (ListView) mDrawerLayout.findViewById(R.id.drawer_list);

        // Set the adapter for the list view
        mNavDrawerAdapter = new NavigationDrawerListAdapter(getRootView().getContext(), 0);
        drawerList.setAdapter(mNavDrawerAdapter);

        drawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

                // Highlight the selected item and close the drawer
                drawerList.setItemChecked(position, true);
                closeDrawer();

                String chosenEntry = getRootView().getResources().getString(
                        mNavDrawerAdapter.getItem(position).getTitleResId());

                if (mListener != null) mListener.onDrawerEntryChosen(chosenEntry);
            }
        });
    }

    /**
     * Refresh drawer's entries
     */
    public void refreshDrawer(boolean isUserLoggedIn) {

        List<NavigationDrawerEntry> entries = new ArrayList<>(8);
        entries.add(new NavigationDrawerEntry(R.string.nav_drawer_entry_my, R.drawable.ic_drawer_assigned_to_me));
        entries.add(new NavigationDrawerEntry(R.string.nav_drawer_entry_new_request, R.drawable.ic_drawer_add_new_request));
        entries.add(new NavigationDrawerEntry(R.string.nav_drawer_entry_map, R.drawable.ic_drawer_location));
        entries.add(new NavigationDrawerEntry(R.string.nav_drawer_entry_requests_list, R.drawable.ic_drawer_requests_list));
        entries.add(new NavigationDrawerEntry(R.string.nav_drawer_entry_settings, R.drawable.ic_drawer_settings));

        // No need for both login/logout options at once
        if (isUserLoggedIn)
            entries.add(new NavigationDrawerEntry(R.string.nav_drawer_entry_logout, R.drawable.ic_drawer_logout));
        else
            entries.add(new NavigationDrawerEntry(R.string.nav_drawer_entry_login, 0));

        mNavDrawerAdapter.clear();
        mNavDrawerAdapter.addAll(entries);
        mNavDrawerAdapter.notifyDataSetChanged();
    }

    @Override
    public Bundle getViewState() {
        return null;
    }

    public void setTitle(String title) {
        mToolbar.setTitle(title);
    }


    public void bindUserData(@Nullable UserItem user) {
        mNavDrawerHeaderViewMvc.bindUserData(user);
    }

    /**
     * Register a listener that will be notified of events and interactions with this MVC view
     * @param listener a listener to notify; null to clear
     */
    public void setListener(MainNavDrawerViewMVCListener listener) {
        mListener = listener;
    }

    /**
     * See {@link ActionBarDrawerToggle#syncState()}
     */
    public void syncDrawerToggleState() {
        mActionBarDrawerToggle.syncState();
    }

    /**
     * See {@link ActionBarDrawerToggle#setDrawerIndicatorEnabled(boolean)}
     * @param enabled true un order to show drawer's indicator ("hamburger"); false in order to
     *                show "up" navigation icon
     */
    public void setDrawerIndicatorEnabled(boolean enabled) {
        mActionBarDrawerToggle.setDrawerIndicatorEnabled(enabled);
    }

    public boolean isDrawerVisible() {
        return mDrawerLayout.isDrawerVisible(GravityCompat.START);
    }

    public void closeDrawer() {
        mDrawerLayout.closeDrawer(GravityCompat.START);
    }

}
