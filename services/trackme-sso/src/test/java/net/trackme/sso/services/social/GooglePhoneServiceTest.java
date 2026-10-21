package net.trackme.sso.services.social;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GooglePhoneServiceTest {
    @Test
    void usesGrantedScopeAndBearerHeaderToRetrievePrimaryNumber() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://people.googleapis.com/v1/people/me?personFields=phoneNumbers"))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andRespond(withSuccess("{\"phoneNumbers\":[{\"value\":\"+7 (900) 123-45-67\",\"metadata\":{\"primary\":true}}]}", MediaType.APPLICATION_JSON));
        assertEquals("+79001234567", new GooglePhoneService(builder.build()).find("test-token", Set.of(GooglePhoneService.PHONE_SCOPE)));
        server.verify();
    }

    @Test
    void doesNotRequestPhoneWithoutPermission() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        assertEquals("", new GooglePhoneService(builder.build()).find("test-token", Set.of("email")));
        server.verify();
    }

    @Test
    void unavailablePeopleApiDoesNotBreakLogin() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://people.googleapis.com/v1/people/me?personFields=phoneNumbers"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));
        assertEquals("", new GooglePhoneService(builder.build()).find("test-token", Set.of(GooglePhoneService.PHONE_SCOPE)));
        server.verify();
    }

    @Test
    void grantedPermissionDoesNotGuaranteeAContactPhoneNumber() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://people.googleapis.com/v1/people/me?personFields=phoneNumbers"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertEquals("", new GooglePhoneService(builder.build()).find("test-token", Set.of(GooglePhoneService.PHONE_SCOPE)));
        server.verify();
    }

    @Test
    void usesCanonicalContactNumberWhenPrimaryFlagIsMissing() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://people.googleapis.com/v1/people/me?personFields=phoneNumbers"))
                .andRespond(withSuccess("{\"phoneNumbers\":[{\"canonicalForm\":\"+79131234567\"}]}", MediaType.APPLICATION_JSON));
        assertEquals("+79131234567", new GooglePhoneService(builder.build()).find("test-token", Set.of(GooglePhoneService.PHONE_SCOPE)));
        server.verify();
    }
}
