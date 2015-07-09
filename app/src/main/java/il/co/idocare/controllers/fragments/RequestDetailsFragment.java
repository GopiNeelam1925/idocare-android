package il.co.idocare.controllers.fragments;

import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import il.co.idocare.Constants;
import il.co.idocare.R;
import il.co.idocare.contentproviders.IDoCareContract;
import il.co.idocare.controllers.interfaces.RequestUserActionApplier;
import il.co.idocare.controllers.listadapters.RequestUserActionApplierImpl;
import il.co.idocare.pojos.RequestItem;
import il.co.idocare.pojos.UserActionItem;
import il.co.idocare.pojos.UserItem;
import il.co.idocare.views.RequestDetailsViewMVC;


public class RequestDetailsFragment extends AbstractFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private final static String LOG_TAG = RequestDetailsFragment.class.getSimpleName();

    private final static int REQUEST_LOADER = 0;
    private final static int USERS_LOADER = 1;
    private final static int USER_ACTIONS_LOADER = 2;

    private RequestDetailsViewMVC mRequestDetailsViewMVC;

    private long mRequestId;
    private RequestItem mRequestItem;

    private Cursor mUsersCursor;
    private Cursor mUserActionsCursor;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mRequestDetailsViewMVC =
                new RequestDetailsViewMVC(getActivity(), container, savedInstanceState);

        setActionBarTitle(getTitle());


        Bundle args = getArguments();
        if (args == null) {
            // TODO: handle this error somehow (maybe pop back stack?)
            Log.e(LOG_TAG, "RequestDetailsFragment was started with no arguments");
        } else {
            mRequestId = args.getLong(Constants.FIELD_NAME_REQUEST_ID);
        }

        getLoaderManager().initLoader(REQUEST_LOADER, null, this);

        return mRequestDetailsViewMVC.getRootView();
    }

    @Override
    public void onStart() {
        super.onStart();

        // Provide inbox Handler to the MVC View
        mRequestDetailsViewMVC.addOutboxHandler(getInboxHandler());
        // Add MVC View's Handler to the set of outbox Handlers
        addOutboxHandler(mRequestDetailsViewMVC.getInboxHandler());

    }

    @Override
    public void onStop() {
        super.onStop();

        // Remove "listener" handlers between this MVC controller and MVC views
        mRequestDetailsViewMVC.removeOutboxHandler(getInboxHandler());
        removeOutboxHandler(mRequestDetailsViewMVC.getInboxHandler());

    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(Constants.FIELD_NAME_REQUEST_ID, mRequestId);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        mRequestId = savedInstanceState.getLong(Constants.FIELD_NAME_REQUEST_ID);
    }

    @Override
    public boolean isTopLevelFragment() {
        return false;
    }

    @Override
    public Class<? extends AbstractFragment> getNavHierParentFragment() {
        return HomeFragment.class;
    }

    @Override
    public String getTitle() {
        return getResources().getString(R.string.request_details_fragment_title);
    }


    @Override
    protected void handleMessage(Message msg) {
        // TODO: complete this method
        switch (Constants.MESSAGE_TYPE_VALUES[msg.what]) {
            case V_PICKUP_REQUEST_BUTTON_CLICKED:
                pickupRequest();
                break;
            case V_CLOSE_REQUEST_BUTTON_CLICKED:
                closeRequest();
                break;
            case V_CREATED_VOTE_UP_BUTTON_CLICKED:
                voteForRequest(1, false);
                break;
            case V_CREATED_VOTE_DOWN_BUTTON_CLICKED:
                voteForRequest(-1, false);
                break;
            case V_CLOSED_VOTE_UP_BUTTON_CLICKED:
                voteForRequest(1, true);
                break;
            case V_CLOSED_VOTE_DOWN_BUTTON_CLICKED:
                voteForRequest(-1, true);
                break;
            default:
                break;
        }
    }




    // ---------------------------------------------------------------------------------------------
    //
    // User actions handling


    private void pickupRequest() {

        if (mRequestItem.getPickedUpBy() != 0) {
            Log.e(LOG_TAG, "tried to pickup an already picked up request");
            return;
        }

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                showProgressDialog("Please wait...", "Updating the request...");
            }

            @Override
            protected Void doInBackground(Void... voids) {

                ContentValues userActionCV = new ContentValues(5);
                userActionCV.put(IDoCareContract.UserActions.COL_TIMESTAMP, System.currentTimeMillis());
                userActionCV.put(IDoCareContract.UserActions.COL_ENTITY_TYPE,
                        IDoCareContract.UserActions.ENTITY_TYPE_REQUEST);
                userActionCV.put(IDoCareContract.UserActions.COL_ENTITY_ID, mRequestId);
                userActionCV.put(IDoCareContract.UserActions.COL_ACTION_TYPE,
                        IDoCareContract.UserActions.ACTION_TYPE_PICKUP_REQUEST);
                userActionCV.put(IDoCareContract.UserActions.COL_ACTION_PARAM,
                        getActiveAccount().name);

                Uri newUri = getContentResolver().insert(
                        IDoCareContract.UserActions.CONTENT_URI,
                        userActionCV
                );

                if (newUri != null) {
                    ContentValues requestCV = new ContentValues(1);
                    requestCV.put(IDoCareContract.Requests.COL_MODIFIED_LOCALLY_FLAG, 1);
                    int updated = getContentResolver().update(
                            ContentUris.withAppendedId(IDoCareContract.Requests.CONTENT_URI,
                                    mRequestId),
                            requestCV,
                            null,
                            null
                    );
                    if (updated != 1)
                        Log.e(LOG_TAG, "failed to set 'LOCALLY_MODIFIED' flag on request entry" +
                                "after a vote");
                }

                // Request pickup is time critical action - need to be uploaded to the server ASAP
                Bundle settingsBundle = new Bundle();
                settingsBundle.putBoolean(
                        ContentResolver.SYNC_EXTRAS_MANUAL, true);
                settingsBundle.putBoolean(
                        ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
                ContentResolver.requestSync(getActiveAccount(), IDoCareContract.AUTHORITY,
                        settingsBundle);

                return (Void) null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                dismissProgressDialog();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }

    private void closeRequest() {

        if (mRequestItem == null) {
            Log.e(LOG_TAG, "closeRequest() was called, but there is no data about the request");
            return;
        }

        Bundle args = new Bundle();
        args.putLong(Constants.FIELD_NAME_REQUEST_ID, mRequestId);
        replaceFragment(CloseRequestFragment.class, true, args);
    }


    private void voteForRequest(final int amount, final boolean voteForClosed) {

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                showProgressDialog("Please wait...", "Updating the request...");
            }

            @Override
            protected Void doInBackground(Void... voids) {

                ContentValues userActionCV = new ContentValues(6);
                userActionCV.put(IDoCareContract.UserActions.COL_TIMESTAMP, System.currentTimeMillis());
                userActionCV.put(IDoCareContract.UserActions.COL_ENTITY_TYPE,
                        IDoCareContract.UserActions.ENTITY_TYPE_REQUEST);
                userActionCV.put(IDoCareContract.UserActions.COL_ENTITY_ID, mRequestId);
                userActionCV.put(IDoCareContract.UserActions.COL_ENTITY_PARAM, voteForClosed ?
                        IDoCareContract.UserActions.ENTITY_PARAM_REQUEST_CLOSED :
                        IDoCareContract.UserActions.ENTITY_PARAM_REQUEST_CREATED);
                userActionCV.put(IDoCareContract.UserActions.COL_ACTION_TYPE,
                        IDoCareContract.UserActions.ACTION_TYPE_VOTE);
                userActionCV.put(IDoCareContract.UserActions.COL_ACTION_PARAM, String.valueOf(amount));

                Uri newUri = getContentResolver().insert(
                        IDoCareContract.UserActions.CONTENT_URI,
                        userActionCV
                );

                if (newUri != null) {
                    ContentValues requestCV = new ContentValues(1);
                    requestCV.put(IDoCareContract.Requests.COL_MODIFIED_LOCALLY_FLAG, 1);
                    int updated = getContentResolver().update(
                            ContentUris.withAppendedId(IDoCareContract.Requests.CONTENT_URI,
                                    mRequestId),
                            requestCV,
                            null,
                            null
                    );
                    if (updated != 1)
                        Log.e(LOG_TAG, "failed to set 'LOCALLY_MODIFIED' flag on request entry" +
                                "after a vote");
                }
                return (Void) null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                dismissProgressDialog();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }



    // End of user actions handling
    //
    // ---------------------------------------------------------------------------------------------



    // ---------------------------------------------------------------------------------------------
    //
    // LoaderCallback methods

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {

        if (id == REQUEST_LOADER) {

            String[] projection = IDoCareContract.Requests.PROJECTION_ALL;

            // Change these values when adding filtering and sorting
            String selection = null;
            String[] selectionArgs = null;
            String sortOrder = null;

            //noinspection ConstantConditions
            return new CursorLoader(getActivity(),
                    ContentUris.withAppendedId(IDoCareContract.Requests.CONTENT_URI, mRequestId),
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder);

        } else if (id == USERS_LOADER) {

            if (mRequestItem == null) {
                Log.e(LOG_TAG, "can't initialize users CursorLoader without request data!");
                return null;
            }

            String[] projection = IDoCareContract.Users.PROJECTION_ALL;

            StringBuilder placeHolders = new StringBuilder(10);
            ArrayList<String> selectionArgsList = new ArrayList<>(3);
            if (mRequestItem.getCreatedBy() != 0) {
                placeHolders.append("?");
                selectionArgsList.add(String.valueOf(mRequestItem.getCreatedBy()));
            }
            if (mRequestItem.getPickedUpBy() != 0) {
                placeHolders.append(", ?");
                selectionArgsList.add(String.valueOf(mRequestItem.getPickedUpBy()));
            }
            if (mRequestItem.getClosedBy() != 0) {
                placeHolders.append(", ?");
                selectionArgsList.add(String.valueOf(mRequestItem.getClosedBy()));
            }

            // Change these values when adding filtering and sorting
            String selection = IDoCareContract.Users.COL_USER_ID +
                    " IN (" + placeHolders.toString() + ")";

            String[] selectionArgs = selectionArgsList.toArray(new String[selectionArgsList.size()]);

            String sortOrder = null;

            //noinspection ConstantConditions
            return new CursorLoader(getActivity(),
                    IDoCareContract.Users.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder);

        } else if (id == USER_ACTIONS_LOADER) {

            if (mRequestItem == null) {
                Log.e(LOG_TAG, "can't initialize user actions CursorLoader without request data!");
                return null;
            }

            String[] projection = IDoCareContract.UserActions.PROJECTION_ALL;

            // Change these values when adding filtering and sorting
            String selection = IDoCareContract.UserActions.COL_ENTITY_TYPE + " = ? AND " +
                    IDoCareContract.UserActions.COL_ENTITY_ID + " = ?";

            String[] selectionArgs = new String[] {IDoCareContract.UserActions.ENTITY_TYPE_REQUEST,
                    String.valueOf(mRequestId)};

            String sortOrder = null;

            //noinspection ConstantConditions
            return new CursorLoader(getActivity(),
                    IDoCareContract.UserActions.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder);
        }
        else {
            Log.e(LOG_TAG, "onCreateLoader() called with unrecognized id: " + id);
            return null;
        }

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (loader.getId() == REQUEST_LOADER) {

            if (cursor != null && cursor.moveToFirst()) {

                mRequestItem = RequestItem.create(cursor, Long.valueOf(getActiveAccount().name));

                refreshView();

                if (mRequestItem != null) {
                    // Once got request's data - init users and user actions loaders
                    if (getLoaderManager().getLoader(USERS_LOADER) == null)
                        getLoaderManager().initLoader(USERS_LOADER, null, this);
                    if (getLoaderManager().getLoader(USER_ACTIONS_LOADER) == null)
                        getLoaderManager().initLoader(USER_ACTIONS_LOADER, null, this);
                }

            } else {
                // If the returned cursor is empty, this might indicate that the ID of the request
                // changed (due to uploading to the server) - if it is the case, we need to restart
                // the loader
                if (cursor != null) {
                    Cursor idMappingCursor = null;
                    idMappingCursor = getContentResolver().query(
                            ContentUris.withAppendedId(IDoCareContract.TempIdMappings.CONTENT_URI,
                                    mRequestId),
                            IDoCareContract.TempIdMappings.PROJECTION_ALL,
                            null,
                            null,
                            null
                    );
                    if (idMappingCursor != null && idMappingCursor.moveToFirst()) {
                        mRequestId = idMappingCursor.getLong(idMappingCursor.getColumnIndexOrThrow(
                                IDoCareContract.TempIdMappings.COL_PERMANENT_ID));

                        getLoaderManager().restartLoader(REQUEST_LOADER, null, this);
                    }
                    if (idMappingCursor != null) idMappingCursor.close();
                }
            }

        } else if (loader.getId() == USERS_LOADER) {

            mUsersCursor = cursor;
            refreshView();

        } else if (loader.getId() == USER_ACTIONS_LOADER) {

            mUserActionsCursor = cursor;
            refreshView();

        } else {
            Log.e(LOG_TAG, "onLoadFinished() called with unrecognized loader id: " + loader.getId());
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (loader.getId() == REQUEST_LOADER) {
            // TODO: should we do s.t. here? Maybe mRequestDetailsViewMVC.bindRequestItem(null)?
        } else if (loader.getId() == USERS_LOADER) {
            // TODO: should do anything here?
        } else if (loader.getId() == USER_ACTIONS_LOADER) {
            // TODO: should do anything here?
        } else {
            Log.e(LOG_TAG, "onLoaderReset() called with unrecognized loader id: " + loader.getId());
        }

    }


    // End of LoaderCallback methods
    //
    // ---------------------------------------------------------------------------------------------




    private void refreshView() {

        RequestItem combinedRequestItem = RequestItem.create(mRequestItem);


        if (mUserActionsCursor != null && mUserActionsCursor.moveToFirst()) {

            RequestUserActionApplier requestUserActionApplier = new RequestUserActionApplierImpl();
            do {
                UserActionItem userAction = UserActionItem.create(mUserActionsCursor);
                combinedRequestItem = requestUserActionApplier.applyUserAction(combinedRequestItem,
                        userAction);
            } while (mUserActionsCursor.moveToNext());
        }

        mRequestDetailsViewMVC.bindRequestItem(combinedRequestItem);

        if (mUsersCursor != null && mUsersCursor.moveToFirst()) {
            do {
                UserItem user = UserItem.create(mUsersCursor);

                if (user.getId() == combinedRequestItem.getCreatedBy()) {
                    mRequestDetailsViewMVC.bindCreatedByUser(user);
                } else if (user.getId() == combinedRequestItem.getPickedUpBy()) {
                    mRequestDetailsViewMVC.bindPickedUpByUser(user);
                } else if (user.getId() == combinedRequestItem.getClosedBy()) {
                    mRequestDetailsViewMVC.bindClosedByUser(user);
                } else {
                    Log.e(LOG_TAG, "user's data returned in the mUsersCursor does not correspond to" +
                            "either of creating, picking up or closing user IDs in the request.");
                }

            } while (mUsersCursor.moveToNext());
        }
    }
}
