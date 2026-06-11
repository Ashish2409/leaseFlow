package com.abm.leaseFlow.common.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {

    /** Base64-encoded HS256 secret key — minimum 256 bits. */
    private String secret;

    /** Access token validity in milliseconds. Default: 15 minutes. */
    private long accessTokenExpiryMs = 900_000L;

    /** Refresh token validity in milliseconds. Default: 7 days. */
    private long refreshTokenExpiryMs = 604_800_000L;
}
