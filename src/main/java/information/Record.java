package information;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)

public class Record {

    private String Status;
    @JsonProperty("Score_Final__c")
    private long Score;
    @JsonProperty("Emails_Count__c")
    private long NumOfEmails;
    private String CaseNumber;
    @JsonProperty("Subscription_Type__c")
    private String SubscriptionType;
    @JsonProperty("attributes")
    private Attributes attributes;
    @JsonProperty("First_Response_Completed__c")
    private boolean Ir;
    @JsonProperty("Hours_from_case_creation__c")
    private float HoursFrom;


    public Record() {
    }

    public String getStatus() {
        return Status;
    }

    public void setStatus(String status) {
        Status = status;
    }

    public long getScore() {
        return Score;
    }

    public void setScore(long score) {
        Score = score;
    }

    public long getNumOfEmails() {
        return NumOfEmails;
    }

    public void setNumOfEmails(long numOfEmails) {
        NumOfEmails = numOfEmails;
    }

    public String getCaseNumber() {
        return CaseNumber;
    }

    public void setCaseNumber(String caseNumber) {
        CaseNumber = caseNumber;
    }

    public String getSubscriptionType() {
        return SubscriptionType;
    }

    public void setSubscriptionType(String subscriptionType) {
        SubscriptionType = subscriptionType;
    }

    public float getHours() {
        return HoursFrom;
    }

    public Attributes getAttributes() {
        return attributes;
    }

    public void setAttributes(Attributes attributes) {
        this.attributes = attributes;
    }

    public boolean isIr() {
        return Ir;
    }

    public void setIr(boolean ir) {
        Ir = ir;
    }

    public String toString() {
        return CaseNumber + ", IR: " + Ir + ", Score: " + Score + ", Hours since: " + HoursFrom + ", Sub' type :" + SubscriptionType + '\n';
    }


}

