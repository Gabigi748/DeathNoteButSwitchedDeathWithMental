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
    @POST("api/auth/register")
    Call<Map<String, Object>> register(@Body Map<String, Object> body);

    @POST("api/auth/login")
    Call<Map<String, Object>> login(@Body Map<String, Object> body);

    @GET("api/auth/profile")
    Call<User> getProfile();

    // Checkin
    @POST("api/checkin")
    Call<Map<String, Object>> submitCheckin(@Body CheckinRequest request);

    @GET("api/checkin/history")
    Call<List<Map<String, Object>>> getCheckinHistory(@Query("days") int days);

    @GET("api/checkin/streak")
    Call<Map<String, Object>> getStreak();

    // Questionnaire
    @POST("api/questionnaire")
    Call<Map<String, Object>> submitQuestionnaire(@Body Map<String, Object> body);

    @GET("api/questionnaire/history")
    Call<List<Map<String, Object>>> getQuestionnaireHistory();

    @GET("api/questionnaire/latest")
    Call<Map<String, Object>> getLatestQuestionnaire();

    // AI
    @GET("api/ai/latest")
    Call<AiAnalysis> getLatestAi();

    @GET("api/ai/analysis")
    Call<AiAnalysis> getAiAnalysis(@Query("date") String date);

    // Stats
    @GET("api/stats/mood")
    Call<List<Map<String, Object>>> getMoodStats(@Query("days") int days);

    @GET("api/stats/symptoms")
    Call<List<Map<String, Object>>> getSymptomStats(@Query("days") int days);

    @GET("api/stats/correlation")
    Call<Map<String, Object>> getCorrelation();
}
