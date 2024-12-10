package net.akarmanov.projectplace.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.acl")
public class AclAppProperties {
    private final String classIdentityQuery;
    private final String sidIdentityQuery;
}

