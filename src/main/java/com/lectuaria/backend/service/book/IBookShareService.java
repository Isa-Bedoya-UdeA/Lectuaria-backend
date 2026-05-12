package com.lectuaria.backend.service.book;

import com.lectuaria.backend.dto.book.BookShareRequestDTO;
import com.lectuaria.backend.dto.book.BookShareResponseDTO;
import com.lectuaria.backend.dto.book.ShareResultDTO;
import com.lectuaria.backend.model.auth.User;

import java.util.List;

public interface IBookShareService {
    ShareResultDTO shareBookWithFriends(Long bookId, BookShareRequestDTO request, User sender);
    List<BookShareResponseDTO> getReceivedShares(User user);
    List<BookShareResponseDTO> getSentShares(User user);
    boolean isBookSharedWithFriend(Long senderId, Long receiverId, Long bookId);
}
