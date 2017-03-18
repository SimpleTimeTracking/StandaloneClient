package org.stt.config;

import java.io.File;

public class PathSetting {
    private final String path;

    public PathSetting(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }

    public File file(String homePath) {
        return new File(path.replace("$HOME$", homePath));
    }
}
