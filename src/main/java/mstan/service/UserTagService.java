package mstan.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.Host;
import com.aerospike.client.Key;
import com.aerospike.client.Operation;
import com.aerospike.client.Record;
import com.aerospike.client.cdt.ListOperation;
import com.aerospike.client.cdt.ListReturnType;
import com.aerospike.client.policy.ClientPolicy;
import com.aerospike.client.policy.CommitLevel;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.Replica;
import com.aerospike.client.policy.WritePolicy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import mstan.domain.Action;
import mstan.domain.Aggregate;
import mstan.domain.AggregatesQueryResult;
import mstan.domain.TimeRange;
import mstan.domain.UserProfileResult;
import mstan.domain.UserTagEvent;

@Service
public class UserTagService {

    private static final Logger log = LoggerFactory.getLogger(UserTagService.class);
    private final AerospikeClient client;

    private final ObjectMapper objectMapper;
    private final int kMaxLimit = 300;
    private final StatManager statManager = new StatManager();

    private static ClientPolicy defaultClientPolicy() {
        ClientPolicy defaultClientPolicy = new ClientPolicy();
        defaultClientPolicy.readPolicyDefault.replica = Replica.MASTER_PROLES;
        defaultClientPolicy.readPolicyDefault.socketTimeout = 1000;
        defaultClientPolicy.readPolicyDefault.totalTimeout = 1000;
        defaultClientPolicy.writePolicyDefault.socketTimeout = 15000;
        defaultClientPolicy.writePolicyDefault.totalTimeout = 15000;
        defaultClientPolicy.writePolicyDefault.maxRetries = 1;
        defaultClientPolicy.writePolicyDefault.commitLevel = CommitLevel.COMMIT_MASTER;
        defaultClientPolicy.writePolicyDefault.recordExistsAction = RecordExistsAction.REPLACE;
        return defaultClientPolicy;
    }

    public UserTagService(ObjectMapper objectMapper, @Value("${aerospike.seeds}") String[] aerospikeSeeds, @Value("${aerospike.port}") int port) {
        this.client = new AerospikeClient(defaultClientPolicy(), Arrays.stream(aerospikeSeeds).map(seed -> new Host(seed, port)).toArray(Host[]::new));
        this.client.truncate(null, "mimuw", "events", null);
        this.client.truncate(null, "mimuw", "index", null);
        this.client.truncate(null, "mimuw", "aggregate-buy", null);
        this.client.truncate(null, "mimuw", "aggregate-view", null);
        this.objectMapper = objectMapper;
    }

    @Async
    public void registerEvent(UserTagEvent event) {
        log.info("event: " + event);

        Key counterKey = new Key("mimuw", "index", eventKey(event.getCookie(), event.getAction()));
        Record record = client.operate(
            new WritePolicy(),
            counterKey,
            Operation.add(new Bin("counter", 1)),
            Operation.get("counter")
        );
        int index = record.getInt("counter") - 1;

        String json;
        try {
            json = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return;
        }

        Key eventKey = new Key("mimuw", "events", eventKey(event.getCookie(), event.getAction()));
        if (index < kMaxLimit) {
            client.operate(new WritePolicy(), eventKey,
                ListOperation.append("eventList", com.aerospike.client.Value.get(json)));
        } else {
            client.operate(new WritePolicy(), eventKey,
                ListOperation.set("eventList", index % kMaxLimit, com.aerospike.client.Value.get(json)));
        }

        log.info("filters: " + Filters.allFilters(
                event.getAction(),
                event.getOrigin(),
                event.getProductInfo().getBrandId(),
                event.getProductInfo().getCategoryId()
            ));

        statManager.addEvent(
            timestampToMinutes(event.getTime()),
            Filters.allFilters(
                event.getAction(),
                event.getOrigin(),
                event.getProductInfo().getBrandId(),
                event.getProductInfo().getCategoryId()
            ),
            event.getProductInfo().getPrice()
        );
    }

