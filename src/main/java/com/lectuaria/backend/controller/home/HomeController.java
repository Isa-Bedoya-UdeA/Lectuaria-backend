package com.lectuaria.backend.controller.home;

import com.lectuaria.backend.dto.home.FriendActivityDTO;
import com.lectuaria.backend.dto.home.HomeResponseDTO;
import com.lectuaria.backend.dto.recommendation.RecommendationDTO;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.security.AuthenticatedUserResolver;
import com.lectuaria.backend.service.home.IHomeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/home")
public class HomeController {

    private final IHomeService homeService;
    private final AuthenticatedUserResolver userResolver;

    public HomeController(IHomeService homeService, AuthenticatedUserResolver userResolver) {
        this.homeService = homeService;
        this.userResolver = userResolver;
    }

    @GetMapping
    public ResponseEntity<EntityModel<HomeResponseDTO>> getHome(
            @RequestParam(required = false) Long genreId,
            HttpServletRequest request) {
        User user = userResolver.requireCurrentUser(request);
        return ResponseEntity.ok(EntityModel.of(homeService.getHome(user, genreId),
                linkTo(methodOn(HomeController.class).getHome(genreId, request)).withSelfRel(),
                linkTo(methodOn(HomeController.class).getRecommendations(10, request)).withRel("recommendations"),
                linkTo(methodOn(HomeController.class).getFriendActivity(20, request)).withRel("friends-activity")));
    }

    @GetMapping("/recommendations")
    public ResponseEntity<CollectionModel<RecommendationDTO>> getRecommendations(
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        User user = userResolver.requireCurrentUser(request);
        return ResponseEntity.ok(CollectionModel.of(homeService.getRecommendations(user, size),
                linkTo(methodOn(HomeController.class).getRecommendations(size, request)).withSelfRel()));
    }

    @DeleteMapping("/recommendations/{bookId}")
    public ResponseEntity<Void> hideRecommendation(
            @PathVariable Long bookId,
            HttpServletRequest request) {
        User user = userResolver.requireCurrentUser(request);
        homeService.hideRecommendation(user, bookId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/friends/activity")
    public ResponseEntity<CollectionModel<FriendActivityDTO>> getFriendActivity(
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        User user = userResolver.requireCurrentUser(request);
        return ResponseEntity.ok(CollectionModel.of(homeService.getFriendActivity(user, size),
                linkTo(methodOn(HomeController.class).getFriendActivity(size, request)).withSelfRel()));
    }
}
