package il.co.idocare.networking;

import android.accounts.Account;
import android.content.ContentProviderClient;
import android.content.ContentUris;
import android.database.Cursor;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import il.co.idocare.Constants;
import il.co.idocare.contentproviders.IDoCareContract;
import il.co.idocare.pojos.RequestItem;
import il.co.idocare.pojos.UserActionItem;

/**
 * This class provides functionality used by DataUploader
 */
public class DataUploaderAssistant {

    private static final String LOG_TAG = DataUploaderAssistant.class.getSimpleName();


    public final static String CREATE_REQUEST_URL = Constants.ROOT_URL + "/api-04/request/add";
    public final static String PICKUP_REQUEST_URL = Constants.ROOT_URL + "/api-04/request/pickup";
    public final static String CLOSE_REQUEST_URL = Constants.ROOT_URL + "/api-04/request/close";
    public final static String VOTE_REQUEST_URL = Constants.ROOT_URL + "/api-04/request/vote";
    public final static String VOTE_ARTICLE_URL = Constants.ROOT_URL + "/api-04/article/vote";


    private Account mAccount;
    private String mAuthToken;
    private ContentProviderClient mProvider;

    public DataUploaderAssistant(Account account, String authToken, ContentProviderClient provider) {
        mAccount = account;
        mAuthToken = authToken;
        mProvider = provider;
    }

    /**
     * This method constructs an appropriate LegacyServerHttpRequest based on the details of UserActionItem
     */
    public LegacyServerHttpRequest createUserActionServerRequest(
            UserActionItem userAction, LegacyServerHttpRequest.OnServerResponseCallback callback,
            Object asyncCompletionToken) {

        LegacyServerHttpRequest serverHttpRequest = new LegacyServerHttpRequest(getUserActionUploadUrl(userAction),
                mAccount, mAuthToken, callback, asyncCompletionToken);

        addUserActionSpecificInfo(serverHttpRequest, userAction);

        return serverHttpRequest;

    }


    /**
     * This method adds headers and fields to LegacyServerHttpRequest based on the information
     * about user's action
     */
    private void addUserActionSpecificInfo(LegacyServerHttpRequest serverHttpRequest,
                                           UserActionItem userAction) {

        String entityType = userAction.mEntityType;
        String actionType = userAction.mActionType;

        switch (entityType) {

            case IDoCareContract.UserActions.ENTITY_TYPE_REQUEST:
                switch (actionType) {

                    case IDoCareContract.UserActions.ACTION_TYPE_CREATE_REQUEST:
                        addCreateRequestSpecificInfo(serverHttpRequest, userAction);
                        break;

                    case IDoCareContract.UserActions.ACTION_TYPE_PICKUP_REQUEST:
                        addPickupRequestSpecificInfo(serverHttpRequest, userAction);
                        break;

                    case IDoCareContract.UserActions.ACTION_TYPE_CLOSE_REQUEST:
                        addCloseRequestSpecificInfo(serverHttpRequest, userAction);
                        break;

                    case IDoCareContract.UserActions.ACTION_TYPE_VOTE:
                        addVoteSpecificInfo(serverHttpRequest, userAction);
                        break;

                    default:
                        throw new IllegalArgumentException("unknown action type '" + actionType
                                + "' for entity '" + entityType + "'");
                }
                break;

            case IDoCareContract.UserActions.ENTITY_TYPE_ARTICLE:
                switch (actionType) {
                    case "DUMMY":
                        // TODO: remove this statement - I added it in order to be able to add "break" below
                        break;
                    case IDoCareContract.UserActions.ACTION_TYPE_VOTE:
                        throw new UnsupportedOperationException("'" + actionType + "' action type" +
                                "is not supported yet!");
                    default:
                        throw new IllegalArgumentException("unknown action type '" + actionType
                                + "' for entity '" + entityType + "'");
                }
                break;

            default:
                throw new IllegalArgumentException("unknown entity type '" + entityType + "'");
        }

    }

