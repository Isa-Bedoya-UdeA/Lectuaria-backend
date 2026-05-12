package com.lectuaria.backend.service.shared.impl;

import com.lectuaria.backend.dto.shared.SharedBookDTO;
import com.lectuaria.backend.dto.list.UserListShareDTO;
import com.lectuaria.backend.model.book.Book;
import com.lectuaria.backend.model.book.BookShare;
import com.lectuaria.backend.repository.book.BookShareRepository;
import com.lectuaria.backend.service.list.IUserListShareService;
import com.lectuaria.backend.service.shared.ISharedWithMeService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SharedWithMeServiceImpl implements ISharedWithMeService {

    private final IUserListShareService userListShareService;
    private final BookShareRepository bookShareRepository;

    public SharedWithMeServiceImpl(IUserListShareService userListShareService,
                                   BookShareRepository bookShareRepository) {
        this.userListShareService = userListShareService;
        this.bookShareRepository = bookShareRepository;
    }

    @Override
    public List<UserListShareDTO> getSharedLists(Long userId) {
        return userListShareService.getSharedLists(userId);
    }

    @Override
    public List<SharedBookDTO> getSharedBooks(Long userId) {
        List<BookShare> bookShares = bookShareRepository.findByReceiverId(userId);
        
        return bookShares.stream()
                .map(bookShare -> {
                    Book book = bookShare.getBook();
                    if (book != null) {
                        return new SharedBookDTO(
                                bookShare.getId(),
                                book.getId(),
                                String.valueOf(book.getIsbn()),
                                book.getTitle(),
                                book.getCoverUrl(),
                                bookShare.getSender().getFullName(),
                                bookShare.getMessage() != null ? bookShare.getMessage() : "",
                                bookShare.getSharedAt()
                        );
                    }
                    return null;
                })
                .filter(sharedBook -> sharedBook != null)
                .collect(Collectors.toList());
    }
}
