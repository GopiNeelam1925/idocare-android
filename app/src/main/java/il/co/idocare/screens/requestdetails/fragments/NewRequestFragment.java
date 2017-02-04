package il.co.idocare.screens.requestdetails.fragments;

import android.app.Activity;
import android.content.ContentValues;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import il.co.idocare.Constants;
import il.co.idocare.R;
import il.co.idocare.authentication.LoggedInUserEntity;
import il.co.idocare.contentproviders.IDoCareContract;
import il.co.idocare.controllers.fragments.AbstractFragment;
import il.co.idocare.eventbusevents.LocationEvents;
import il.co.idocare.mvcviews.newrequest.NewRequestViewMvc;
import il.co.idocare.mvcviews.newrequest.NewRequestViewMvcImpl;
import il.co.idocare.requests.RequestEntity;
import il.co.idocare.requests.RequestsManager;
import il.co.idocare.screens.requests.fragments.RequestsAllFragment;
import il.co.idocare.utils.Logger;


public class NewRequestFragment extends NewAndCloseRequestBaseFragment
        implements NewRequestViewMvc.NewRequestViewMvcListener {

    private static final String TAG = "NewRequestFragment";

    @Inject RequestsManager mRequestsManager;
    @Inject Logger mLogger;

    NewRequestViewMvc mNewRequestViewMvc;



    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        getControllerComponent().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mNewRequestViewMvc = new NewRequestViewMvcImpl(inflater, container);
        mNewRequestViewMvc.registerListener(this);

        return mNewRequestViewMvc.getRootView();
    }

    @Nullable
    @Override
    public Class<? extends Fragment> getHierarchicalParentFragment() {
        return RequestsAllFragment.class;
    }

    @Override
    public String getTitle() {
        return getResources().getString(R.string.new_request_fragment_title);
    }




    // ---------------------------------------------------------------------------------------------
    //
    // Callbacks from MVC view(s)

    @Override
    public void onCreateRequestClicked() {
        createRequest();
    }

    @Override
    public void onTakePictureClicked() {
        takePictureWithCamera();
    }

    // End callbacks from MVC view(s)
    //
    // ---------------------------------------------------------------------------------------------


    @Override
    protected void onNewPictureAdded(int index, @NonNull String pathToPicture) {
        mNewRequestViewMvc.showPicture(index, pathToPicture);
    }

    @Override
    protected int getMaxPictures() {
        return NewRequestViewMvc.MAX_PICTURES;
    }

    /**
     * Add new request to the local cache, mark it as modified and add the corresponding user
     * action to the local cache of user actions
     */
    private void createRequest() {
        mLogger.d(TAG, "createRequest()");

        String createdBy = mLoginStateManager.getLoggedInUser().getUserId();

        LocationEvents.BestLocationEstimateEvent bestLocationEstimateEvent =
                EventBus.getDefault().getStickyEvent(LocationEvents.BestLocationEstimateEvent.class);

        if (bestLocationEstimateEvent == null
                || !mLocationHelper.isAccurateLocation(bestLocationEstimateEvent.location)) {
            Log.d(TAG, "aborting request creation due to lack of, or insufficiently accurate " +
                    "location estimate");
            Toast.makeText(getActivity(), getString(R.string.msg_insufficient_location_accuracy),
                    Toast.LENGTH_LONG).show();
            return;
        }

        Location location = bestLocationEstimateEvent.location;

        double latitude = 0, longitude = 0;
        if (location != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
        }
        List<String> createdPictures = getPicturesPaths();
//        StringBuilder sb = new StringBuilder("");
//        for (int i=0; i < picturesPahts.size(); i++) {
//            sb.append(picturesPahts.get(i));
//            if (i < picturesPahts.size()-1) sb.append(Constants.PICTURES_LIST_SEPARATOR);
//        }
//        String createdPictures = sb.toString();

        Bundle bundleNewRequest = mNewRequestViewMvc.getViewState();
        String createdComment =
                bundleNewRequest.getString(NewRequestViewMvc.KEY_CREATED_COMMENT);

        // Generate a temporary ID for this request - the actual ID will be assigned by the server
        // TODO: this ID might be not unique
        long tempId = UUID.randomUUID().getLeastSignificantBits();

        long timestamp = System.currentTimeMillis();

        if (!validRequestParameters(createdBy, createdPictures)) {
            Log.d(TAG, "aborting request creation due to invalid parameters");
            return;
        }

        RequestEntity newRequest = RequestEntity.getBuilder()
                .setId(String.valueOf(tempId))
                .setCreatedBy(createdBy)
                .setCreatedAt(String.valueOf(timestamp))
                .setCreatedComment(createdComment)
                .setCreatedPictures(createdPictures)
                .setLongitude(longitude)
                .setLatitude(latitude)
                .setModifiedLocally(true)
                .build();

        mRequestsManager.addNewRequest(newRequest);

        mServerSyncController.requestImmediateSync(); // TODO: remove this after geocoder and names appear without sync
        mMainFrameHelper.replaceFragment(RequestsAllFragment.class, false, true, null);

//        // Create entries for user action corresponding to request's creation
//        final ContentValues userActionCV = new ContentValues();
//        userActionCV.put(IDoCareContract.UserActions.COL_TIMESTAMP, timestamp);
//        userActionCV.put(IDoCareContract.UserActions.COL_ENTITY_TYPE,
//                IDoCareContract.UserActions.ENTITY_TYPE_REQUEST);
//        userActionCV.put(IDoCareContract.UserActions.COL_ENTITY_ID, tempId);
//        userActionCV.put(IDoCareContract.UserActions.COL_ACTION_TYPE, IDoCareContract.UserActions.ACTION_TYPE_CREATE_REQUEST);
//
//
//        new AsyncTask<Void, Void, Void>() {
//            @Override
//            protected Void doInBackground(Void... voids) {
//                getActivity().getContentResolver().insert(IDoCareContract.Requests.CONTENT_URI, requestCV);
//                getActivity().getContentResolver().insert(IDoCareContract.UserActions.CONTENT_URI, userActionCV);
//                return null;
//            }
//
//            @Override
//            protected void onPostExecute(Void aVoid) {
//            }
//        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[] {null});

    }

    private boolean validRequestParameters(String userId, List<String> pictures) {

        if (TextUtils.isEmpty(userId)) {
            onUserLoggedOut();
            return false;
        }

        if (pictures == null || pictures.isEmpty()) {
            Toast.makeText(getActivity(), getString(R.string.msg_pictures_required),
                    Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }


}
