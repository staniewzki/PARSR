package mstan.domain;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class TimeRange {
    private Instant begin;
    private Instant end;

    public TimeRange(String str) {
        String[] bounds = str.split("_");
        assert(bounds.length == 2);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        this.begin = LocalDateTime.parse(bounds[0], formatter).toInstant(ZoneOffset.UTC);
        this.end = LocalDateTime.parse(bounds[1], formatter).toInstant(ZoneOffset.UTC);
    }

    public Instant getBegin() {
        return begin;
    }

    public Instant getEnd() {
        return end;
    }
}
