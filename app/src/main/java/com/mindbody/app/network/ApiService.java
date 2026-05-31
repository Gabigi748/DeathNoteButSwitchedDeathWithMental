package com.mindbody.app.network;

import com.mindbody.app.model.AiAnalysis;
import com.mindbody.app.model.CheckinRequest;
import com.mindbody.app.model.User;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    // Auth
    @POST("auth/register")
    Call<Map<String, Object>> register(@Body Map<String, Object> body);

    @POST("auth/login")
    Call<Map<String, Object>> login(@Body Map<String, Object> body);

    @GET("auth/profile")
    Call<User> getProfile();

    // Checkin
    @POST("checkin")
    Call<Map<String, Object>> submitCheckin(@Body CheckinRequest request);

    @GET("checkin/history")
    Call<List<Map<String, Object>>> getCheckinHistory(@Query("days") int days);

    @GET("checkin/streak")
    Call<Map<String, Object>> getStreak();

    // Questionnaire
    @POST("questionnaire")
    Call<Map<String, Object>> submitQuestionnaire(@Body Map<String, Object> body);

    @GET("questionnaire/history")
    Call<List<Map<String, Object>>> getQuestionnaireHistory();

    @GET("questionnaire/latest")
    Call<Map<String, Object>> getLatestQuestionnaire();

    // AI
    @GET("ai/latest")
    Call<AiAnalysis> getLatestAi();

    @GET("ai/analysis")
    Call<AiAnalysis> getAiAnalysis(@Query("date") String date);

    // Stats
    @GET("stats/mood")
    Call<Map<String, Object>> getMoodStats(@Query("days") int days);

    @GET("stats/symptoms")
    Call<Map<String, Object>> getSymptomStats(@Query("days") int days);

    @GET("stats/correlation")
    Call<Map<String, Object>> getCorrelation();
}
