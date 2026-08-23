package com.example.web.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Configuration {
    private int port;
    private String webRoot;

    // normal constructor wont work, we need to add @JsonCreator and JsonProperty to
    // indicate the field, because in runtime jackson wont know which parameter is
    // which, so a default code is: a = new Configuration(); a.setPort(something);
    // b.setWebRoot(something) if we dont use a JsonCreator and that requires a
    // constructor ()
    @JsonCreator
    public Configuration(@JsonProperty("port") int port,
            @JsonProperty("webRoot") String webRoot) {
        this.port = port;
        this.webRoot = webRoot;
    }

    public Configuration() {
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setWebRoot(String webRoot) {
        this.webRoot = webRoot;
    }

    public int getPort() {
        return this.port;
    }

    public String getWebRoot() {
        return this.webRoot;
    }
}
