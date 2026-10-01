package org.example.server;

import com.beust.jcommander.Parameter;

public class Args {

    private String type;
    private Integer index;
    private String message;


    @Parameter(names = "-t",
            description = "Is the type of the request.",
            required = true
    )
    public void setType(String type) {
        this.type = type;
    }

    @Parameter(names = "-i",
            description = "Is the index of the cell."
    )
    public void setIndex(Integer index) {
        this.index = index;
    }

    @Parameter(names = "-m",
            description = "Is the value/message to save in the database (only needed for set requests).",
            required = false
    )
    public void setMessage(String message) {
        this.message = message;
    }


    public String getType() {
        return type;
    }

    public Integer getIndex() {
        return index;
    }

    public String getMessage() {
        return message;
    }
}
