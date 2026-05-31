package com.mindbody.app.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefManager {

    private static final String PREF_NAME = "mindbody_prefs";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_NICKNAME = "nickname";
    private static final String KEY_EMAIL = "email";

    private final SharedPreferences prefs;

    public SharedPrefManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public void saveNickname(String nickname) {
        prefs.edit().putString(KEY_NICKNAME, nickname).apply();
    }

    public String getNickname() {
        return prefs.getString(KEY_NICKNAME, "");
    }

    public void saveEmail(String email) {
        prefs.edit().putString(KEY_EMAIL, email).apply();
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public boolean isLoggedIn() {
        return getToken() != null && !getToken().isEmpty();
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
