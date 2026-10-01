package org.example.client;

import com.beust.jcommander.Parameter;

public class Args {

    @Parameter(names = "-in",
            description = "Data file name"
    )
    private String fileName;

    @Parameter(names = "-t",
            description = "Is the type of the request."
    )
    private String type;
    @Parameter(names = "-k",
            description = "Specifies the key."
    )
    private String key;
    @Parameter(names = "-v",
            description = "Is the value"
    )
    private String value;

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setValue(String value) {
        this.value = value;
    }


    public String getType() {
        return type;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public String getFileName() {
        return fileName;
    }
}

