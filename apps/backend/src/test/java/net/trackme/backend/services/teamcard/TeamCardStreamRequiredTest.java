package net.trackme.backend.services.teamcard;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import net.trackme.backend.domain.TeamCard;
import net.trackme.backend.repos.TeamCardsRepository;
import net.trackme.backend.services.stream.StreamService;
import net.trackme.commons.acl.AclService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.validation.beanvalidation.MethodValidationInterceptor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class TeamCardStreamRequiredTest {
    private final TeamCardsRepository repository = mock(TeamCardsRepository.class);
    private final StreamService streams = mock(StreamService.class);
    private final AclService acl = mock(AclService.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final TeamCardEventsProducer producer = mock(TeamCardEventsProducer.class);
    private ValidatorFactory validatorFactory;
    private TeamCardsService service;

    @BeforeEach
    void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        var target = new TeamCardsServiceImpl(repository, streams, acl, events, producer);
        var proxy = new ProxyFactory(target);
        proxy.addAdvice(new MethodValidationInterceptor(validatorFactory.getValidator()));
        service = (TeamCardsService) proxy.getProxy();
    }

    @AfterEach
    void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void missingStreamIsRejectedBeforeSavingOrPublishingEvents() {
        var team = new TeamCard();

        var error = assertThrows(ConstraintViolationException.class,
                () -> service.createTeamCard(team, null, "tracker1"));

        assertEquals("Привяжите команду к потоку акселератора.",
                error.getConstraintViolations().iterator().next().getMessage());
        verifyNoInteractions(repository, streams, acl, events, producer);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void missingTrackerIsRejectedBeforeSavingOrPublishingEvents(String username) {
        var team = new TeamCard();
        UUID streamId = UUID.randomUUID();

        var error = assertThrows(ConstraintViolationException.class,
                () -> service.createTeamCard(team, streamId, username));

        assertEquals("Укажите трекера команды.",
                error.getConstraintViolations().iterator().next().getMessage());
        verifyNoInteractions(repository, streams, acl, events, producer);
    }
}
