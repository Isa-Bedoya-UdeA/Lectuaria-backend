package com.lectuaria.backend.controller.user;

import com.lectuaria.backend.dto.user.UpdatePrivacySettingsRequestDTO;
import com.lectuaria.backend.dto.user.UserPrivacySettingsDTO;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.security.AuthenticatedUserResolver;
import com.lectuaria.backend.service.user.IUserPrivacySettingsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/users/me/privacy")
public class UserPrivacySettingsController {

    private final IUserPrivacySettingsService privacyService;
    private final AuthenticatedUserResolver userResolver;

    public UserPrivacySettingsController(IUserPrivacySettingsService privacyService,
                                          AuthenticatedUserResolver userResolver) {
        this.privacyService = privacyService;
        this.userResolver = userResolver;
    }

    @GetMapping
    public ResponseEntity<EntityModel<UserPrivacySettingsDTO>> getMyPrivacySettings(HttpServletRequest request) {
        User user = userResolver.requireCurrentUser(request);
        return ResponseEntity.ok(EntityModel.of(privacyService.getSettings(user.getId()),
                linkTo(methodOn(UserPrivacySettingsController.class).getMyPrivacySettings(request)).withSelfRel(),
                linkTo(methodOn(UserPrivacySettingsController.class).updateMyPrivacySettings(request, null)).withRel("update")));
    }

    @PutMapping
    public ResponseEntity<EntityModel<UserPrivacySettingsDTO>> updateMyPrivacySettings(
            HttpServletRequest request,
            @Valid @RequestBody UpdatePrivacySettingsRequestDTO dto) {
        User user = userResolver.requireCurrentUser(request);
        return ResponseEntity.ok(EntityModel.of(privacyService.updateSettings(user.getId(), dto),
                linkTo(methodOn(UserPrivacySettingsController.class).updateMyPrivacySettings(request, dto)).withSelfRel()));
    }
}
