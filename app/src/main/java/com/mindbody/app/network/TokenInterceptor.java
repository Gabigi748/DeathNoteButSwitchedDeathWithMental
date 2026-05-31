package com.mindbody.app.network;

import android.content.Context;

import com.mindbody.app.util.SharedPrefManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class TokenInterceptor implements Interceptor {

    private final SharedPrefManager prefManager;

    public TokenInterceptor(Context context) {
        this.prefManager = new SharedPrefManager(context);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        String token = prefManager.getToken();

        if (token == null || token.isEmpty()) {
            return chain.proceed(originalRequest);
        }

        Request newRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer " + token)
                .build();

        return chain.proceed(newRequest);
    }
}
