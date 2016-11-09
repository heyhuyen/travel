package com.codepath.travel;

import android.app.Application;

import com.parse.Parse;
import com.parse.ParseObject;
import com.parse.interceptors.ParseLogInterceptor;

/**
 * Created by aditikakadebansal on 11/8/16.
 */

public class ParseApplication extends Application {

    private static final String APP_ID = "traveltrails";
    private static final String SERVER_URL = "http://traveltrails.herokuapp.com/parse";

    @Override
    public void onCreate() {
        super.onCreate();

        // set applicationId, and server based on the values in the Heroku settings.
        // clientKey is not needed unless explicitly configured
        // any network interceptors must be added with the Configuration Builder given this syntax
        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId(APP_ID) // should correspond to APP_ID env variable
                .clientKey(null)  // set explicitly unless clientKey is explicitly configured on Parse server
                .addNetworkInterceptor(new ParseLogInterceptor())
                .server(SERVER_URL).build());

        // New test creation of object below
        ParseObject testObject = new ParseObject("TestObject");
        testObject.put("foo", "bar");
        testObject.saveInBackground();
    }
}
