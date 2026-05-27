package com.lectuaria.backend.util;

public class ISBNValidator {

    public static boolean isValid(String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) {
            return false;
        }

        if (isbn.toLowerCase().matches("[a-wyz]")) {
            return false;
        }

        String clean = isbn.replaceAll("[-.\\.\\s]", "");

        if (!clean.matches("[0-9xX]*")) {
            return false;
        }

        return clean.length() == 10 || clean.length() == 13;
    }

    public static String getErrorMessage(String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) {
            return "ISBN no puede estar vacío";
        }

        if (isbn.toLowerCase().matches("[a-wyz]")) {
            return "Formato de ISBN incorrecto. No debe contener letras.";
        }

        String clean = isbn.replaceAll("[-.\\.\\s]", "");

        if (!clean.matches("[0-9xX]*")) {
            return "Formato de ISBN incorrecto.";
        }

        if (clean.length() != 10 && clean.length() != 13) {
            return "ISBN debe tener 10 o 13 dígitos.";
        }

        return "ISBN inválido";
    }
}