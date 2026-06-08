package com.lectuaria.backend.controller.user;

import com.lectuaria.backend.dto.user.FriendActivityDTO;
import com.lectuaria.backend.dto.user.UserProfileDTO;
import com.lectuaria.backend.dto.user.UserStatsDTO;
import com.lectuaria.backend.dto.statistics.ReadingStatisticsDTO;
import com.lectuaria.backend.dto.statistics.SocialStatisticsDTO;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.security.AuthenticatedUserResolver;
import com.lectuaria.backend.service.user.IUserProfileService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/users")
public class UserProfileController {

    private final IUserProfileService userProfileService;
    private final AuthenticatedUserResolver userResolver;

    public UserProfileController(IUserProfileService userProfileService,
                                  AuthenticatedUserResolver userResolver) {
        this.userProfileService = userProfileService;
        this.userResolver = userResolver;
    }

    @GetMapping("/{usernameSlug}")
    public ResponseEntity<EntityModel<UserProfileDTO>> getUserProfile(
            @PathVariable String usernameSlug,
            HttpServletRequest request) {
        User currentUser = userResolver.tryGetCurrentUser(request);
        UserProfileDTO profile = userProfileService.getUserProfileByUsername(usernameSlug, currentUser);
        return ResponseEntity.ok(EntityModel.of(profile,
                linkTo(methodOn(UserProfileController.class).getUserProfile(usernameSlug, request)).withSelfRel(),
                linkTo(methodOn(UserProfileController.class).getUserStats(usernameSlug)).withRel("stats"),
                linkTo(methodOn(UserProfileController.class).getReadingStatistics(usernameSlug)).withRel("reading-statistics"),
                linkTo(methodOn(UserProfileController.class).getSocialStatistics(usernameSlug)).withRel("social-statistics"),
                linkTo(methodOn(UserProfileController.class).getFriendActivity(usernameSlug, request)).withRel("activity")));
    }

    @GetMapping("/{usernameSlug}/stats")
    public ResponseEntity<EntityModel<UserStatsDTO>> getUserStats(@PathVariable String usernameSlug) {
        return ResponseEntity.ok(EntityModel.of(userProfileService.getUserStats(usernameSlug),
                linkTo(methodOn(UserProfileController.class).getUserStats(usernameSlug)).withSelfRel()));
    }

    @GetMapping("/{usernameSlug}/reading-statistics")
    public ResponseEntity<EntityModel<ReadingStatisticsDTO>> getReadingStatistics(@PathVariable String usernameSlug) {
        return ResponseEntity.ok(EntityModel.of(userProfileService.getReadingStatistics(usernameSlug),
                linkTo(methodOn(UserProfileController.class).getReadingStatistics(usernameSlug)).withSelfRel()));
    }

    @GetMapping("/{usernameSlug}/social-statistics")
    public ResponseEntity<EntityModel<SocialStatisticsDTO>> getSocialStatistics(@PathVariable String usernameSlug) {
        return ResponseEntity.ok(EntityModel.of(userProfileService.getSocialStatistics(usernameSlug),
                linkTo(methodOn(UserProfileController.class).getSocialStatistics(usernameSlug)).withSelfRel()));
    }

    @GetMapping("/{usernameSlug}/activity")
    public ResponseEntity<CollectionModel<FriendActivityDTO>> getFriendActivity(
            @PathVariable String usernameSlug,
            HttpServletRequest request) {
        User currentUser = userResolver.tryGetCurrentUser(request);
        List<FriendActivityDTO> activities = userProfileService.getFriendActivity(usernameSlug, currentUser);
        return ResponseEntity.ok(CollectionModel.of(activities,
                linkTo(methodOn(UserProfileController.class).getFriendActivity(usernameSlug, request)).withSelfRel()));
    }
}
