package il.co.idocarerequests.screens.requestdetails.mvcviews;

import il.co.idocarecore.requests.RequestEntity;
import il.co.idocarecore.screens.common.mvcviews.ObservableViewMvc;
import il.co.idocarecore.users.UserEntity;

/**
 * This MVC view represents a screen showing request's details
 */
public interface RequestDetailsViewMvc
        extends ObservableViewMvc<RequestDetailsViewMvc.RequestDetailsViewMvcListener> {

    interface RequestDetailsViewMvcListener {
        void onCloseRequestClicked();
        void onPickupRequestClicked();
        void onClosedVoteUpClicked();
        void onClosedVoteDownClicked();
        void onCreatedVoteUpClicked();
        void onCreatedVoteDownClicked();
    }


    void bindRequest(RequestEntity request);

    void bindCurrentUserId(String currentUserId);

    void bindCreatedByUser(UserEntity user);

    void bindClosedByUser(UserEntity user);

    void bindPickedUpByUser(UserEntity user);
}
