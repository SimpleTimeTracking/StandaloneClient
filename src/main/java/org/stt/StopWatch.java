package org.stt;

import java.util.logging.Logger;

public class StopWatch {
    private static final Logger LOG = Logger.getLogger(StopWatch.class.getName());
    private final String name;
    private final long start;

    public StopWatch(String name) {
        this.name = name;
        start = System.currentTimeMillis();
    }

    public void stop() {
        LOG.finest(() -> String.format("%s : %d", name, (System.currentTimeMillis() - start)));
    }
}
