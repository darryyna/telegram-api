package edu.lytvyniuk.telegrambot.service.calendar;

/*
  @author darin
  @project telegram-bot
  @class CalendarService
  @version 1.0.0
  @since 18.04.2026 - 15.25
*/
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import edu.lytvyniuk.telegrambot.entity.calendarEvent.CalendarEvent;
import edu.lytvyniuk.telegrambot.entity.calendarEvent.CalendarEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalendarService {

    private final CalendarEventRepository eventRepository;

    @Value("${google.calendar.enabled:false}")
    private boolean googleCalendarEnabled;

    @Value("${google.calendar.credentials-path:credentials.json}")
    private String credentialsPath;

    @Value("${google.calendar.tokens-dir:tokens}")
    private String tokensDir;

    private static final String APPLICATION_NAME = "TelegramBot Calendar";
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);
    private static final ZoneId KYIV_ZONE = ZoneId.of("Europe/Kyiv");
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private Calendar googleCalendar;

    @PostConstruct
    public void init() {
        if (!googleCalendarEnabled) {
            log.info("Google Calendar integration disabled. Using local DB only.");
            return;
        }
        try {
            googleCalendar = buildGoogleCalendarService();
            log.info("Google Calendar service initialized successfully.");
        } catch (Exception e) {
            log.warn("Google Calendar init failed (will use local DB only): {}", e.getMessage());
            googleCalendar = null;
        }
    }
    @Transactional
    public CalendarEvent createEvent(Long telegramId, String title, LocalDateTime start, LocalDateTime end) {
        CalendarEvent event = CalendarEvent.builder()
                .telegramId(telegramId)
                .title(title)
                .eventStart(start)
                .eventEnd(end != null ? end : start.plusHours(1))
                .build();

        if (googleCalendar != null) {
            String gId = pushToGoogle(title, start, end != null ? end : start.plusHours(1));
            event.setGoogleEventId(gId);
        }

        CalendarEvent saved = eventRepository.save(event);
        log.info("Event '{}' created for user {} at {}", title, telegramId, start);
        return saved;
    }

    public List<CalendarEvent> getUpcoming(Long telegramId) {
        return eventRepository.findByTelegramIdAndEventStartAfterOrderByEventStartAsc(
                telegramId, LocalDateTime.now(KYIV_ZONE));
    }

    public List<CalendarEvent> getEventsInRange(Long telegramId, LocalDateTime from, LocalDateTime to) {
        return eventRepository.findByTelegramIdAndEventStartBetweenOrderByEventStartAsc(
                telegramId, from, to);
    }

    public List<CalendarEvent> getTodayEvents(Long telegramId) {
        LocalDate today = LocalDate.now(KYIV_ZONE);
        return getEventsInRange(telegramId,
                today.atStartOfDay(),
                today.atTime(LocalTime.MAX));
    }

    public List<CalendarEvent> getWeekEvents(Long telegramId) {
        LocalDate today = LocalDate.now(KYIV_ZONE);
        return getEventsInRange(telegramId,
                today.atStartOfDay(),
                today.plusDays(7).atTime(LocalTime.MAX));
    }

    @Transactional
    public boolean deleteEvent(Long eventId, Long telegramId) {
        Optional<CalendarEvent> opt = eventRepository.findById(eventId);
        if (opt.isPresent() && opt.get().getTelegramId().equals(telegramId)) {
            CalendarEvent ev = opt.get();
            // Try to delete from Google Calendar too
            if (googleCalendar != null && ev.getGoogleEventId() != null) {
                deleteFromGoogle(ev.getGoogleEventId());
            }
            eventRepository.deleteById(eventId);
            return true;
        }
        return false;
    }

    public String formatEventList(List<CalendarEvent> events, String header) {
        if (events.isEmpty()) return "Немає запланованих подій.";
        StringBuilder sb = new StringBuilder("*" + header + "*\n\n");
        for (CalendarEvent e : events) {
            sb.append(String.format("📅 *#%d* %s\n   🕐 %s",
                    e.getId(), e.getTitle(), e.getEventStart().format(DISPLAY_FMT)));
            if (e.getEventEnd() != null) {
                sb.append(String.format(" — %s", e.getEventEnd().format(DISPLAY_FMT)));
            }
            if (e.getGoogleEventId() != null) sb.append(" *(GCal)*");
            sb.append("\n\n");
        }
        sb.append("Для видалення: `/deleteevent <id>`");
        return sb.toString();
    }
    private String pushToGoogle(String title, LocalDateTime start, LocalDateTime end) {
        try {
            Event event = new Event().setSummary(title);
            ZonedDateTime startZdt = start.atZone(KYIV_ZONE);
            ZonedDateTime endZdt   = end.atZone(KYIV_ZONE);

            event.setStart(new EventDateTime()
                    .setDateTime(new DateTime(startZdt.toInstant().toEpochMilli()))
                    .setTimeZone(KYIV_ZONE.getId()));
            event.setEnd(new EventDateTime()
                    .setDateTime(new DateTime(endZdt.toInstant().toEpochMilli()))
                    .setTimeZone(KYIV_ZONE.getId()));

            Event created = googleCalendar.events().insert("primary", event).execute();
            log.info("Event pushed to Google Calendar: {}", created.getId());
            return created.getId();
        } catch (Exception e) {
            log.error("Failed to push event to Google Calendar: {}", e.getMessage());
            return null;
        }
    }

    private void deleteFromGoogle(String googleEventId) {
        try {
            googleCalendar.events().delete("primary", googleEventId).execute();
            log.info("Event {} deleted from Google Calendar", googleEventId);
        } catch (Exception e) {
            log.warn("Failed to delete Google Calendar event {}: {}", googleEventId, e.getMessage());
        }
    }

    private Calendar buildGoogleCalendarService() throws Exception {
        NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
        InputStream credStream;
        try {
            credStream = new ClassPathResource(credentialsPath).getInputStream();
        } catch (Exception e) {
            credStream = new FileInputStream(credentialsPath);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
                new InputStreamReader(credStream));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                transport, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(tokensDir)))
                .setAccessType("offline")
                .build();

        Credential credential = new AuthorizationCodeInstalledApp(
                flow, new LocalServerReceiver.Builder().setPort(8888).build())
                .authorize("user");

        return new Calendar.Builder(transport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
}
