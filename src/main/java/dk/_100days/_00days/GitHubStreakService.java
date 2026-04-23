package dk._100days._00days;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GitHubStreakService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final String USERNAME = "NRamm99";
    private static final Pattern CONTRIBUTION_COUNT_PATTERN = Pattern.compile("(\\d+) contribution");

    private final HttpClient httpClient;
    private final Clock clock;

    public GitHubStreakService() {
        this(HttpClient.newHttpClient(), Clock.systemDefaultZone());
    }

    GitHubStreakService(HttpClient httpClient, Clock clock) {
        this.httpClient = httpClient;
        this.clock = clock;
    }

    public StreakData fetchStreak() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://github.com/users/" + USERNAME + "/contributions"))
                    .header("User-Agent", "100days-app")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                return StreakData.unavailable(USERNAME);
            }

            List<ContributionDay> days = parseContributionDays(response.body());
            if (days.isEmpty()) {
                return StreakData.unavailable(USERNAME);
            }

            int streak = calculateCurrentStreak(days, LocalDate.now(clock));
            ContributionDay latest = days.get(days.size() - 1);

            return new StreakData(
                    USERNAME,
                    streak,
                    latest.date().format(DATE_FORMATTER),
                    latest.count(),
                    false
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return StreakData.unavailable(USERNAME);
        } catch (IOException e) {
            return StreakData.unavailable(USERNAME);
        }
    }

    List<ContributionDay> parseContributionDays(String html) {
        Document document = Jsoup.parse(html);
        List<ContributionDay> days = new ArrayList<>();

        for (Element day : document.select(".ContributionCalendar-day[data-date]")) {
            String date = day.attr("data-date");
            if (date.isBlank()) {
                continue;
            }

            days.add(new ContributionDay(
                    LocalDate.parse(date, DATE_FORMATTER),
                    extractContributionCount(day)
            ));
        }

        days.sort(Comparator.comparing(ContributionDay::date));
        return days;
    }

    private int extractContributionCount(Element day) {
        Element tooltip = day.nextElementSibling();
        if (tooltip == null || !"tool-tip".equals(tooltip.tagName())) {
            return 0;
        }

        Matcher matcher = CONTRIBUTION_COUNT_PATTERN.matcher(tooltip.text());
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }

        return 0;
    }

    int calculateCurrentStreak(List<ContributionDay> days, LocalDate today) {
        int lastIndex = days.size() - 1;
        while (lastIndex >= 0 && days.get(lastIndex).date().isAfter(today)) {
            lastIndex--;
        }

        if (lastIndex < 0) {
            return 0;
        }

        int streak = 0;
        LocalDate expectedDate = days.get(lastIndex).date();

        if (days.get(lastIndex).count() == 0) {
            return 0;
        }

        for (int i = lastIndex; i >= 0; i--) {
            ContributionDay day = days.get(i);
            if (!day.date().equals(expectedDate) || day.count() == 0) {
                break;
            }
            streak++;
            expectedDate = expectedDate.minusDays(1);
        }

        return streak;
    }

    record ContributionDay(LocalDate date, int count) {
    }

    public record StreakData(
            String username,
            int streakDays,
            String latestDate,
            int latestContributionCount,
            boolean unavailable
    ) {
        static StreakData unavailable(String username) {
            return new StreakData(username, 0, "", 0, true);
        }
    }
}
