package mstan.domain;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface UserTagRepository extends JpaRepository<UserTagEntry, Long> {
    @Query(value = "SELECT * FROM user_tag WHERE cookie = ?1 AND action = ?2 AND time >= ?3 and time < ?4 ORDER BY time DESC LIMIT ?5", nativeQuery = true)
    List<UserTagEntry> findByCookieActionAndTimeBetween(String cookie, int action, Instant start, Instant end, int limit);

    @Query(value = "SELECT * FROM user_tag WHERE cookie = ?1 AND action = ?2 ORDER BY time DESC OFFSET 200 ROWS FETCH NEXT 1 ROWS ONLY", nativeQuery = true)
    UserTagEntry findOutdatedEntryByCookieAction(String cookie, int action);

    @Modifying
    @Query(value = "DELETE FROM user_tag WHERE cookie = ?1 AND action = ?2 AND time <= ?3", nativeQuery = true)
    void deleteOutdatedEntriesByCookieAction(String cookie, int action, Instant from);
}
