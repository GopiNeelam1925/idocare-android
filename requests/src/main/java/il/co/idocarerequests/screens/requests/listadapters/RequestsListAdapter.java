package il.co.idocarerequests.screens.requests.listadapters;

import android.content.Context;
import androidx.annotation.NonNull;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.Collections;
import java.util.List;

import il.co.idocarecore.requests.RequestEntity;
import il.co.idocarerequests.screens.requests.mvcviews.RequestPreviewViewMvcImpl;
import il.co.idocarecore.users.UserEntity;
import il.co.idocarecore.users.UsersManager;

/**
 * This adapter should be used in order to populate ListView with {@link RequestEntity} objects
 */
public class RequestsListAdapter extends ArrayAdapter<RequestEntity> {


    private Context mContext;
    private final UsersManager mUsersManager;

    public RequestsListAdapter(Context context, int resource, UsersManager usersManager) {
        super(context, resource);
        mContext = context;
        mUsersManager = usersManager;
    }

    public void bindRequests(List<RequestEntity> requests) {
        clear();
        addAll(requests);
        notifyDataSetChanged();
    }


    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final RequestPreviewViewMvcImpl requestPreviewViewMvc;
        if (convertView == null) {
            requestPreviewViewMvc =
                    new RequestPreviewViewMvcImpl(LayoutInflater.from(mContext), parent);
            convertView = requestPreviewViewMvc.getRootView();
            convertView.setTag(requestPreviewViewMvc);
        } else {
            requestPreviewViewMvc = (RequestPreviewViewMvcImpl) convertView.getTag();
        }

        RequestEntity request = getItem(position);

        requestPreviewViewMvc.bindRequest(request);

        final String createdByUserId = request.getCreatedBy();

        final View finalConvertView = convertView;

        finalConvertView.setHasTransientState(true);

        mUsersManager.fetchUsersByIdAndNotify(
                Collections.singletonList(createdByUserId),
                new UsersManager.UsersManagerListener() {
                    @Override
                    public void onUsersFetched(List<UserEntity> users) {
                        if (users.size() > 0) {
                            requestPreviewViewMvc.bindCreatedByUser(users.get(0));
                        }
                        finalConvertView.setHasTransientState(false);
                    }
                });

        return convertView;
    }

}
