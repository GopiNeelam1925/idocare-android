package il.co.idocare.useractions.entities;

import android.support.annotation.NonNull;

import il.co.idocare.contentproviders.IDoCareContract;

/**
 * This entity encapsulates info about user's pick up action
 */

public class PickUpRequestUserActionEntity extends UserActionEntity {

    public PickUpRequestUserActionEntity(long timestamp,
                                         @NonNull String requestId,
                                         @NonNull String pickedUpByUserId) {

        super(timestamp,
                IDoCareContract.UserActions.ENTITY_TYPE_REQUEST,
                requestId,
                null,
                IDoCareContract.UserActions.ACTION_TYPE_PICKUP_REQUEST,
                pickedUpByUserId);
    }

    public String getPickedUpByUserId() {
        return getActionParam();
    }

}
