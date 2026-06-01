package com.mindbody.app.model;

import com.google.gson.annotations.SerializedName;

public class Symptom {
    @SerializedName("body_part")
    private String bodyPart;

    @SerializedName("symptom_name")
    private String symptomName;

    @SerializedName("severity")
    private String severity;

    @SerializedName("is_custom")
    private boolean isCustom;

    public Symptom() {
    }

    public Symptom(String bodyPart, String symptomName, String severity) {
        this.bodyPart = bodyPart;
        this.symptomName = symptomName;
        this.severity = severity;
        this.isCustom = false;
    }

    public String getBodyPart() { return bodyPart; }
    public void setBodyPart(String bodyPart) { this.bodyPart = bodyPart; }

    public String getSymptomName() { return symptomName; }
    public void setSymptomName(String symptomName) { this.symptomName = symptomName; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public boolean isCustom() { return isCustom; }
    public void setCustom(boolean custom) { isCustom = custom; }
}
