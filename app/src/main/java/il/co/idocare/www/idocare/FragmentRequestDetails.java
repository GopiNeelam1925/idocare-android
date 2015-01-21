package il.co.idocare.www.idocare;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;


public class FragmentRequestDetails extends Fragment implements HttpTaskExecutor.HttpTaskDoneCallback {

    private final static String LOG_TAG = "FragmentRequestDetails";

    private final static String JSON_TAG_URIS = "filelist";

    private RequestPicturesAdapter mListAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_request_details, container, false);

        mListAdapter = new RequestPicturesAdapter(getActivity(), 0);
        ListView listPictures = (ListView) view.findViewById(R.id.list_request_pictures);
        listPictures.setAdapter(mListAdapter);

        if (savedInstanceState == null) {
            HashMap<String, String> paramMap = new HashMap<String, String>(2);
            paramMap.put("username", Constants.USERNAME);
            paramMap.put("password", Constants.PASSWORD);
            Main.sHttpTaskExecutor.executePost(Constants.HttpTaskTag.REQUEST_DETAILS, this, Constants.IMGLIST_URI, paramMap);
        }

        return view;
    }

    @Override
    public void httpTaskDone(Constants.HttpTaskTag tag, String responseData) {
      if (tag == Constants.HttpTaskTag.REQUEST_DETAILS) {
          List<String> uris = extractUrisFromJSON(responseData);
          if (uris != null) {
              mListAdapter.clear();
              mListAdapter.addAll(uris);
              mListAdapter.notifyDataSetChanged();
          } else {
              Log.e(LOG_TAG, "list of URIs is null");
          }
      } else {
          Log.e(LOG_TAG, "httpTaskDone was called with unrecognized tag: " + tag.toString());
      }
    }


    private List<String> extractUrisFromJSON(String jsonData) {

        ArrayList<String> urisList = null;

        if (jsonData != null) {
            try {
                JSONObject jsonObj = new JSONObject(jsonData);

                // Getting JSON Array
                JSONArray uris = jsonObj.getJSONArray(JSON_TAG_URIS);

                urisList = new ArrayList<String>(uris.length());

                // Adding uris to the list
                for (int i = 0; i < uris.length(); i++) {
                    urisList.add(uris.getString(i));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            Log.e(LOG_TAG,  "Couldn't get any data from the url");
        }

        return urisList;
    }

    private static class ViewHolder {
        ImageView imageView;
    }

    private class RequestPicturesAdapter extends ArrayAdapter<String> {

        private LayoutInflater mInflater;
        private ImageLoadingListener animateFirstListener = new AnimateFirstDisplayListener();

        public RequestPicturesAdapter(Context context, int resource) {
            super(context, resource);
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view = convertView;
            final ViewHolder holder;
            if (convertView == null) {
                view = mInflater.inflate(R.layout.request_images_list_item, parent, false);
                holder = new ViewHolder();
                holder.imageView = (ImageView) view.findViewById(R.id.image);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            ImageLoader.getInstance().displayImage(getItem(position), holder.imageView, animateFirstListener);

            return view;
        }
    }

    private static class AnimateFirstDisplayListener extends SimpleImageLoadingListener {

        static final List<String> displayedImages = Collections.synchronizedList(new ArrayList<String>());

        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            if (loadedImage != null) {
                ImageView imageView = (ImageView) view;
                boolean firstDisplay = !displayedImages.contains(imageUri);
                if (firstDisplay) {
                    FadeInBitmapDisplayer.animate(imageView, 500);
                    displayedImages.add(imageUri);
                }
            }
        }
    }


}
