package il.co.idocare.controllers.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import il.co.idocare.Constants;
import il.co.idocare.controllers.adapters.NavigationDrawerListAdapter;
import il.co.idocare.controllers.fragments.HomeFragment;
import il.co.idocare.controllers.fragments.LoginFragment;
import il.co.idocare.controllers.fragments.AbstractFragment;
import il.co.idocare.R;
import il.co.idocare.controllers.fragments.NewRequestFragment;
import il.co.idocare.controllers.fragments.SplashFragment;
import il.co.idocare.models.RequestsMVCModel;
import il.co.idocare.models.UsersMVCModel;
import il.co.idocare.pojos.NavigationDrawerEntry;
import il.co.idocare.utils.UtilMethods;


public class MainActivity extends Activity implements
        AbstractFragment.IDoCareFragmentCallback,
        FragmentManager.OnBackStackChangedListener {

    private static final String LOG_TAG = "MainActivity";


    public GoogleApiClient mGoogleApiClient;

    private RequestsMVCModel mRequestsModel;

    private UsersMVCModel mUsersModel;



    // ---------------------------------------------------------------------------------------------
    //
    // Activity lifecycle management

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Decide which fragment to show if the app is not restored
        if (savedInstanceState == null) {

            getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
            if (getActionBar() != null) getActionBar().hide();

            setContentView(R.layout.activity_main);

            SharedPreferences prefs = getSharedPreferences(Constants.PREFERENCES_FILE, MODE_PRIVATE);

            if (prefs.contains(Constants.FieldName.USER_ID.getValue()) &&
                    prefs.contains(Constants.FieldName.USER_PUBLIC_KEY.getValue())) {
                // Show splash screen if user details exist
                replaceFragment(SplashFragment.class, false, null);
            } else {
                // Bring up login fragment
                replaceFragment(LoginFragment.class, false, null);
            }

        } else {

            getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
            if (getActionBar() != null) getActionBar().show();

            setContentView(R.layout.activity_main);
        }

        initializeModels();

        initUniversalImageLoader();

        buildGoogleApiClient();

        setupDrawer();

        // This callback will be used to show/hide up (back) button in actionbar
        getFragmentManager().addOnBackStackChangedListener(this);


    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mGoogleApiClient != null) mGoogleApiClient.connect();

        // TODO: verify that this call resolves the missing UP button when the activity is restarted
        onBackStackChanged();

    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mGoogleApiClient != null) mGoogleApiClient.disconnect();

    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        boolean actionsVisibility = !drawerLayout.isDrawerVisible(Gravity.START);

        for(int i=0;i<menu.size();i++){
            menu.getItem(i).setVisible(actionsVisibility);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    // End of activity lifecycle management
    //
    // ---------------------------------------------------------------------------------------------

    // ---------------------------------------------------------------------------------------------
    //
    // Back stack management


    @Override
    public void onBackStackChanged() {
        if (getActionBar() != null) {
            // Enable Up button only  if there are entries in the back stack
            boolean hasBackstackEntries = getFragmentManager().getBackStackEntryCount() > 0;
            getActionBar().setDisplayHomeAsUpEnabled(hasBackstackEntries);
        }
    }

    @Override
    public boolean onNavigateUp() {
        getFragmentManager().popBackStack();
        return true;
    }

    // End of back stack management
    //
    // ---------------------------------------------------------------------------------------------

    // ---------------------------------------------------------------------------------------------
    //
    // Fragments management

    // TODO: maybe we need to preserve the state of the replaced fragments?
    @Override
    public void replaceFragment(Class<? extends AbstractFragment> claz, boolean addToBackStack,
                                Bundle args) {

        if (isFragmentShown(claz)) {
            // The requested fragment is already shown - nothing to do
            Log.v(LOG_TAG, "the fragment " + claz.getSimpleName() + " is already shown");
            return;
        }

        // Set default padding for the main frame layout. Fragments might overwrite in
        // their onCreateView
        UtilMethods.setPaddingPx(findViewById(R.id.frame_contents),
                (int) getResources().getDimension(R.dimen.frame_contents_padding));

        FragmentTransaction ft = getFragmentManager().beginTransaction();

        // Create new fragment
        AbstractFragment newFragment;

        try {
            newFragment = claz.newInstance();
            if (args != null) newFragment.setArguments(args);
        } catch (InstantiationException e) {
            e.printStackTrace();
            return;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return;
        }

        if (newFragment.isTopLevelFragment()) {
            // Top level fragments don't have UP button
            getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        } else if (addToBackStack) {
            ft.addToBackStack(null);
        }

        // Change to a new fragment
        ft.replace(R.id.frame_contents, newFragment, claz.getClass().getSimpleName());
        ft.commit();

    }

    /**
     * Check whether a fragment of a specific class is currently shown
     * @param claz class of fragment to test. Null considered as "test no fragment shown"
     * @return true if fragment of the same class (or a superclass) is currently shown
     */
    private boolean isFragmentShown(Class<? extends AbstractFragment> claz) {
        Fragment currFragment = getFragmentManager().findFragmentById(R.id.frame_contents);


        return (currFragment == null && claz == null) || (
                currFragment != null && claz.isInstance(currFragment));
    }

    // End of fragments management
    //
    // ---------------------------------------------------------------------------------------------

    // ---------------------------------------------------------------------------------------------
    //
    // Navigation drawer management

    /**
     * Initiate the navigation drawer
     */
    private void setupDrawer() {

        setupDrawerListView();

        setupDrawerAndActionBarDependencies();

    }

    @SuppressLint("NewApi")
    private void setupDrawerListView() {

        final DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        final ListView drawerList = (ListView) findViewById(R.id.drawer_contents);

        // Set the adapter for the list view
        final NavigationDrawerListAdapter adapter = new NavigationDrawerListAdapter(this, 0);
        drawerList.setAdapter(adapter);

        // Populate the adapter with entries
        String[] entries = getResources().getStringArray(R.array.nav_drawer_entries);
        TypedArray icons = getResources().obtainTypedArray(R.array.nav_drawer_icons);

        for (int i=0; i<entries.length; i++) {
            adapter.add(new NavigationDrawerEntry(entries[i], icons.getResourceId(i, 0)));
        }

        icons.recycle();

        drawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

                // Highlight the selected item and close the drawer
                drawerList.setItemChecked(position, true);
                drawerLayout.closeDrawer(drawerList);

                String chosenEntry = adapter.getItem(position).getTitle();

                // Can't do switch/case on Strings :(
                if (chosenEntry.equals(getResources().getString(R.string.nav_drawer_entry_home))) {
                    replaceFragment(HomeFragment.class, false, null);
                }
                else if (chosenEntry.equals(getResources().getString(R.string.nav_drawer_entry_new_request))) {
                    replaceFragment(NewRequestFragment.class, false, null);
                }
                else if (chosenEntry.equals(getResources().getString(R.string.nav_drawer_entry_logout))) {
                    MainActivity.this.logOutCurrentUser();
                }
                else {
                    Log.e(LOG_TAG, "drawer entry \"" + chosenEntry + "\" has no functionality");
                }

                // Clear back-stack
                // TODO: this is correct only if all entries in the drawer are "top level"
                getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

            }
        });
    }

    private void setupDrawerAndActionBarDependencies() {

        final DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        drawerLayout.setDrawerListener(new DrawerLayout.DrawerListener() {

            private boolean mIsDrawerVisibleLast = false;

            @Override
            public void onDrawerSlide(View view, float v) {
                boolean isDrawerVivible = drawerLayout.isDrawerVisible(Gravity.START);

                // For performance update the action bar only when the state of drawer changes
                if (isDrawerVivible != mIsDrawerVisibleLast) {

                    if (isDrawerVivible) {
                        setActionBarTitle(0);
                    } else {
                        Fragment currFragment = getFragmentManager().findFragmentById(R.id.frame_contents);
                        if (currFragment != null) {
                            setActionBarTitle(((AbstractFragment)currFragment).getTitle());
                        }
                    }

                    invalidateOptionsMenu();

                    mIsDrawerVisibleLast = isDrawerVivible;
                }
            }

            @Override
            public void onDrawerOpened(View view) {

            }

            @Override
            public void onDrawerClosed(View view) {
            }

            @Override
            public void onDrawerStateChanged(int state) {
            }
        });
    }

    // End of navigation drawer management
    //
    // ---------------------------------------------------------------------------------------------


    // ---------------------------------------------------------------------------------------------
    //
    // Action bar management

    public void setActionBarTitle(int resourceId) {

        if (getActionBar() != null ) {
            getActionBar().show();
            if (resourceId != 0) {
                getActionBar().setTitle(resourceId);
            } else {
                getActionBar().setTitle("");
            }
        }
    }

    // End of action bar management
    //
    // ---------------------------------------------------------------------------------------------



    // ---------------------------------------------------------------------------------------------
    //
    // User session management

    private void logOutCurrentUser() {
        SharedPreferences prefs =
                getSharedPreferences(Constants.PREFERENCES_FILE, Context.MODE_PRIVATE);

        prefs.edit().remove(Constants.FieldName.USER_ID.getValue()).apply();
        prefs.edit().remove(Constants.FieldName.USER_PUBLIC_KEY.getValue()).commit();
        replaceFragment(LoginFragment.class, false, null);

    }

    // End of user session management
    //
    // ---------------------------------------------------------------------------------------------


    // ---------------------------------------------------------------------------------------------
    //
    // Models management

    private void initializeModels() {
        mRequestsModel = new RequestsMVCModel(this);
        mRequestsModel.initialize();
        mUsersModel = new UsersMVCModel(this);
    }

    @Override
    public RequestsMVCModel getRequestsModel() {
        return mRequestsModel;
    }

    @Override
    public UsersMVCModel getUsersModel() {
        return mUsersModel;
    }

    // End of models management
    //
    // ---------------------------------------------------------------------------------------------



    /**
     * Handle the initiation of UIL (third party package under Apache 2.0 license)
     */
    private void initUniversalImageLoader() {
        // TODO: alter the configuration of UIL according to our needs
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this)
                .defaultDisplayImageOptions(Constants.DEFAULT_DISPLAY_IMAGE_OPTIONS)
                .build();
        ImageLoader.getInstance().init(config);
    }

    /**
     * Initialize the client which will be used to connect to Google Play Services
     */
    private void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .build();
    }


}
