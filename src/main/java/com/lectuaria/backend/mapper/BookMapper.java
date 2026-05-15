package com.lectuaria.backend.mapper;

import com.lectuaria.backend.dto.book.BookDetailDTO;
import com.lectuaria.backend.dto.book.BookSummaryDTO;
import com.lectuaria.backend.model.book.Book;
import com.lectuaria.backend.model.book.Author;
import com.lectuaria.backend.model.book.Genre;
import com.lectuaria.backend.model.book.Publisher;
import com.lectuaria.backend.model.book.BookFormat;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface BookMapper {

    @Mapping(target = "authors", source = "authors", qualifiedByName = "authorsToStringList")
    @Mapping(target = "genres", source = "genres", qualifiedByName = "genresToDtoList")
    @Mapping(target = "publishers", source = "publishers", qualifiedByName = "publishersToStringList")
    @Mapping(target = "formats", source = "formats", qualifiedByName = "formatsToStringList")
    @Mapping(target = "availability", ignore = true)
    BookDetailDTO toDetailDto(Book book);

    @Mapping(target = "authors", source = "authors", qualifiedByName = "authorsToStringList")
    @Mapping(target = "genres", source = "genres", qualifiedByName = "genresToDtoList")
    @Mapping(target = "libraryId", ignore = true)
    @Mapping(target = "userAddedId", ignore = true)
    @Mapping(target = "createdById", source = "createdBy.id")
    @Mapping(target = "availableLibraries", ignore = true)
    BookSummaryDTO toSummaryDto(Book book);

    List<BookSummaryDTO> toSummaryDtoList(List<Book> books);

    @Named("authorsToStringList")
    default List<String> authorsToStringList(List<Author> authors) {
        if (authors == null)
            return null;
        return authors.stream()
                .map(Author::getName)
                .collect(Collectors.toList());
    }

    @Named("genresToDtoList")
    default List<com.lectuaria.backend.dto.book.GenreDTO> genresToDtoList(List<Genre> genres) {
        if (genres == null)
            return null;
        return genres.stream()
                .map(genre -> new com.lectuaria.backend.dto.book.GenreDTO(genre.getId(), genre.getName(),
                        genre.getDescription()))
                .collect(Collectors.toList());
    }

    @Named("publishersToStringList")
    default List<String> publishersToStringList(List<Publisher> publishers) {
        if (publishers == null)
            return null;
        return publishers.stream()
                .map(Publisher::getName)
                .collect(Collectors.toList());
    }

    @Named("formatsToStringList")
    default List<String> formatsToStringList(List<BookFormat> formats) {
        if (formats == null)
            return null;
        return formats.stream()
                .map(format -> format.getFormat().getName())
                .collect(Collectors.toList());
    }
}
