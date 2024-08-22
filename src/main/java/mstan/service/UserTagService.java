package mstan.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import mstan.domain.Action;
import mstan.domain.TimeRange;
import mstan.domain.UserProfileResult;
import mstan.domain.UserTagEntry;
import mstan.domain.UserTagEvent;
import mstan.domain.UserTagRepository;

@Service
public class UserTagService {

    private static final Logger log = LoggerFactory.getLogger(UserTagService.class);

    private final UserTagRepository userTagRepository;

    private final int kMaxLimit = 200;

    private final Map<EventKey, AtomicInteger> count = new HashMap<>();
    private final Map<Filters, Stats> stats = new HashMap<>();

    public UserTagService(UserTagRepository userTagRepository) {
        this.userTagRepository = userTagRepository;
    }

    @Async
    public void registerEvent(UserTagEvent event) {
        log.info("event: " + event);
        UserTagEntry entry = new UserTagEntry();
        entry.fromEvent(event);
        userTagRepository.save(entry);

        EventKey key = new EventKey(event.getCookie(), event.getAction());
        if (!count.containsKey(key)) {
            count.put(key, new AtomicInteger(0));
        }

        List<Filters> allFilters = Filters.allFilters(
            event.getAction(),
            event.getCookie(),
            event.getProductInfo().getBrandId(),
            event.getProductInfo().getCategoryId()
        );

        for (Filters f : allFilters) {
            if (stats.containsKey(f)) {
                stats.put(f, new Stats());
            }

            stats.get(f).updateWith(event.getProductInfo().getPrice());
        }

        if (count.get(key).incrementAndGet() % kMaxLimit == 0) {
            cleanupEvents(event.getCookie(), event.getAction());
        }
    }

    public void cleanupEvents(String cookie, Action action) {
        UserTagEntry entry = this.userTagRepository.findOutdatedEntryByCookieAction(cookie, action.ordinal());
        if (entry != null) {
            System.err.println("cleaning up entries for cookie: " + cookie + " action: " + action.name());
            this.userTagRepository.deleteOutdatedEntriesByCookieAction(cookie, action.ordinal(), entry.getTime());
        }
    }

    public UserProfileResult findUserProfiles(String cookie, TimeRange range, int limit) {
        List<UserTagEvent> views = new ArrayList<>();
        List<UserTagEvent> buys = new ArrayList<>();
        for (UserTagEntry entry : this.userTagRepository.findByCookieActionAndTimeBetween(cookie, Action.VIEW.ordinal(), range.getBegin(), range.getEnd(), limit))
            views.add(entry.intoEvent());
        for (UserTagEntry entry : this.userTagRepository.findByCookieActionAndTimeBetween(cookie, Action.BUY.ordinal(), range.getBegin(), range.getEnd(), limit))
            buys.add(entry.intoEvent());

        return new UserProfileResult(cookie, views, buys);
    }

}
