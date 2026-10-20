package net.trackme.sso.services.yandex;

/** Compatibility for pending profiles serialized before the shared social login rollout. */
@Deprecated
public record YandexProfile(String id, String email, String name, java.time.Instant expiresAt)
        implements java.io.Serializable {}
