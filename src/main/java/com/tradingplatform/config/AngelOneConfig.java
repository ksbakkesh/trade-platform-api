package com.tradingplatform.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(AngelOneProperties.class)
public class AngelOneConfig {

    /**
     * One shared RestClient for all Angel One calls. Common headers that don't
     * change per-request (client code, source ID, IPs, MAC) are set here.
     * The Authorization (JWT) header changes per-call since the token rotates,
     * so that's added per-request in AngelOneAuthClient / downstream clients.
     */
    @Bean
    public RestClient angelOneRestClient(AngelOneProperties props) {
        return RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .defaultHeader("X-UserType", "USER")
                .defaultHeader("X-SourceID", "WEB")
                .defaultHeader("X-PrivateKey", props.getApiKey())
                .defaultHeader("X-ClientLocalIP", props.getClientLocalIp())
                .defaultHeader("X-ClientPublicIP", props.getClientPublicIp())
                .defaultHeader("X-MACAddress", props.getMacAddress())
                .build();
    }
}
