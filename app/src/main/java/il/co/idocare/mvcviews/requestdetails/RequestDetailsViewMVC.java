package il.co.idocare.mvcviews.requestdetails;

import il.co.idocare.datamodels.functional.RequestItem;
import il.co.idocare.datamodels.functional.UserItem;
import il.co.idocare.mvcviews.ObservableViewMVC;
import il.co.idocare.mvcviews.ViewMVC;
import il.co.idocare.requests.RequestEntity;

/**
 * This MVC view represents a screen showing request's details
 */
public interface RequestDetailsViewMvc
        extends ObservableViewMVC<RequestDetailsViewMvc.RequestDetailsViewMvcListener> {

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

    void bindCreatedByUser(UserItem user);

    void bindClosedByUser(UserItem user);

    void bindPickedUpByUser(UserItem user);
}