    // Runs every minute
    @Scheduled(fixedRate = 60000, initialDelayString = "${random.int(60000)}")
    public void execute() {
        log.info("execute");
        for (Map.Entry<String, Map<Filters, Stats>> bucket : statManager.drain().entrySet()) {
            log.info("key: " + bucket.getKey());
            for (Map.Entry<Filters, Stats> entry : bucket.getValue().entrySet()) {
                Action action = entry.getKey().getAction();
                String keyName = bucket.getKey() + entry.getKey().value();
                Key key = new Key("mimuw", aggregateSetName(action), keyName);
                log.info(key + " count: " + entry.getValue().getCount() + " sumPrice: " + entry.getValue().getSumPrice());
                client.operate(
                    new WritePolicy(),
                    key,
                    Operation.add(new Bin("count", entry.getValue().getCount())),
                    Operation.add(new Bin("sumPrice", entry.getValue().getSumPrice()))
                );
            }
        }
    }

    private String timestampToMinutes(Instant timestamp) {
        return timestamp.toString().substring(0, "2022-03-01T00:00".length());
    }

    private String aggregateSetName(Action action) {
        return "aggregate-" + (action == Action.BUY ? "buy" : "view");
    }

    private String eventKey(String cookie, Action action) {
        return cookie + "-" + action.name();
    }

    private List<UserTagEvent> fetchEventsByAction(String cookie, Action action, TimeRange range, int limit) {
        Key key = new Key("mimuw", "events", eventKey(cookie, action));
        Record record = client.operate(new WritePolicy(), key,
            ListOperation.getByIndexRange("eventList", 0, ListReturnType.VALUE));

        try {
            List<UserTagEvent> events = new ArrayList<>();
            if (record != null && record.bins.containsKey("eventList")) {
                @SuppressWarnings("unchecked")
                List<String> eventJsonList = (List<String>) record.getList("eventList");
                for (String eventJson : eventJsonList) {
                    UserTagEvent event = objectMapper.readValue(eventJson, UserTagEvent.class);
                    if (range.getBegin().compareTo(event.getTime()) <= 0 && event.getTime().compareTo(range.getEnd()) < 0) {
                        events.add(event);
                    }
                }
            }

            events.sort((a, b) -> { return -1 * a.getTime().compareTo(b.getTime()); });

            // Remove the extra entries
            int currentSize = events.size();
            if (currentSize > limit)
                events.subList(limit, currentSize).clear();

            return events;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public UserProfileResult findUserProfiles(String cookie, TimeRange range, int limit) {
        return new UserProfileResult(
            cookie,
            fetchEventsByAction(cookie, Action.VIEW, range, limit),
            fetchEventsByAction(cookie, Action.BUY, range, limit)
        );
    }

    public AggregatesQueryResult findAggregates(TimeRange range, Action action, String origin, String brandId, String categoryId, List<Aggregate> aggregates) {
        boolean aggregateCount = aggregates.contains(Aggregate.COUNT);
        boolean aggregatePrice = aggregates.contains(Aggregate.SUM_PRICE);

        List<String> columns = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();
        columns.add("1m_bucket");
        columns.add("action");
        if (origin != null) columns.add("origin");
        if (brandId != null) columns.add("brand_id");
        if (categoryId != null) columns.add("category_id");
        if (aggregateCount) columns.add("count");
        if (aggregatePrice) columns.add("sum_price");

        Filters filters = new Filters(action, origin, brandId, categoryId);
        // log.info("filters value: " + filters.value());
        for (Instant bucket = range.getBegin(); bucket.isBefore(range.getEnd()); bucket = bucket.plus(Duration.ofMinutes(1))) {
            String timestamp = timestampToMinutes(bucket);
            String keyName = timestamp + filters.value();
            Key key = new Key("mimuw", aggregateSetName(action), keyName);
            log.info(timestamp + ", " + keyName);
            log.info("jazdunia");
            int count = 0;
            int sumPrice = 0;
            try {
                Record record = client.operate(null, key, Operation.get("count"), Operation.get("sumPrice"));
                count = record.getInt("count");
                sumPrice = record.getInt("sumPrice");
            } catch (Exception e) {
                log.info(e.getMessage());
                log.info(e.getLocalizedMessage());
                log.info(e.getClass().toString());
            }
            log.info(String.valueOf(count));
            log.info(String.valueOf(sumPrice));

            List<String> row = new ArrayList<>();
            row.add(timestamp + ":00");
            row.add(action.name());
            if (origin != null) row.add(origin);
            if (brandId != null) row.add(brandId);
            if (categoryId != null) row.add(categoryId);
            if (aggregateCount) row.add(String.valueOf(count));
            if (aggregatePrice) row.add(String.valueOf(sumPrice));
            log.info(row.toString());
            rows.add(row);
        }

        return new AggregatesQueryResult(columns, rows);
    }
}
