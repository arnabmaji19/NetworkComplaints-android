package io.github.arnabmaji19.networkcomplaints.api;

import com.loopj.android.http.AsyncHttpClient;

//Base class for creating other APIs
public abstract class API {

    private static final String BASE_URL = "http://34.203.204.120:3000/";
    protected static final AsyncHttpClient client = new AsyncHttpClient();


    public abstract void post();

    protected final String getUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }
}
