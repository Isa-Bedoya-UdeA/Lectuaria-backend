package com.lectuaria.backend.controller.shared;

import com.lectuaria.backend.dto.shared.SharedBookDTO;
import com.lectuaria.backend.dto.list.UserListShareDTO;
import com.lectuaria.backend.security.AuthenticatedUserResolver;
import com.lectuaria.backend.service.shared.ISharedWithMeService;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/shared-with-me")
public class SharedWithMeController {

    private final ISharedWithMeService sharedWithMeService;
    private final AuthenticatedUserResolver userResolver;

    public SharedWithMeController(ISharedWithMeService sharedWithMeService,
                                  AuthenticatedUserResolver userResolver) {
        this.sharedWithMeService = sharedWithMeService;
        this.userResolver = userResolver;
    }

    @GetMapping("/lists")
    public ResponseEntity<CollectionModel<UserListShareDTO>> getSharedLists() {
        Long userId = userResolver.requireCurrentUserId();
        return ResponseEntity.ok(CollectionModel.of(sharedWithMeService.getSharedLists(userId),
                linkTo(methodOn(SharedWithMeController.class).getSharedLists()).withSelfRel(),
                linkTo(methodOn(SharedWithMeController.class).getSharedBooks()).withRel("books")));
    }

    @GetMapping("/books")
    public ResponseEntity<CollectionModel<SharedBookDTO>> getSharedBooks() {
        Long userId = userResolver.requireCurrentUserId();
        return ResponseEntity.ok(CollectionModel.of(sharedWithMeService.getSharedBooks(userId),
                linkTo(methodOn(SharedWithMeController.class).getSharedBooks()).withSelfRel(),
                linkTo(methodOn(SharedWithMeController.class).getSharedLists()).withRel("lists")));
    }
}
