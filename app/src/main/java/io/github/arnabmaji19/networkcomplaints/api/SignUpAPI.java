package io.github.arnabmaji19.networkcomplaints.api;

import android.util.Log;

import com.google.gson.Gson;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;
import io.github.arnabmaji19.networkcomplaints.model.User;

public class SignUpAPI extends API {

    private static final String TAG = "SignUpAPI";
    private static final String RELATIVE_URL = "android/signup";

    //status codes
    public static final int STATUS_CODE_SUCCESSFUL = 200;
    public static final int STATUS_CODE_EMAIL_ALREADY_REGISTERED = 409;
    public static final int STATUS_CODE_FAILED = 500;


    private RequestParams params;
    private String url;
    private OnCompleteListener onCompleteListener;

    public SignUpAPI(User user) {
        Gson gson = new Gson();
        //create request params
        this.params = new RequestParams("user", gson.toJson(user)); //add details to be included in api
        this.url = getUrl(RELATIVE_URL);
    }


    public void addOnCompleteListener(OnCompleteListener onCompleteListener) {
        this.onCompleteListener = onCompleteListener;
    }

    @Override
    public void post() {
        Log.d(TAG, "post: " + this.url);
        //send post request via api
        client.post(this.url, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                Log.d(TAG, "onSuccess: " + response);
                if (onCompleteListener != null)
                    onCompleteListener.onComplete(statusCode);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                Log.d(TAG, "onFailure: " + statusCode);
                if (onCompleteListener != null)
                    onCompleteListener.onComplete(statusCode);
            }
        });
    }

    public interface OnCompleteListener {
        void onComplete(int statusCode);
    }
}
