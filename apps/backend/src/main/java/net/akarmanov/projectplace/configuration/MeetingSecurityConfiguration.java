package net.akarmanov.projectplace.configuration;

import net.akarmanov.projectplace.security.MeetingPermissionEvaluator;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MeetingSecurityConfiguration {

    private MeetingPermissionEvaluator meetingPermissionEvaluator;
}
