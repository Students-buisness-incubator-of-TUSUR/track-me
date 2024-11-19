package net.akarmanov.projectplace.security;

import lombok.RequiredArgsConstructor;
import net.akarmanov.projectplace.services.meeting.MeetingService;
import net.akarmanov.projectplace.services.user.UserService;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
@RequiredArgsConstructor
public class MeetingPermissionEvaluator implements PermissionEvaluator {

    private final UserService userService;

    private final MeetingService meetingService;

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        return false;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType,
                                 Object permission) {
        return false;
    }
}
