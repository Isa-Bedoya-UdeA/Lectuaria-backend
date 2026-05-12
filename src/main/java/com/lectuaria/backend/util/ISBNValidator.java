package com.lectuaria.backend.util;

/**
 * Validador de ISBN siguiendo las mismas reglas del frontend
 */
public class ISBNValidator {

    /**
     * Valida el formato del ISBN
     * Reglas:
     * 1. No debe contener letras (excepto x/X)
     * 2. Después de limpiar, solo números y x/X
     * 3. Debe tener 10 o 13 dígitos
     * 
     * @param isbn el ISBN a validar
     * @return true si es válido, false si no
     */
    public static boolean isValid(String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) {
            return false;
        }

        // Regla 1: No debe contener letras (excepto x/X)
        if (isbn.matches(".*[a-wyz].*i")) {
            return false;
        }

        // Limpiar: eliminar guiones, puntos y espacios
        String clean = isbn.replaceAll("[-.\\.\\s]", "");

        // Regla 2: Solo números y x/X
        if (!clean.matches("[0-9xX]*")) {
            return false;
        }

        // Regla 3: Debe tener 10 o 13 dígitos
        return clean.length() == 10 || clean.length() == 13;
    }

    /**
     * Obtiene un mensaje de error descriptivo
     * 
     * @param isbn el ISBN inválido
     * @return mensaje de error
     */
    public static String getErrorMessage(String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) {
            return "ISBN no puede estar vacío";
        }

        // Verifica si contiene letras (excepto x/X)
        if (isbn.matches(".*[a-wyz].*i")) {
            return "Formato de ISBN incorrecto. No debe contener letras.";
        }

        // Limpiar: eliminar guiones, puntos y espacios
        String clean = isbn.replaceAll("[-.\\.\\s]", "");

        // Verifica si contiene caracteres inválidos
        if (!clean.matches("[0-9xX]*")) {
            return "Formato de ISBN incorrecto.";
        }

        // Verifica longitud
        if (clean.length() != 10 && clean.length() != 13) {
            return "ISBN debe tener 10 o 13 dígitos.";
        }

        return "ISBN inválido";
    }
}
