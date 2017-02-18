package il.co.idocare.networking.newimplementation;

import java.util.Map;

import il.co.idocare.networking.newimplementation.schemes.responses.GetUsersInfoResponseScheme;
import il.co.idocare.networking.newimplementation.schemes.responses.RequestsResponseScheme;
import il.co.idocare.networking.newimplementation.schemes.responses.LoginNativeResponseScheme;
import il.co.idocare.networking.newimplementation.schemes.responses.RequestResponseScheme;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PartMap;

/**
 * This interface contains definitions of endpoints consumed by Retrofit
 */
public interface ServerApi {


    // ---------------------------------------------------------------------------------------------
    // Authentication

    // this call handles both native and FB signups (they differ by request fields)
    @POST("user/add")
    Call<LoginNativeResponseScheme> signup(@Body MultipartBody body);

    @FormUrlEncoded
    @POST("user/login")
    Call<LoginNativeResponseScheme> loginNative(
            @Header("Idc-user-data") String username,
            @Field("user_data_auth") String password
    );



    // ---------------------------------------------------------------------------------------------
    // Users


    @FormUrlEncoded
    @POST("user/get")
    Call<GetUsersInfoResponseScheme> getUsersInfo(@FieldMap Map<String, String> userIds);


    // ---------------------------------------------------------------------------------------------
    // Requests

    @POST("request")
    Call<RequestsResponseScheme> getRequests();

    @POST("request/add")
    Call<RequestResponseScheme> createNewRequest(@Body MultipartBody body);

    @FormUrlEncoded
    @POST("request/pickup")
    Call<Void> pickupRequest(@Field("request_id") String requestId);

    @POST("request/close")
    Call<Void> closeRequest(@Body MultipartBody part);

    @FormUrlEncoded
    @POST("request/vote")
    Call<Void> voteForRequest(
            @Field("entity_id") String requestId,
            @Field("score") int score,
            @Field("entity_param") String createdOrClosed
    );

}
