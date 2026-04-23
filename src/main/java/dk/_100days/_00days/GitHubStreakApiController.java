package dk._100days._00days;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GitHubStreakApiController {

    private final GitHubStreakService gitHubStreakService;

    public GitHubStreakApiController(GitHubStreakService gitHubStreakService) {
        this.gitHubStreakService = gitHubStreakService;
    }

    @GetMapping("/api/streak")
    public GitHubStreakService.StreakData streak() {
        return gitHubStreakService.fetchStreak();
    }
}
