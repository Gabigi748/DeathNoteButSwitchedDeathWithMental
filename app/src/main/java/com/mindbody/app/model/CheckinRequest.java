package com.mindbody.app.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CheckinRequest {
    @SerializedName("mood_score")
    private int moodScore;

    @SerializedName("symptoms")
    private List<Symptom> symptoms;

    @SerializedName("diary_text")
    private String diaryText;

    public CheckinRequest(int moodScore, List<Symptom> symptoms, String diaryText) {
        this.moodScore = moodScore;
        this.symptoms = symptoms;
        this.diaryText = diaryText;
    }

    public int getMoodScore() { return moodScore; }
    public void setMoodScore(int moodScore) { this.moodScore = moodScore; }

    public List<Symptom> getSymptoms() { return symptoms; }
    public void setSymptoms(List<Symptom> symptoms) { this.symptoms = symptoms; }

    public String getDiaryText() { return diaryText; }
    public void setDiaryText(String diaryText) { this.diaryText = diaryText; }
}
