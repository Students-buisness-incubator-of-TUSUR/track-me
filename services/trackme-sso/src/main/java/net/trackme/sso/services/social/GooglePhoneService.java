package net.trackme.sso.services.social;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Requests only contact phone numbers, never authentication/recovery credentials. */
@lombok.extern.slf4j.Slf4j
public class GooglePhoneService {
    public static final String PHONE_SCOPE = "https://www.googleapis.com/auth/user.phonenumbers.read";
    private final RestClient client;

    public GooglePhoneService(RestClient client) { this.client = client; }

    public static GooglePhoneService create() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(3));
        return new GooglePhoneService(RestClient.builder().requestFactory(factory).build());
    }

    public String find(String accessToken, java.util.Set<String> grantedScopes) {
        if (!grantedScopes.contains(PHONE_SCOPE)) {
            log.info("Google phone lookup: permission not granted");
            return "";
        }
        try {
            var person = client.get().uri("https://people.googleapis.com/v1/people/me?personFields=phoneNumbers")
                    .headers(headers -> headers.setBearerAuth(accessToken)).retrieve().body(JsonNode.class);
            if (person == null) {
                log.info("Google phone lookup: empty People API response");
                return "";
            }
            String fallback = "";
            for (var phone : person.path("phoneNumbers")) {
                String raw = phone.path("canonicalForm").asText(phone.path("value").asText(""));
                String number = raw.replaceAll("[^+0-9]", "");
                if (!number.matches("\\+?\\d{10,15}")) continue;
                if (phone.path("metadata").path("primary").asBoolean()) {
                    log.info("Google phone lookup: contact number available");
                    return number;
                }
                if (fallback.isEmpty()) fallback = number;
            }
            log.info("Google phone lookup: contact number available={}", !fallback.isEmpty());
            return fallback;
        } catch (RestClientResponseException exception) {
            log.info("Google phone lookup: People API HTTP status={}", exception.getStatusCode().value());
            return "";
        } catch (RestClientException _) {
            // This optional profile field must not break authentication or leak token/response data.
            log.info("Google phone lookup: People API request failed");
            return "";
        }
    }
}
