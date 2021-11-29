package no.nav.arbeid.tsbx.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * <p>Stores session identifiers for received front channel logout signals. Stores them for one hour after being received.</p>
 *
 * <p>Can be checked to validate local sessions.
 */
public class IdPortenFrontChannelLogoutEventStore {

    private final Cache<String, Instant> loggedOutIdPortenSessions
            = Caffeine.newBuilder()
            .maximumSize(2000)
            .expireAfter(
            new Expiry<String, Instant>() {
                @Override
                public long expireAfterCreate(@NonNull String key, @NonNull Instant value, long currentTime) {
                    return Duration.ofSeconds(3600).toNanos();
                }

                @Override
                public long expireAfterUpdate(@NonNull String key, @NonNull Instant value, long currentTime, @NonNegative long currentDuration) {
                    return currentDuration;
                }

                @Override
                public long expireAfterRead(@NonNull String key, @NonNull Instant value, long currentTime, @NonNegative long currentDuration) {
                    return currentDuration;
                }
            }
    ).build();

    /**
     * Register an ID-porten front channel logout event for the given session id.
     * @param idPortenSessionId
     */
    public void registerLogout(String idPortenSessionId) {
        loggedOutIdPortenSessions.put(idPortenSessionId, Instant.now());
    }

    /**
     * Returns at which time a logout signal was received for the provided ID-porten session id.
     *
     * @param idPortenSessionId
     * @return a timestamp, or empty Optional if no logout signal has been received within the last hour
     * for the provided session id.
     */
    public Optional<Instant> timeOfLogout(String idPortenSessionId) {
        return Optional.ofNullable(loggedOutIdPortenSessions.getIfPresent(idPortenSessionId));
    }

    /**
     * Removes a registered front channel logout event.
     */
    public void remove(String idPortenSessionId) {
        loggedOutIdPortenSessions.invalidate(idPortenSessionId);
    }
}
