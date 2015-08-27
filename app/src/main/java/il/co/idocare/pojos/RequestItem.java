package il.co.idocare.pojos;


import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.Locale;

import il.co.idocare.Constants;
import il.co.idocare.contentproviders.IDoCareContract.Requests;
import il.co.idocare.utils.UtilMethods;

/**
 * This object contains data about a single request (users involved, state, location, etc)
 */
public class RequestItem {


    private final static String LOG_TAG = "RequestItem";


    public enum RequestStatus {NEW_BY_OTHER, NEW_BY_ME, PICKED_UP_BY_OTHER, PICKED_UP_BY_ME,
        CLOSED_BY_OTHER, CLOSED_BY_ME, UNKNOWN}

    /**
     * This String array specifies the fields that are mandatory for RequestItem object in
     * order to be useful. This array can be used as is in queries against ContentProvider.
     */
    public static String[] MANDATORY_REQUEST_FIELDS = new String[] {
            Constants.FIELD_NAME_REQUEST_ID,
            Constants.FIELD_NAME_CREATED_BY,
            Constants.FIELD_NAME_CREATED_AT,
            Constants.FIELD_NAME_CREATED_COMMENT,
            Constants.FIELD_NAME_CREATED_PICTURES,
            Constants.FIELD_NAME_CREATED_REPUTATION,
            Constants.FIELD_NAME_CREATED_POLLUTION_LEVEL,
            Constants.FIELD_NAME_PICKED_UP_BY,
            Constants.FIELD_NAME_CLOSED_BY,
            Constants.FIELD_NAME_LONGITUDE,
            Constants.FIELD_NAME_LATITUDE,
    };


    @SerializedName(Constants.FIELD_NAME_REQUEST_ID)
    private long mId = 0;
    @SerializedName(Constants.FIELD_NAME_CREATED_BY)
    private long mCreatedBy = 0;
    @SerializedName(Constants.FIELD_NAME_CREATED_AT)
    private String mCreatedAt = null;
    @SerializedName(Constants.FIELD_NAME_CREATED_COMMENT)
    private String mCreatedComment = null;
    @SerializedName(Constants.FIELD_NAME_CREATED_PICTURES)
    private String mCreatedPictures = null;
    @SerializedName(Constants.FIELD_NAME_CREATED_REPUTATION)
    private int mCreatedReputation = 0;
    @SerializedName(Constants.FIELD_NAME_LATITUDE)
    private double mLat = 0;
    @SerializedName(Constants.FIELD_NAME_LONGITUDE)
    private double mLong = 0;
    @SerializedName(Constants.FIELD_NAME_CREATED_POLLUTION_LEVEL)
    private int mCreatedPollutionLevel = 0;
    @SerializedName(Constants.FIELD_NAME_PICKED_UP_BY)
    private long mPickedUpBy = 0;
    @SerializedName(Constants.FIELD_NAME_PICKED_UP_AT)
    private String mPickedUpAt = null;
    @SerializedName(Constants.FIELD_NAME_CLOSED_BY)
    private long mClosedBy = 0;
    @SerializedName(Constants.FIELD_NAME_CLOSED_AT)
    private String mClosedAt = null;
    @SerializedName(Constants.FIELD_NAME_CLOSED_COMMENT)
    private String mClosedComment = null;
    @SerializedName(Constants.FIELD_NAME_CLOSED_PICTURES)
    private String mClosedPictures = null;
    @SerializedName(Constants.FIELD_NAME_CLOSED_REPUTATION)
    private int mClosedReputation = 0;

    private String mLocation = null;
    private RequestStatus mStatus = RequestStatus.UNKNOWN;


    private RequestItem(long id) {
        mId = id;
    }

    /**
     * Create RequestItem object by querying the cursor at the current position.
     * @param cursor the Cursor to be queried
     * @return newly created RequestItem object
     * @throws IllegalArgumentException if any of the mandatory fields (as specified by
     * {@link RequestItem#MANDATORY_REQUEST_FIELDS}) are missing from the cursor
     */
    public static RequestItem create(Cursor cursor)
            throws IllegalArgumentException {
        RequestItem request = null;

        // Mandatory fields
        try {
            long requestId = cursor.getLong(cursor.getColumnIndexOrThrow(Constants.FIELD_NAME_REQUEST_ID));
            long createdBy = cursor.getLong(cursor.getColumnIndexOrThrow(Constants.FIELD_NAME_CREATED_BY));
            String createdAt = cursor.getString(cursor.getColumnIndexOrThrow(Constants.FIELD_NAME_CREATED_AT));
            String createdComment = cursor.getString(cursor.getColumnIndexOrThrow(Constants.FIELD_NAME_CREATED_COMMENT));
            String createdPictures = cursor.getString(cursor.getColumnIndexOrThrow(Constants.FIELD_NAME_CREATED_PICTURES));
            int createdReputation = cursor.getInt(cursor.getColumnIndexOrThrow(Constants.FIELD_NAME_CREATED_REPUTATION));
            int createdPollutionLevel = cursor.getInt(cursor.getColumnIndexOrThrow(Constants.FIELD_NAME_CREATED_POLLUTION_LEVEL));
            long pickedUpBy = cursor.getLong(cursor.getColumnIndexOrThrow(Constants.FIELD_NAME_PICKED_UP_BY));
            long closedBy = cursor.getLong(cursor.getColumnIndexOrThrow(Constants.FIELD_NAME_CLOSED_BY));
            double latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(Constants.FIELD_NAME_LATITUDE));
            double longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(Constants.FIELD_NAME_LONGITUDE));


