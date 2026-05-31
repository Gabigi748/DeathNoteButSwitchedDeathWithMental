package com.mindbody.app.model;

import com.google.gson.annotations.SerializedName;

public class AiAnalysis {
    @SerializedName("ai_response")
    private String aiResponse;

    @SerializedName("analysis_date")
    private String analysisDate;

    @SerializedName("trigger_type")
    private String triggerType;

    public String getAiResponse() { return aiResponse; }
    public void setAiResponse(String aiResponse) { this.aiResponse = aiResponse; }

    public String getAnalysisDate() { return analysisDate; }
    public void setAnalysisDate(String analysisDate) { this.analysisDate = analysisDate; }

    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
}
