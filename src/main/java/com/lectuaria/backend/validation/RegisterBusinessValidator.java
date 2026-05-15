package com.lectuaria.backend.validation;

import com.lectuaria.backend.dto.auth.RegisterRequestDTO;
import com.lectuaria.backend.model.auth.UserRole;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RegisterBusinessValidator {

    private static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$";

    public List<String> validate(RegisterRequestDTO request) {
        List<String> errors = new ArrayList<>();

        // Validación de contraseña
        if (request.getPassword() != null && !request.getPassword().matches(PASSWORD_REGEX)) {
            errors.add("La contraseña debe tener mínimo 8 caracteres, una mayúscula, una minúscula y un número.");
        }

        if (request.getPassword() != null && !request.getPassword().equals(request.getConfirmPassword())) {
            errors.add("La confirmación de contraseña no coincide.");
        }

        // Validación para usuarios lectores
        if (request.getUserRole() == UserRole.READER && isBlank(request.getUsername())) {
            errors.add("El nombre de usuario es obligatorio para usuarios lectores.");
        }

        // Validación para bibliotecarios
        if (request.getUserRole() == UserRole.LIBRARIAN) {
            validateLibrarian(request, errors);
        }

        return errors;
    }

    private void validateLibrarian(RegisterRequestDTO request, List<String> errors) {
        if (request.getLibrary() == null) {
            errors.add("Los datos de la biblioteca son obligatorios para bibliotecarios.");
            return;
        }

        var lib = request.getLibrary();

        validateRequired(lib.getName(), "El nombre de la biblioteca es obligatorio.", errors);
        validateRequired(lib.getAddress(), "La dirección de la biblioteca es obligatoria.", errors);
        validateRequired(lib.getContactEmail(), "El correo de contacto de la biblioteca es obligatorio.", errors);
        validateRequired(lib.getOpeningHours(), "El horario de atención es obligatorio.", errors);
        validateRequired(lib.getIdZone(), "La zona/comuna de la biblioteca es obligatoria.", errors);

        // Validar formato de email de contacto
        if (lib.getContactEmail() != null && !lib.getContactEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            errors.add("El correo de contacto no tiene un formato válido.");
        }
    }

    private void validateRequired(String value, String errorMessage, List<String> errors) {
        if (isBlank(value)) {
            errors.add(errorMessage);
        }
    }

    private void validateRequired(Long value, String errorMessage, List<String> errors) {
        if (value == null) {
            errors.add(errorMessage);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}