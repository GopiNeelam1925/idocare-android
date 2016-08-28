package il.co.idocare.managers;

import android.support.annotation.NonNull;

import il.co.idocare.authentication.LoginStateManager;
import il.co.idocare.contentproviders.IDoCareContract;
import il.co.idocare.entities.UserActionEntity;
import il.co.idocare.entities.cachers.UserActionCacher;
import il.co.idocare.multithreading.BackgroundThreadPoster;
import il.co.idocare.networking.ServerSyncController;
import il.co.idocare.utils.Logger;

/**
 * This manager encapsulates functionality related to "vote" action
 */
public class VoteManager {

    private static final String TAG = "VoteManager";

    public static final int VOTE_UP_CREATED = 1;
    public static final int VOTE_DOWN_CREATED = 2;
    public static final int VOTE_UP_CLOSED = 3;
    public static final int VOTE_DOWN_CLOSED = 4;

    private final BackgroundThreadPoster mBackgroundThreadPoster;
    private final LoginStateManager mLoginStateManager;
    private final UserActionCacher mUserActionCacher;
    private final Logger mLogger;
    private final ServerSyncController mServerSyncController;

    public VoteManager(@NonNull BackgroundThreadPoster backgroundThreadPoster,
                       @NonNull LoginStateManager loginStateManager,
                       @NonNull UserActionCacher userActionCacher,
                       @NonNull Logger logger,
                       @NonNull ServerSyncController serverSyncController) {
        mBackgroundThreadPoster = backgroundThreadPoster;
        mLoginStateManager = loginStateManager;
        mUserActionCacher = userActionCacher;
        mLogger = logger;
        mServerSyncController = serverSyncController;
    }

    /**
     * Vote for request
     * @param requestId ID of the request to vote for
     * @param voteType either one of: {@link #VOTE_UP_CREATED}, {@link #VOTE_DOWN_CREATED},
     *                 {@link #VOTE_UP_CLOSED}, {@link #VOTE_DOWN_CLOSED}
     */
    public void voteForRequest(final long requestId, final int voteType) {

        mLogger.d(TAG, "voteForRequest(); request ID: " + requestId + "; vote type: " + voteType);

        final String actionType = IDoCareContract.UserActions.ACTION_TYPE_VOTE_FOR_REQUEST;
        final String actionParam;
        final String entityParam;

        switch (voteType) {
            case VOTE_UP_CREATED:
                entityParam = IDoCareContract.UserActions.ENTITY_PARAM_REQUEST_CREATED;
                actionParam = "1";
                break;
            case VOTE_DOWN_CREATED:
                entityParam = IDoCareContract.UserActions.ENTITY_PARAM_REQUEST_CREATED;
                actionParam = "-1";
                break;
            case VOTE_UP_CLOSED:
                entityParam = IDoCareContract.UserActions.ENTITY_PARAM_REQUEST_CLOSED;
                actionParam = "1";
                break;
            case VOTE_DOWN_CLOSED:
                entityParam = IDoCareContract.UserActions.ENTITY_PARAM_REQUEST_CLOSED;
                actionParam = "-1";
                break;
            default:
                throw new IllegalArgumentException("vote type must be either VOTE_UP_CREATED or VOTE_DOWN_CREATED");
        }

        final String activeUserId = mLoginStateManager.getActiveAccountUserId();

        if (activeUserId == null || activeUserId.isEmpty()) {
            mLogger.e(TAG, "no logged in user - vote ignored");
            return;
        }

        final UserActionEntity userActionEntity = new UserActionEntity(
                System.currentTimeMillis(),
                IDoCareContract.UserActions.ENTITY_TYPE_REQUEST,
                requestId,
                entityParam,
                actionType,
                actionParam
        );

        mBackgroundThreadPoster.post(new Runnable() {
            @Override
            public void run() {
                if (mUserActionCacher.cacheUserAction(userActionEntity)) {
                    mServerSyncController.syncUserDataImmediate(activeUserId);
                }
            }
        });
    }
}
