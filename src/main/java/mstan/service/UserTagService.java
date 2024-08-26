package mstan.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.Host;
import com.aerospike.client.Key;
import com.aerospike.client.Operation;
import com.aerospike.client.Record;
import com.aerospike.client.cdt.ListOperation;
import com.aerospike.client.cdt.ListReturnType;
import com.aerospike.client.exp.ExpOperation;
import com.aerospike.client.policy.ClientPolicy;
import com.aerospike.client.policy.CommitLevel;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.Replica;
import com.aerospike.client.policy.WritePolicy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import mstan.domain.Action;
import mstan.domain.Device;
import mstan.domain.TimeRange;
import mstan.domain.UserProfileResult;
import mstan.domain.UserTagEvent;

@Service
public class UserTagService {

    private static final Logger log = LoggerFactory.getLogger(UserTagService.class);

    private final AerospikeClient client;
    private final ObjectMapper objectMapper;

    private final int kMaxLimit = 300;

    // private final Map<Filters, Stats> stats = new HashMap<>();

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
        this.objectMapper = objectMapper;
    }

    @Async
    public void registerEvent(UserTagEvent event) {
        log.info("event: " + event);

        Key counterKey = new Key("mimuw", "index", eventKey(event.getCookie(), event.getAction()));
        Record record = client.operate(new WritePolicy(), counterKey,
            Operation.add(new Bin("counter", 1)),
            Operation.get("counter"));
        int index = record.getInt("counter") - 1;

        String json;
        try {
            json = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return;
        }

        Key eventKey = new Key("mimuw", "events", eventKey(event.getCookie(), event.getAction()));
        if (index <= kMaxLimit) {
            client.operate(new WritePolicy(), eventKey,
                ListOperation.append("eventList", com.aerospike.client.Value.get(json)));
        } else {
            client.operate(new WritePolicy(), eventKey,
                ListOperation.set("eventList", index % kMaxLimit, com.aerospike.client.Value.get(json)));
        }
    }

    private static Device[] deviceValues = Device.values();
    private static Action[] actionValues = Action.values();

    private String eventKey(String cookie, Action action) {
        return cookie + "-" + action.name();
    }

    private List<UserTagEvent> fetchEventsByAction(String cookie, Action action, TimeRange range, int limit) {
        Key key = new Key("mimuw", "events", eventKey(cookie, action));

        Record record = client.operate(new WritePolicy(), key,
            ListOperation.getByIndexRange("eventList", 0, ListReturnType.VALUE));

        List<UserTagEvent> events = new ArrayList<>();

        try {
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

}
