package com.bryankrosenbaum.stepintime.model;

import java.io.Serializable;

public class User implements Serializable {
    private String displayName;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
