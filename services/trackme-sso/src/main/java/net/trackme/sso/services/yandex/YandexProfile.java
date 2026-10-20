package net.trackme.sso.services.yandex;

/**
 * Compatibility for pending profiles serialized before the shared social login rollout.
 * @deprecated Retained only for stored sessions; remove after legacy pending sessions expire.
 */
@Deprecated(since = "2026-10", forRemoval = false)
public record YandexProfile(String id, String email, String name, java.time.Instant expiresAt)
        implements java.io.Serializable {}
