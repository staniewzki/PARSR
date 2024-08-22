package mstan;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mstan.domain.Action;
import mstan.domain.Aggregate;
import mstan.domain.AggregatesQueryResult;
import mstan.domain.TimeRange;
import mstan.domain.UserProfileResult;
import mstan.domain.UserTagEvent;
import mstan.service.UserTagService;

@RestController
public class EchoClient {

    private static final Logger log = LoggerFactory.getLogger(EchoClient.class);

    private final UserTagService userTagService;

    public EchoClient(UserTagService userTagService) throws SQLException {
        this.userTagService = userTagService;
    }

    @PostMapping("/user_tags")
    public ResponseEntity<Void> addUserTag(@RequestBody(required = false) UserTagEvent userTag) {
        this.userTagService.registerEvent(userTag);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/user_profiles/{cookie}")
    public ResponseEntity<UserProfileResult> getUserProfile(@PathVariable("cookie") String cookie,
            @RequestParam("time_range") String timeRangeStr,
            @RequestParam(defaultValue = "200") int limit,
            @RequestBody(required = false) UserProfileResult expectedResult) {
        TimeRange range = new TimeRange(timeRangeStr);
        UserProfileResult resp = this.userTagService.findUserProfiles(cookie, range, limit);

        if (!resp.equals(expectedResult)) {
            System.out.println("-- wrong answer --");
            log.warn("---- response ----------");
            log.warn("views: " + resp.getViews());
            log.warn("buys: " + resp.getBuys());
            log.warn("---- expected result ----");
            log.warn("views: " + expectedResult.getViews());
            log.warn("buys: " + expectedResult.getBuys());
        }

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/aggregates")
    public ResponseEntity<AggregatesQueryResult> getAggregates(@RequestParam("time_range") String timeRangeStr,
            @RequestParam("action") Action action,
            @RequestParam("aggregates") List<Aggregate> aggregates,
            @RequestParam(value = "origin", required = false) String origin,
            @RequestParam(value = "brand_id", required = false) String brandId,
            @RequestParam(value = "category_id", required = false) String categoryId,
            @RequestBody(required = false) AggregatesQueryResult expectedResult) {

        return ResponseEntity.ok(expectedResult);
    }
}
