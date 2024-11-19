package net.akarmanov.projectplace.services.tesk;

import java.util.concurrent.ThreadLocalRandom;

public class TrackNumberUtil {
    public static Integer generateTrackNumber() {
        return ThreadLocalRandom.current().nextInt(100000, 999999);
    }
}
