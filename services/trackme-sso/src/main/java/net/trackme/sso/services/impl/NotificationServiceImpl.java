package net.trackme.sso.services.impl;

import java.text.Collator;
import java.util.*;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.trackme.sso.config.AppProperties;
import net.trackme.sso.dao.entity.UserEntity;
import net.trackme.sso.dao.repository.UserRepository;
import net.trackme.sso.services.EmailService;
import net.trackme.sso.services.NotificationService;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final String NOT_ASSIGNED = "Не назначен";
    private static final String FIELD_TEAM_CARD_NAME = "teamCardName";
    private static final String FIELD_STREAM_NAME = "streamName";
    private static final String FIELD_TRACKER_FULL_NAME = "trackerFullName";
    private static final String FIELD_MEETING_LINK = "meetingLink";
    private static final String FIELD_AVERAGE_GRADE = "averageGrade";

    private static final Collator RUSSIAN_COLLATOR = Collator.getInstance(Locale.of("ru", "RU"));

    private final UserRepository userRepository;

    private final AppProperties appProperties;

    private final EmailService emailService;

    @Override
    public void sendMeetingNotHappenedNotification(String teamCardUsername,
                                                   String teamCardName,
                                                   String streamName,
                                                   String meetingLink,
                                                   String trackerFullName) {
        var emailTo = userRepository.findByUsername(teamCardUsername)
                .orElseThrow();

        String fullName = (trackerFullName != null && !trackerFullName.isBlank()) 
        ? getShortName(trackerFullName) 
        : emailTo.getFullName();        

        emailService.sendMail(
                emailTo.getEmail(),
                appProperties.getMail().getFrom(),
                "[" + appProperties.getMail().getSubject() + "] Пропущена встреча",
                "email-meeting-not-happened.html",
                Map.of(
                        "email", emailTo,
                        "fullName", fullName,
                        "appName", appProperties.getMail().getSubject(),
                        "supportEmail", appProperties.getMail().getFrom(),
                        FIELD_TEAM_CARD_NAME, teamCardName,
                        FIELD_STREAM_NAME, streamName,
                        FIELD_MEETING_LINK, meetingLink));
    }

    @Override
    public void sendTeamCardSummary(
            List<LinkedHashMap<String, String>> teamCardSummaryEvents) {
        // Группируем по потокам
        Map<String, List<LinkedHashMap<String, String>>> groupedByStream = groupAndSortByStream(teamCardSummaryEvents);
        
        List<String> infos = new ArrayList<>();
        
        for (var entry : groupedByStream.entrySet()) {
            String streamName = entry.getKey();
            List<LinkedHashMap<String, String>> streamEvents = entry.getValue();
            
            int meetingNumber = 1; // Нумерация встреч начинается с 1 для каждого потока
            
            for (var event : streamEvents) {
                String trackerInfo = getShortName(event.getOrDefault(FIELD_TRACKER_FULL_NAME, NOT_ASSIGNED));
                
                var info = String.format("Поток: %s - Команда: %s - Трекер: %s - Встреча: %d<br>Ссылка на встречу: %s",
                        streamName,
                        event.get(FIELD_TEAM_CARD_NAME),
                        trackerInfo,
                        meetingNumber,
                        event.get(FIELD_MEETING_LINK));
                meetingNumber++;
                infos.add(info);
            }
        }

        String summary = String.join("<br><br>", infos);

        sendMessageToAnyEmails(
                "[" + appProperties.getMail().getSubject() + "] Сводка по пропущенным встречам",
                "email-not-happened-meetings-summary.html",
                summary);
    }

    @Override
    public void sendTeamCardLowGradeSummary(
            List<LinkedHashMap<String, String>> teamCardLowGradeSummaryEvents) {
        // Группируем по потокам
        Map<String, List<LinkedHashMap<String, String>>> groupedByStream = groupAndSortByStream(teamCardLowGradeSummaryEvents);
        
        List<String> infos = new ArrayList<>();
        int count = 1;
        
        for (var entry : groupedByStream.entrySet()) {
            String streamName = entry.getKey();
            List<LinkedHashMap<String, String>> streamEvents = entry.getValue();
            
            for (var event : streamEvents) {
                String trackerInfo = getShortName(event.getOrDefault(FIELD_TRACKER_FULL_NAME, NOT_ASSIGNED));
                
                var info = String.format("%d. Поток: %s - Команда: %s - Трекер: %s - Рейтинг: %s",
                        count,
                        streamName,
                        event.get(FIELD_TEAM_CARD_NAME),
                        trackerInfo,
                        event.get(FIELD_AVERAGE_GRADE));
                count++;
                infos.add(info);
            }
        }

        String summary = String.join("<br><br>", infos);

        sendMessageToAnyEmails(
                "[" + appProperties.getMail().getSubject() + "] Сводка по командам с низким рейтингом",
                "email-team-card-low-grade-summary.html",
                summary);
    }

    /**
     * Группирует события по потокам и сортирует команды внутри потока по алфавиту
     * (сначала английские названия, потом русские).
     */
    private Map<String, List<LinkedHashMap<String, String>>> groupAndSortByStream(
            List<LinkedHashMap<String, String>> events) {
        
        // Группируем по streamName, сохраняя порядок потоков
        Map<String, List<LinkedHashMap<String, String>>> grouped = new LinkedHashMap<>();
        
        for (var event : events) {
            String streamName = event.get(FIELD_STREAM_NAME);
            grouped.computeIfAbsent(streamName, k -> new ArrayList<>()).add(event);
        }
        
        // Сортируем команды внутри каждого потока
        for (var entry : grouped.entrySet()) {
            List<LinkedHashMap<String, String>> streamTeams = entry.getValue();
            streamTeams.sort((a, b) -> {
                String nameA = a.getOrDefault(FIELD_TEAM_CARD_NAME, "");
                String nameB = b.getOrDefault(FIELD_TEAM_CARD_NAME, "");
                return compareNamesAlphabetically(nameA, nameB);
            });
        }
        
        return grouped;
    }

    /**
     * Сравнивает названия: сначала английские (по алфавиту), потом русские (по алфавиту UTF-8).
     */
    private int compareNamesAlphabetically(String name1, String name2) {
        if (name1 == null) return 1;
        if (name2 == null) return -1;
        
        boolean isEnglish1 = name1.matches("^[A-Za-z].*");
        boolean isEnglish2 = name2.matches("^[A-Za-z].*");
        
        if (isEnglish1 && !isEnglish2) {
            return -1; // Английское перед русским
        } else if (!isEnglish1 && isEnglish2) {
            return 1;  // Русское после английского
        } else {
            // Оба английские или оба русские — сортируем по алфавиту
            return RUSSIAN_COLLATOR.compare(name1, name2);
        }
    }

    private void sendMessageToAnyEmails(String subject, String templateName, String summary) {
        var emailTos = userRepository.findAll()
                .stream()
                .filter(userEntity -> userEntity
                        .getRoles()
                        .stream()
                        .anyMatch(roleEntity -> appProperties
                                .getMail()
                                .getSummarySendRoles()
                                .contains(roleEntity.getCode())))
                .filter(UserEntity::getActive)
                .filter(user -> user.getEmail() != null && !user.getEmail().isBlank())
                .toList();

        if (emailTos.isEmpty()) {
            log.warn("No recipients with valid email for summary");
            return;
        }
        for (var emailTo : emailTos) {
            emailService.sendMail(
                    emailTo.getEmail(),
                    appProperties.getMail().getFrom(),
                    subject,
                    templateName,
                    Map.of(
                            "email", emailTo.getEmail(),
                            "fullName", emailTo.getFullName(),
                            "appName", appProperties.getMail().getSubject(),
                            "supportEmail", appProperties.getMail().getFrom(),
                            "summary", summary));
        }
    }

    private String getShortName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return NOT_ASSIGNED;
        }
        String[] parts = fullName.trim().split(" ");
        if (parts.length >= 2) {
            return parts[0] + " " + parts[1];
        }
        return fullName;
    }
}