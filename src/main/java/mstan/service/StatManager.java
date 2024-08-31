package mstan.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatManager {
    private Map<String, Map<Filters, Stats>> stats = new HashMap<>();
    private final Object lock = new Object();

    private static final Logger log = LoggerFactory.getLogger(UserTagService.class);

    public StatManager() {}

    void addEvent(String minuteBucket, List<Filters> filters, int price) {
        // log.info(minuteBucket + " filters: " + filters + " price: " + price);
        synchronized (lock) {
            if (!stats.containsKey(minuteBucket)) {
                stats.put(minuteBucket, new HashMap<>());
            }
            Map<Filters, Stats> statsBucket = stats.get(minuteBucket);
            for (Filters f : filters) {
                if (!statsBucket.containsKey(f)) {
                    statsBucket.put(f, new Stats());
                }
                statsBucket.get(f).updateWith(price);
                // log.info("count: " + statsBucket.get(f).getCount());
            }
        }
    }

    public Map<String, Map<Filters, Stats>> drain() {
        Map<String, Map<Filters, Stats>> result;
        synchronized (lock) {
            result = stats;
            stats = new HashMap<>();
        }
        return result;
    }
}
