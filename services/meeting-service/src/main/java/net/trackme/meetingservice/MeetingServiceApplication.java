package net.trackme.meetingservice;

import net.trackme.commons.acl.AclService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.acls.model.MutableAclService;

@SpringBootApplication
public class MeetingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MeetingServiceApplication.class, args);
    }

    @Bean
    AclService aclService(MutableAclService mutableAclService) {
        return new AclService(mutableAclService);
    }
}
