package com.lectuaria.backend.service.home.recommendation;

import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.book.Book;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Strategy (GoF) para el algoritmo de recomendacion de libros.
 *
 * Cada implementacion encapsula UNA forma de elegir candidatos, generar
 * una explicacion legible y asignar un score. El {@code HomeServiceImpl}
 * inyecta {@code List<RecommendationStrategy>} ordenadas por {@code @Order}
 * y las encadena, permitiendo anadir nuevos algoritmos (trending,
 * "amigos leyeron", "mismo autor") sin modificar el orquestador.
 *
 * Contrato:
 *  - {@link #selectCandidates(User, int)} devuelve libros candidatos
 *    para el usuario, sin filtrar duplicados ni excluidos.
 *  - {@link #buildReason(Book, Map)} genera la explicacion visible al usuario.
 *  - {@link #score(Book)} asigna una puntuacion para ordenar.
 */
public interface RecommendationStrategy {

    /**
     * Nombre legible de la estrategia, util para logs y debugging.
     */
    String name();

    /**
     * Selecciona libros candidatos segun esta estrategia.
     * @param user usuario destino de la recomendacion
     * @param maxCandidates cota superior del tamano de la lista devuelta
     */
    List<Book> selectCandidates(User user, int maxCandidates);

    /**
     * Genera la explicacion visible de por qué se recomienda el libro.
     * @param book libro recomendado
     * @param preferredGenres mapa de generos preferidos del usuario (id -> nombre)
     */
    String buildReason(Book book, Map<Long, String> preferredGenres);

    /**
     * Calcula la puntuacion de un libro (mayor = mejor posicion).
     */
    BigDecimal score(Book book);
}