            request = new RequestItem(requestId);
            request.setCreatedBy(createdBy);
            request.setCreatedAt(createdAt);
            request.setCreatedComment(createdComment);
            request.setCreatedPictures(createdPictures);
            request.setCreatedReputation(createdReputation);
            request.setCreatedPollutionLevel(createdPollutionLevel);
            request.setPickedUpBy(pickedUpBy);
            request.setClosedBy(closedBy);
            request.setLatitude(latitude);
            request.setLongitude(longitude);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Couldn't create a new RequestItem: one or more " +
                    "of the mandatory fields missing from the cursor", e);
        }

        // Non-mandatory fields

        int i;

        if ((i = cursor.getColumnIndex(Constants.FIELD_NAME_PICKED_UP_AT)) != -1) {
            request.setPickedUpAt(cursor.getString(i));
        }
        if ((i = cursor.getColumnIndex(Constants.FIELD_NAME_CLOSED_AT)) != -1) {
            request.setClosedAt(cursor.getString(i));
        }
        if ((i = cursor.getColumnIndex(Constants.FIELD_NAME_CLOSED_COMMENT)) != -1) {
            request.setClosedComment(cursor.getString(i));
        }
        if ((i = cursor.getColumnIndex(Constants.FIELD_NAME_CLOSED_PICTURES)) != -1) {
            request.setClosedPictures(cursor.getString(i));
        }
        if ((i = cursor.getColumnIndex(Constants.FIELD_NAME_CLOSED_REPUTATION)) != -1) {
            request.setClosedReputation(cursor.getInt(i));
        }
        if ((i = cursor.getColumnIndex(Constants.FIELD_NAME_LOCATION)) != -1) {
            request.setLocation(cursor.getString(i));
        }

        request.formatDates();

        return request;

    }

    /**
     * Create RequestItem from a string formatted as JSON object
     * @param jsonObjectString a string formatted as JSON object having request's data
     */
    public static RequestItem create(String jsonObjectString) {
        Gson gson = new Gson();
        RequestItem request = gson.fromJson(jsonObjectString, RequestItem.class);

        request.formatDates();

        return request;
    }

    /**
     * Create a copy of the existing RequestItem
     */
    public static RequestItem create (RequestItem other) {
        RequestItem newRequest = new RequestItem(other.getId());
        newRequest.setCreatedBy(other.getCreatedBy())
                .setCreatedAt(other.getCreatedAt())
                .setCreatedComment(other.getCreatedComment())
                .setCreatedPictures(other.getCreatedPictures())
                .setCreatedReputation(other.getCreatedReputation())
                .setLatitude(other.getLatitude())
                .setLongitude(other.getLongitude())
                .setCreatedPollutionLevel(other.getCreatedPollutionLevel())
                .setPickedUpBy(other.getPickedUpBy())
                .setPickedUpAt(other.getPickedUpAt())
                .setClosedBy(other.getClosedBy())
                .setClosedAt(other.getClosedAt())
                .setClosedComment(other.getClosedComment())
                .setClosedPictures(other.getClosedPictures())
                .setClosedReputation(other.getClosedReputation())
                .setLocation(other.getLocation())
                .setStatus(other.getStatus());

        return newRequest;
    }

    /*
    TODO: why is this method here? Why formatting dates at all? If formatting - not here!
     */
    private void formatDates() {
        if (!TextUtils.isEmpty(getCreatedAt()))
            setCreatedAt(UtilMethods.formatDate(getCreatedAt()));
        if (!TextUtils.isEmpty(getPickedUpAt()))
            setPickedUpAt(UtilMethods.formatDate(getPickedUpAt()));
        if (!TextUtils.isEmpty(getClosedAt()))
            setClosedAt(UtilMethods.formatDate(getClosedAt()));
    }


    /**
     * This method will set a correct status for this request based on the ID of the currently
     * active user.
     * @param activeUserId
     */
    public void setStatus(long activeUserId) {

        if (getClosedBy() != 0) {
            if (activeUserId == getClosedBy())
                setStatus(RequestStatus.CLOSED_BY_ME);
            else
                setStatus(RequestStatus.CLOSED_BY_OTHER);
        } else if (getPickedUpBy() != 0) {
            if (activeUserId == getPickedUpBy())
                setStatus(RequestStatus.PICKED_UP_BY_ME);
            else
                setStatus(RequestStatus.PICKED_UP_BY_OTHER);
        } else if (getCreatedBy() != 0) {
            if (activeUserId == getCreatedBy())
                setStatus(RequestStatus.NEW_BY_ME);
            else
                setStatus(RequestStatus.NEW_BY_OTHER);
        } else {
            Log.e(LOG_TAG, "Could not set request status! Supplied user ID: " + activeUserId +
                    "\nFields values:" +
                    "\nCreated by: " + getCreatedBy() +
                    "\nPicked up by: " + getPickedUpBy() +
                    "\nClosed by: " + getClosedBy());
        }
    }


    /**
     * Convert this request object to ContentValues object that can be passed to ContentProvider
     */
    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();

        values.put(Requests.COL_REQUEST_ID, getId());
        values.put(Requests.COL_CREATED_BY, getCreatedBy());
        values.put(Requests.COL_CREATED_AT, getCreatedAt());
        values.put(Requests.COL_CREATED_COMMENT, getCreatedComment());
        values.put(Requests.COL_CREATED_PICTURES, getCreatedPictures());
        values.put(Requests.COL_CREATED_REPUTATION, getCreatedReputation());
        values.put(Requests.COL_LATITUDE, getLatitude());
        values.put(Requests.COL_LONGITUDE, getLongitude());
        values.put(Requests.COL_POLLUTION_LEVEL, getCreatedPollutionLevel());
        values.put(Requests.COL_PICKED_UP_BY, getPickedUpBy());
        values.put(Requests.COL_PICKED_UP_AT, getPickedUpAt());
        values.put(Requests.COL_CLOSED_BY, getClosedBy());
        values.put(Requests.COL_CLOSED_AT, getClosedAt());
        values.put(Requests.COL_CLOSED_COMMENT, getClosedComment());
        values.put(Requests.COL_CLOSED_PICTURES, getClosedPictures());
        values.put(Requests.COL_CLOSED_REPUTATION, getClosedReputation());
        values.put(Requests.COL_LOCATION, getLocation());

        return values;
    }




    // ---------------------------------------------------------------------------------------------
    //
    // Setters

    public RequestItem setCreatedBy(long user) {
        mCreatedBy = user;
        return this;
    }


    public RequestItem setCreatedAt(String date) {
        mCreatedAt = date;
        return this;
    }

    public RequestItem setLatitude(double latitude) {
        mLat = latitude;
        return  this;
    }

    public RequestItem setLongitude(double longitude) {
        mLong = longitude;
        return this;
    }


    public RequestItem setCreatedPollutionLevel(int pollutionLevel) {
        mCreatedPollutionLevel = pollutionLevel;
        return this;
    }


    public RequestItem setCreatedComment(String comment) {
        mCreatedComment = comment;
        return this;
    }


    public RequestItem setCreatedPictures(String pictures) {
        mCreatedPictures = pictures;
        return this;
    }

    public RequestItem setCreatedReputation(int reputation) {
        mCreatedReputation = reputation;
        return this;
    }

    public RequestItem setPickedUpBy(long user) {
        mPickedUpBy = user;
        return this;
    }

    public RequestItem setPickedUpAt(String date) {
        mPickedUpAt = date;
        return this;
    }


    public RequestItem setClosedBy(long user) {
        mClosedBy = user;
        return this;
    }

    public RequestItem setClosedAt(String date) {
        mClosedAt = date;
        return this;
    }


    public RequestItem setClosedComment(String comment) {
        mClosedComment = comment;
        return this;
    }


    public RequestItem setClosedPictures(String pictures) {
        mClosedPictures = pictures;
        return this;
    }

    public RequestItem setClosedReputation(int reputation) {
        mClosedReputation = reputation;
        return this;
    }

    public RequestItem setLocation(String location) {
        mLocation = location;
        return this;
    }

    private RequestItem setStatus(RequestStatus status) {
        mStatus = status;
        return this;
    }


    // ---------------------------------------------------------------------------------------------
    //
    // Getters


    public long getId() {
        return mId;
    }

    public long getCreatedBy() {
        return mCreatedBy;
    }

    public String getCreatedAt() {
        return mCreatedAt;
    }

    public String getCreatedComment() {
        return mCreatedComment;
    }

    public String getCreatedPictures() {
        return mCreatedPictures;
    }

    public int getCreatedReputation() {
        return mCreatedReputation;
    }

    public double getLatitude() {
        return mLat;
    }

    public double getLongitude() {
        return mLong;
    }

    public int getCreatedPollutionLevel() {
        return mCreatedPollutionLevel;
    }

    public long getPickedUpBy() {
        return mPickedUpBy;
    }

    public String getPickedUpAt() {
        return mPickedUpAt;
    }

    public long getClosedBy() {
        return mClosedBy;
    }

    public String getClosedAt() {
        return mClosedAt;
    }

    public String getClosedComment() {
        return mClosedComment;
    }

    public String getClosedPictures() {
        return mClosedPictures;
    }

    public int getClosedReputation() {
        return mClosedReputation;
    }

    public String getLocation() {
        return mLocation;
    }

    public RequestItem.RequestStatus getStatus() {
        return mStatus;
    }



}
