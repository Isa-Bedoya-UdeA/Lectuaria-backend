package com.lectuaria.backend.dto.list;

import com.lectuaria.backend.model.list.ListVisibility;

/**
 * DTO para edicion parcial de una lista de usuario.
 * Reutiliza la forma de CreateListRequestDTO pero se mantiene como tipo
 * separado para que el contrato REST de edicion pueda divergir en el
 * futuro (ej. aceptar solo cambios de privacidad) sin romper la creacion.
 */
public class UpdateListRequestDTO {
    private String name;
    private String description;
    private ListVisibility visibility;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public ListVisibility getVisibility() { return visibility; }
    public void setVisibility(ListVisibility visibility) { this.visibility = visibility; }
}
