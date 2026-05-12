package com.lectuaria.backend.service.shared;

import com.lectuaria.backend.dto.shared.SharedBookDTO;
import com.lectuaria.backend.dto.list.UserListShareDTO;

import java.util.List;

public interface ISharedWithMeService {
    List<UserListShareDTO> getSharedLists(Long userId);
    List<SharedBookDTO> getSharedBooks(Long userId);
}
