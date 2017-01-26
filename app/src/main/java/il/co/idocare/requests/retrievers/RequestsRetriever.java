package il.co.idocare.requests.retrievers;

import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import il.co.idocare.requests.RequestEntity;
import il.co.idocare.useractions.entities.UserActionEntity;
import il.co.idocare.useractions.retrievers.UserActionsRetriever;
import il.co.idocare.requests.RequestsByIdComparator;
import il.co.idocare.useractions.UserActionsToRequestsApplier;

/**
 * This class ca be user in order to retrieve information about requests. In contrast to
 * {@link RawRequestRetriever}, information returned by this class reflects the state of requests
 * after potential local modifications by the user were accounted for.
 */

public class RequestsRetriever {

    private RawRequestRetriever mRawRequestsRetriever;
    private UserActionsRetriever mUserActionsRetriever;
    private UserActionsToRequestsApplier mUserActionsToRequestsApplier;

    private RequestsByIdComparator mComparator = new RequestsByIdComparator();

    public RequestsRetriever(@NonNull RawRequestRetriever rawRequestsRetriever,
                             @NonNull UserActionsRetriever userActionsRetriever,
                             @NonNull UserActionsToRequestsApplier userActionsToRequestsApplier) {
        mRawRequestsRetriever = rawRequestsRetriever;
        mUserActionsRetriever = userActionsRetriever;
        mUserActionsToRequestsApplier = userActionsToRequestsApplier;
    }

    public List<RequestEntity> getAllRequests() {
        List<RequestEntity> rawRequests = mRawRequestsRetriever.getAllRequests();
        List<UserActionEntity> userActions = mUserActionsRetriever.getAllUserActions();

        return applyUserActionsToRequests(rawRequests, userActions);
    }

    /**
     * Get information about requests assigned to the user.
     * @param userId ID of the user
     * @return a list of requests assigned to the user
     */
    @WorkerThread
    public @NonNull List<RequestEntity> getRequestsAssignedToUser(@NonNull String userId) {
        List<RequestEntity> rawRequests = mRawRequestsRetriever.getRequestsAssignedToUser(userId);
        List<UserActionEntity> userActions = mUserActionsRetriever.getAllUserActions();

        return applyUserActionsToRequests(rawRequests, userActions);
    }

    private List<RequestEntity> applyUserActionsToRequests(@NonNull List<RequestEntity> requests,
                                                           @NonNull List<UserActionEntity> userActions) {

        if (userActions.isEmpty() || requests.isEmpty()) return requests;


        Collections.sort(requests, mComparator); // sort in order to be able to use binary search

        List<RequestEntity> updatedRequests = new ArrayList<>(requests);

        RequestEntity updatedRequest;

        for (UserActionEntity userAction : userActions) {
            if (mUserActionsToRequestsApplier.isUserActionAffectingRequest(userAction)) {

                // find the request affected by user action, and if it is found - apply the action to it
                int affectedEntityPos =
                        Collections.binarySearch(
                                requests,
                                RequestEntity.getBuilder().setId(userAction.getEntityId()).build(),
                                mComparator);

                if (affectedEntityPos >= 0) {
                    updatedRequest = mUserActionsToRequestsApplier.applyUserActionToRequest(
                            userAction, requests.get(affectedEntityPos));
                    updatedRequests.set(affectedEntityPos, updatedRequest);
                }

            }
        }

        return updatedRequests;

    }
}
