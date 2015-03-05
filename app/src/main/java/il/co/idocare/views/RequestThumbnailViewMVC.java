package il.co.idocare.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener;

import il.co.idocare.Constants;
import il.co.idocare.R;
import il.co.idocare.handlermessaging.HandlerMessagingSlave;
import il.co.idocare.models.RequestsMVCModel;
import il.co.idocare.models.UsersMVCModel;
import il.co.idocare.pojos.RequestItem;

/**
 * This is the top level View which should be used as a "thumbnail" for requests
 * when they are displayed in a list.
 */
public class RequestThumbnailViewMVC extends RelativeLayout implements
        ViewMVC,
        HandlerMessagingSlave {

    private static final String LOG_TAG = "RequestThumbnailViewMVC";

    private final Object LOCK = new Object();

    private Handler mInboxHandler;
    private RequestsMVCModel mRequestsModel;
    private UsersMVCModel mUsersModel;

   private RequestItem mRequestItem;


    private TextView mTxtRequestStatus;
    private TextView mTxtRequestLocation;
    private ImageView mImgRequestThumbnail;
    private TextView mTxtNoRequestThumbnailPicture;
    private TextView mTxtCreatedComment;
    private TextView mTxtCreatedBy;
    private TextView mTxtCreatedAt;
    private TextView mTxtCreatedVotes;

    private boolean mIsClosed;
    private boolean mIsPickedUp;



    public RequestThumbnailViewMVC(Context context, RequestsMVCModel requestsModel,
                                   UsersMVCModel usersModel) {
        super(context);
        mRequestsModel = requestsModel;
        mUsersModel = usersModel;

        init(context);
    }



    /**
     * All three constructors invoke this method.
     */
    private void init(Context context) {

        // Inflate the underlying layout
        LayoutInflater.from(context).inflate(R.layout.layout_request_thumbnail, this, true);

        // This padding is required in order not to hide the border when colorizing inner views
        int padding = (int) getResources().getDimension(R.dimen.border_background_width);
        getRootView().setPadding(padding, padding, padding, padding);

        // Set background color and border for the whole item
        getRootView().setBackgroundColor(getResources().getColor(android.R.color.white));
        getRootView().setBackgroundResource(R.drawable.border_background);


        mTxtRequestStatus = (TextView) findViewById(R.id.txt_request_status);
        mTxtRequestLocation = (TextView) findViewById(R.id.txt_request_fine_location);
        mImgRequestThumbnail = (ImageView) findViewById(R.id.img_request_thumbnail);
        mTxtNoRequestThumbnailPicture =
                (TextView) findViewById(R.id.txt_no_request_thumbnail_picture);
        mTxtCreatedComment = (TextView) findViewById(R.id.txt_created_comment);
        mTxtCreatedBy = (TextView) findViewById(R.id.txt_created_by);
        mTxtCreatedAt = (TextView) findViewById(R.id.txt_date);
        mTxtCreatedVotes = (TextView) findViewById(R.id.txt_created_votes);

        mIsClosed = false;
        mIsPickedUp = false;
    }


    @Override
    public Handler getInboxHandler() {

        // Since most of the work done in MVC Views consist of manipulations on underlying
        // Android Views, it will be convenient (and less error prone) if MVC View's inbox Handler
        // will be running on UI thread.
        if (mInboxHandler == null) {
            mInboxHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    RequestThumbnailViewMVC.this.handleMessage(msg);
                }
            };
        }
        return mInboxHandler;
    }


    @Override
    public Bundle getViewState() {
        return null;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // Register for updates with requests and users models
        mRequestsModel.addOutboxHandler(getInboxHandler());
        mUsersModel.addOutboxHandler(getInboxHandler());
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // Unregistering from updates with requests and users models
        mRequestsModel.removeOutboxHandler(getInboxHandler());
        mUsersModel.removeOutboxHandler(getInboxHandler());
    }


    private void handleMessage(Message msg) {
        // TODO: write request/user update logic
        switch (Constants.MESSAGE_TYPE_VALUES[msg.what]) {
            case M_USER_DATA_UPDATE:
                long userId = ((Long)msg.obj);
                synchronized (LOCK) {
                    if (mRequestItem.getCreatedBy() == userId) {
                        updateUsers();
                    }
                }
                break;

            case M_REQUEST_DATA_UPDATE:
                long requestId = ((Long)msg.obj);

                synchronized (LOCK) {
                    if (mRequestItem.getId() == requestId) {
                        showRequest(requestId);
                    }
                }

            default:
                break;
        }
    }
    /**
     * Update this thumbnail with the details of the request having the specified ID
     * @param requestId id of the request item which should be shown
     */
    public void showRequest(long requestId) {

        synchronized (LOCK) {
            // Get the request item
            mRequestItem = mRequestsModel.getRequest(requestId);
            if (mRequestItem == null) {
                Log.e(LOG_TAG, "could not find request in the model. ID: " + String.valueOf(requestId));
            }

        }

        // Update the UI
        setStatus();
        setColors();
        setTexts();
        setPictures();

        // Update user's details
        updateUsers();
    }



    private void setStatus() {
        if (mRequestItem.getClosedBy() != 0) {
            mIsClosed = true;
        }
        else if (mRequestItem.getPickedUpBy() != 0) {
            mIsClosed = false;
            mIsPickedUp = true;
        }
        else {
            mIsClosed = false;
            mIsPickedUp = false;
        }
    }

    private void setColors() {
        int statusColor;

        if (mIsClosed)
            statusColor = getResources().getColor(R.color.closed_request_status);
        else if (mIsPickedUp)
            statusColor = getResources().getColor(R.color.picked_up_request_status);
        else
            statusColor = getResources().getColor(R.color.new_request_status);

        mTxtRequestStatus.setBackgroundColor(statusColor);
        mTxtRequestLocation.setBackgroundColor(statusColor);
    }

    private void setTexts() {
        if (mIsClosed)
            mTxtRequestStatus.setText(getResources().getString(R.string.txt_closed_request_title));
        else if (mIsPickedUp)
            mTxtRequestStatus.setText(getResources().getString(R.string.txt_picked_up_request_title));
        else
            mTxtRequestStatus.setText(getResources().getString(R.string.txt_new_request_title));

        // TODO: need to set city name
        mTxtRequestLocation.setText("TODO City Name");

        mTxtCreatedComment.setText(mRequestItem.getCreatedComment());
        mTxtCreatedAt.setText(mRequestItem.getCreatedAt());

        // TODO: set actual votes
        mTxtCreatedVotes.setText("TODO\nVotes");
    }


    private void setPictures() {

        mImgRequestThumbnail.setVisibility(View.GONE);
        mTxtNoRequestThumbnailPicture.setVisibility(View.VISIBLE);

        if (mRequestItem.getCreatedPictures() != null && mRequestItem.getCreatedPictures().length > 0) {

            ImageLoader.getInstance().displayImage(
                    mRequestItem.getCreatedPictures()[0],
                    mImgRequestThumbnail,
                    Constants.DEFAULT_DISPLAY_IMAGE_OPTIONS,
                    new RequestThumbnailLoadingListener(mTxtNoRequestThumbnailPicture,
                            getResources().getString(R.string.text_request_thumbnail_loading),
                            getResources().getString(R.string.text_request_thumbnail_failed),
                            getResources().getString(R.string.text_request_thumbnail_cancelled)),
                    new RequestThumbnailLoadingProgressListener(mTxtNoRequestThumbnailPicture,
                            getResources().getString(R.string.text_request_thumbnail_loading)));

        } else {
            mTxtNoRequestThumbnailPicture.
                    setText(getResources().getString(R.string.text_request_thumbnail_no_picture));
        }
    }

    private void updateUsers() {
        long createdBy;
        synchronized (LOCK) {
            // We need this sync for the situation when both the request and some relevant user
            // are updated simultaneously (prevent taking createdBy from the old request)
            createdBy = mRequestItem.getCreatedBy();
        }
        mTxtCreatedBy.setText(mUsersModel.getUser(createdBy).getNickname());
    }

    /**
     * ImageLoadingListener used for alternating between TextView and ImageView when
     * request thumbnail is being loaded
     */
    private static class RequestThumbnailLoadingListener implements ImageLoadingListener {

        TextView mTxtBeforeImage;
        String mLoading;
        String mFailed;
        String mCancelled;

        public RequestThumbnailLoadingListener(TextView txtBeforeImage, String loading,
                                               String failed, String cancelled) {
            mTxtBeforeImage = txtBeforeImage;
            mLoading = loading;
            mFailed = failed;
            mCancelled = cancelled;
        }


        @Override
        public void onLoadingStarted(String imageUri, View view) {
            // Make TextView visible and set loading text
            mTxtBeforeImage.setVisibility(View.VISIBLE);
            mTxtBeforeImage.setText(mLoading);
            // Hide the ImageView
            view.setVisibility(View.GONE);
        }

        @Override
        public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
            mTxtBeforeImage.setText(mFailed);
        }

        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            // Hide TextView
            mTxtBeforeImage.setVisibility(View.GONE);
            // Show ImageView
            view.setVisibility(View.VISIBLE);
            AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
            anim.setDuration(500);
            anim.setRepeatCount(0);
            view.startAnimation(anim);
        }

        @Override
        public void onLoadingCancelled(String imageUri, View view) {
            mTxtBeforeImage.setText(mCancelled);
        }
    }

    /**
     * ImageLoadingProgressListener used to animate request thumbnail while the picture hasn't
     * been loaded yet.
     */
    private static class RequestThumbnailLoadingProgressListener implements ImageLoadingProgressListener {

        TextView mTxtBeforeImage;
        String mLoadingText;
        int mNumOfDots;

        public RequestThumbnailLoadingProgressListener(TextView txtBeforeImage, String text) {
            mTxtBeforeImage = txtBeforeImage;
            mLoadingText = text;
            mNumOfDots = 0;
        }

        @Override
        public void onProgressUpdate(String imageUri, View view, int current, int total) {
            // Add the required amount of dots to the end of loading string
            String dot = ".";
            String textWithDots = mLoadingText + new String(new char[mNumOfDots % 4]).replace("\0", dot);
            // Set the new text and increase the "dot counter"
            mTxtBeforeImage.setText(textWithDots);
            mNumOfDots++;


        }
    }
}