    private void addCreateRequestSpecificInfo(LegacyServerHttpRequest serverHttpRequest,
                                              UserActionItem userAction) {
        RequestItem requestItem = null;
        try {
            Cursor cursor = mProvider.query(
                    ContentUris.withAppendedId(IDoCareContract.Requests.CONTENT_URI,
                            userAction.mEntityId),
                    IDoCareContract.Requests.PROJECTION_ALL,
                    null,
                    null,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                requestItem = RequestItem.create(cursor);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            return;
        }
        if (requestItem == null) {
            Log.e(LOG_TAG, "couldn't obtain information about newly created request");
            return;
        }
        serverHttpRequest.addStandardHeaders();
        serverHttpRequest.addTextField(Constants.FIELD_NAME_LATITUDE,
                String.valueOf(requestItem.getLatitude()));
        serverHttpRequest.addTextField(Constants.FIELD_NAME_LONGITUDE,
                String.valueOf(requestItem.getLongitude()));
        serverHttpRequest.addTextField(Constants.FIELD_NAME_CREATED_POLLUTION_LEVEL,
                String.valueOf(requestItem.getCreatedPollutionLevel()));
        serverHttpRequest.addTextField(Constants.FIELD_NAME_CREATED_COMMENT,
                String.valueOf(requestItem.getCreatedComment()));

        if (!TextUtils.isEmpty(requestItem.getCreatedPictures())) {
            String[] createdPicturesUris =
                    requestItem.getCreatedPictures().split(Constants.PICTURES_LIST_SEPARATOR);
            for (int i = 0; i < createdPicturesUris.length; i++) {
                if (!TextUtils.isEmpty(createdPicturesUris[i])) {
                    serverHttpRequest.addPictureField(Constants.FIELD_NAME_CREATED_PICTURES,
                            "picture" + String.valueOf(i), createdPicturesUris[i]);
                }
            }
        }
    }


    private void addVoteSpecificInfo(LegacyServerHttpRequest serverHttpRequest,
                                     UserActionItem userAction) {
        serverHttpRequest.addStandardHeaders();

        if (!TextUtils.isEmpty(userAction.mActionParam)) {
            serverHttpRequest.addTextField(Constants.FIELD_NAME_SCORE, userAction.mActionParam);
        }

        serverHttpRequest.addTextField(Constants.FIELD_NAME_ENTITY_ID,
                String.valueOf(userAction.mEntityId));

        if (!TextUtils.isEmpty(userAction.mEntityParam)) {
            serverHttpRequest.addTextField(Constants.FIELD_NAME_ENTITY_PARAM, userAction.mEntityParam);
        }

    }


    private void addPickupRequestSpecificInfo(LegacyServerHttpRequest serverHttpRequest,
                                              UserActionItem userAction) {
        serverHttpRequest.addStandardHeaders();

        serverHttpRequest.addTextField(Constants.FIELD_NAME_REQUEST_ID,
                String.valueOf(userAction.mEntityId));

    }


    private void addCloseRequestSpecificInfo(LegacyServerHttpRequest serverHttpRequest,
                                             UserActionItem userAction) {
        String closedComment = null;
        String closedPictures = null;

        try {
            JSONObject json = new JSONObject(userAction.mActionParam);
            closedComment = json.getString(Constants.FIELD_NAME_CLOSED_COMMENT);
            closedPictures = json.getString(Constants.FIELD_NAME_CLOSED_PICTURES);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "couldn't parse CLOSED_REQUEST action param as JSON object");
        }

        serverHttpRequest.addStandardHeaders();

        serverHttpRequest.addTextField(Constants.FIELD_NAME_REQUEST_ID,
                String.valueOf(userAction.mEntityId));

        if (!TextUtils.isEmpty(closedComment)) {
            serverHttpRequest.addTextField(Constants.FIELD_NAME_CLOSED_COMMENT, closedComment);
        }

        if (!TextUtils.isEmpty(closedPictures)) {
            String[] closedPicturesUris =
                    closedPictures.split(Constants.PICTURES_LIST_SEPARATOR);
            for (int i = 0; i < closedPicturesUris.length; i++) {
                if (!TextUtils.isEmpty(closedPicturesUris[i])) {
                    serverHttpRequest.addPictureField(Constants.FIELD_NAME_CLOSED_PICTURES,
                            "picture" + String.valueOf(i), closedPicturesUris[i]);
                }
            }
        }


    }


    /**
     * Get URL of the appropriate server API based on the type of user action that needs to be
     * uploaded
     */
    private String getUserActionUploadUrl(UserActionItem userAction) {
        String entityType = userAction.mEntityType;
        String actionType = userAction.mActionType;

        switch (entityType) {

            case IDoCareContract.UserActions.ENTITY_TYPE_REQUEST:
                switch (actionType) {
                    case IDoCareContract.UserActions.ACTION_TYPE_CREATE_REQUEST:
                        return CREATE_REQUEST_URL;
                    case IDoCareContract.UserActions.ACTION_TYPE_PICKUP_REQUEST:
                        return PICKUP_REQUEST_URL;
                    case IDoCareContract.UserActions.ACTION_TYPE_CLOSE_REQUEST:
                        return CLOSE_REQUEST_URL;
                    case IDoCareContract.UserActions.ACTION_TYPE_VOTE:
                        return VOTE_REQUEST_URL;
                    default:
                        throw new IllegalArgumentException("unknown action type '" + actionType
                                + "' for entity '" + entityType + "'");
                }

            case IDoCareContract.UserActions.ENTITY_TYPE_ARTICLE:
                switch (actionType) {
                    case IDoCareContract.UserActions.ACTION_TYPE_VOTE:
                        return VOTE_ARTICLE_URL;
                    default:
                        throw new IllegalArgumentException("unknown action type '" + actionType
                                + "' for entity '" + entityType + "'");
                }

            default:
                throw new IllegalArgumentException("unknown entity type '" + entityType + "'");
        }
    }
}
