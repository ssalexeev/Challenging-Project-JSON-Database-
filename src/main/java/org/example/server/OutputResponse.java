package org.example.server;

public class OutputResponse {
    private String response;
    private String reason;
    private String value;


    public OutputResponse(String response, String reason) {
        this.response = response;
        this.reason = reason;
    }

    public OutputResponse() {
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
    public void setOk() {
        this.response = "OK";
    }

    public void setError() {
        this.response = "ERROR";
    }



    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
