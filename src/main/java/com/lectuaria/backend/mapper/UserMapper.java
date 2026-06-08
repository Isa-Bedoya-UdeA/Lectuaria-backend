package com.lectuaria.backend.mapper;

import com.lectuaria.backend.dto.auth.RegisterRequestDTO;
import com.lectuaria.backend.dto.user.UserProfileDTO;
import com.lectuaria.backend.model.auth.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "stats", ignore = true)
    @Mapping(target = "friendshipStatus", ignore = true)
    @Mapping(target = "recentReviews", ignore = true)
    UserProfileDTO toUserProfileDto(User user);

    @Mapping(target = "password", ignore = true)
    @Mapping(target = "confirmPassword", ignore = true)
    @Mapping(target = "library", ignore = true)
    @Mapping(target = "userRole", source = "role")
    RegisterRequestDTO toDto(User user);

    // Custom method para construir un User desde un RegisterRequestDTO.
    // El password ya viene hasheado por el service layer.
    default User createUserFromRegisterRequest(RegisterRequestDTO registerRequestDTO, String hashedPassword) {
        return new User(
            registerRequestDTO.getFullName(),
            registerRequestDTO.getEmail(),
            hashedPassword,
            registerRequestDTO.getUserRole(),
            registerRequestDTO.getUsername(),
            null, // photoUrl
            null  // biography
        );
    }
}
