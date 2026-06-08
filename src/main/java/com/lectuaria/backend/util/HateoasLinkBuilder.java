package com.lectuaria.backend.util;

import com.lectuaria.backend.controller.books.BookController;
import com.lectuaria.backend.dto.common.PaginatedResponse;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;

import java.util.List;
import java.util.function.Function;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Helper para construir respuestas HATEOAS sin repetir el patrón
 * {@code linkTo(methodOn(ControllerClass.class).method(...))} en cada controller.
 *
 * Centraliza la conversion de DTOs y paginas a {@link EntityModel} /
 * {@link PagedModel} con sus enlaces hipermedia correspondientes.
 */
public final class HateoasLinkBuilder {

    private HateoasLinkBuilder() {}

    /**
     * Enuelve un recurso en un {@link EntityModel} con su enlace self.
     * Si se pasa un id y un {@link RelBuilder}, se agregan enlaces adicionales
     * segun la relacion declarada.
     */
    public static <T> EntityModel<T> wrap(T resource, Long id, RelBuilder<T> relBuilder) {
        if (resource == null) {
            return null;
        }
        EntityModel<T> model = EntityModel.of(resource);
        if (id != null) {
            relBuilder.addLinks(model, resource, id);
        }
        return model;
    }

    /**
     * Convierte una lista de recursos en un CollectionModel con enlace self.
     */
    public static <T> CollectionModel<T> wrapAll(List<T> resources, Link selfLink) {
        return CollectionModel.of(resources, selfLink);
    }

    /**
     * Convierte un {@link PaginatedResponse} en un {@link PagedModel} HATEOAS
     * con enlaces prev/next/first/last y self.
     */
    public static <T> PagedModel<EntityModel<T>> wrapPage(
            PaginatedResponse<T> page,
            Class<?> selfControllerClass,
            Object selfControllerMethodArg) {
        List<EntityModel<T>> content = page.getContent().stream()
                .map(item -> EntityModel.of(item))
                .toList();

        PagedModel.PageMetadata metadata = new PagedModel.PageMetadata(
                page.getPageSize(),
                page.getPageNumber(),
                page.getTotalElements());

        PagedModel<EntityModel<T>> model = PagedModel.of(content, metadata);

        // self
        Link self = linkTo(selfControllerClass).withSelfRel();
        model.add(self);

        // first / last
        if (page.getTotalPages() > 0) {
            model.add(linkTo(selfControllerClass).withRel("first"));
            model.add(linkTo(selfControllerClass).withRel("last"));
        }

        // prev / next (best-effort: same controller, page +/- 1)
        if (page.isHasPrevious()) {
            model.add(Link.of("?page=" + (page.getPageNumber() - 1) + "&size=" + page.getPageSize(), "prev"));
        }
        if (page.isHasNext()) {
            model.add(Link.of("?page=" + (page.getPageNumber() + 1) + "&size=" + page.getPageSize(), "next"));
        }

        return model;
    }

    /**
     * Enlace simple self a un controller. Util cuando el cliente no necesita
     * hipermedia compleja.
     */
    public static Link selfLink(Class<?> controllerClass) {
        return linkTo(controllerClass).withSelfRel();
    }

    /**
     * Enlace a un metodo especifico de un controller.
     */
    public static Link methodLink(Class<?> controllerClass, String methodName, Object... args) {
        return WebMvcLinkBuilder.linkTo(methodOn(controllerClass, methodName, args))
                .withSelfRel();
    }

    /**
     * Interfaz funcional para agregar enlaces adicionales a un EntityModel.
     * Se usa cuando el controller quiere personalizar los enlaces
     * relacionados (e.g. self, related, share, ratings).
     */
    @FunctionalInterface
    public interface RelBuilder<T> {
        void addLinks(EntityModel<T> model, T resource, Long id);
    }

    /**
     * Helper que construye un RelBuilder con uno o mas enlaces a metodos
     * especificos del {@link BookController}. Se dejo como referencia; el
     * factory {@code BookResponseFactory} es la implementacion activa
     * del Factory Method pattern.
     */
    public static <T> RelBuilder<T> withBookRelations() {
        return (model, resource, id) -> {
            model.add(linkTo(methodOn(BookController.class).getBookById(id)).withSelfRel());
            model.add(linkTo(methodOn(BookController.class).getSimilarBooks(id)).withRel("similar"));
            model.add(linkTo(methodOn(BookController.class).getBookShareLink(id)).withRel("share-link"));
        };
    }

    /**
     * Convierte un DTO a EntityModel sin agregar enlaces adicionales.
     * Util para endpoints simples donde solo se necesita self.
     */
    public static <T> EntityModel<T> selfOnly(T resource) {
        return EntityModel.of(resource);
    }

    /**
     * Aplica una transformacion a cada elemento de una lista y devuelve
     * el resultado como CollectionModel.
     */
    public static <T, R> CollectionModel<R> mapAndWrap(
            List<T> source, Function<T, R> mapper, Link selfLink) {
        return CollectionModel.of(source.stream().map(mapper).toList(), selfLink);
    }
}
