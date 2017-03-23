package org.stt.config;

import java.time.DayOfWeek;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public class WorktimeConfig implements ConfigurationContainer {
    private List<String> breakActivities = Arrays.asList("pause", "break", "coffee");
    private Map<DayOfWeek, Duration> workingHours;
    private PathSetting workingTimesFile = new PathSetting("$HOME$/.stt/worktimes");

    public WorktimeConfig() {
        workingHours = Arrays.stream(DayOfWeek.values())
                .collect(
                        toMap(identity(),
                                dayOfWeek -> dayOfWeek != DayOfWeek.SUNDAY
                                        ? Duration.ofHours(8) : Duration.ZERO,
                                (u, v) -> {
                                    throw new IllegalStateException(String.format("Duplicate key %s", u));
                                },
                                LinkedHashMap::new));
    }

    public List<String> getBreakActivities() {
        return breakActivities;
    }

    public void setBreakActivities(List<String> breakActivities) {
        this.breakActivities = breakActivities;
    }

    public Map<DayOfWeek, Duration> getWorkingHours() {
        return workingHours;
    }

    public void setWorkingHours(Map<DayOfWeek, Duration> workingHours) {
        this.workingHours = workingHours;
    }

    public PathSetting getWorkingTimesFile() {
        return workingTimesFile;
    }

    public void setWorkingTimesFile(PathSetting workingTimesFile) {
        this.workingTimesFile = workingTimesFile;
    }
}
