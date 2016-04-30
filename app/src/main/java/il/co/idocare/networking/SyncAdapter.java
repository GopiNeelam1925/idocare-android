package il.co.idocare.networking;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;

import il.co.idocare.Constants;
import il.co.idocare.authentication.AccountAuthenticator;
import il.co.idocare.location.OpenStreetMapsReverseGeocoderFactory;
import il.co.idocare.location.ReverseGeocoderFactory;
import il.co.idocare.networking.interfaces.LegacyServerResponseHandlerFactory;
import il.co.idocare.utils.Logger;

/**
 * Our sync adapter
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {


    private static final String LOG_TAG = SyncAdapter.class.getSimpleName();

    /**
     * This constant should be used as a key to a long in syncExtras Bundle object in order
     * to perform partial sync and only get data of one particular user.
     */
    public static final String SYNC_EXTRAS_USER_ID = "SYNC_EXTRAS_USER_ID";


    private final Logger mLogger;


    /**
     * Set up the sync adapter
     */
    public SyncAdapter(@NonNull Context context,
                       boolean autoInitialize,
                       @NonNull Logger logger) {
        super(context, autoInitialize);
        mLogger = logger;
    }


    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {

        mLogger.d(LOG_TAG, "onPerformSync() called; \"sync extras\" bundle:\n" + extras);

        String authToken = getAuthToken(account);

        String userId = AccountManager.get(getContext())
                .getUserData(account, Constants.FIELD_NAME_USER_ID);

        if (!TextUtils.isEmpty(userId) && !TextUtils.isEmpty(authToken)) {
            DataUploader dataUploader =
                    new DataUploader(userId, authToken, provider);

            // This call will block until all local actions will be synchronized to the server
            // and the respective ContentProvider will be updated
            dataUploader.uploadAll();
        } else {
            mLogger.d(LOG_TAG, "no user ID or auth token - skipping data upload and using an open API."
                    + "\nUser ID: " + userId + "\nAuth token: " + authToken);
        }


        ReverseGeocoderFactory reverseGeocoderFactory =
                new OpenStreetMapsReverseGeocoderFactory();

        LegacyServerResponseHandlerFactory serverResponseHandlerFactory =
                new LegacySimpleServerResponseHandlerFactory(reverseGeocoderFactory);

        DataDownloader dataDownloader =
                new DataDownloader(userId, authToken, provider, serverResponseHandlerFactory);


        if (extras.containsKey(SYNC_EXTRAS_USER_ID)) {
            // perform partial sync of users' data
            dataDownloader.downloadUserData(extras.getLong(SYNC_EXTRAS_USER_ID));

        } else {
            // This call will block until all relevant data will be synchronized from the server
            // and the respective ContentProvider will be updated
            dataDownloader.downloadAll();
        }


        Log.d(LOG_TAG, "onPerformSync() returned");

    }


    private String getAuthToken(Account account) {
        String authToken = null;
        try {
            authToken = AccountManager.get(this.getContext()).blockingGetAuthToken(
                    account,
                    AccountAuthenticator.AUTH_TOKEN_TYPE_DEFAULT,
                    true);
        } catch (OperationCanceledException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (AuthenticatorException e) {
            e.printStackTrace();
        }

        return authToken;
    }

}
