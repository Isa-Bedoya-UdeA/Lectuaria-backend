package com.lectuaria.backend.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TestConnection {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/lectuaria"; // ← minúsculas
        String user = "postgres";
        String password = "1234"; // ← Tu contraseña real

        System.out.println("Intentando conectar a: " + url);

        try {
            // Forzar carga del driver
            Class.forName("org.postgresql.Driver");

            Connection conn = DriverManager.getConnection(url, user, password);
            System.out.println("✅ Conexión exitosa!");
            System.out.println("   Database: " + conn.getCatalog());
            System.out.println("   URL: " + conn.getMetaData().getURL());
            conn.close();
        } catch (ClassNotFoundException e) {
            System.out.println("❌ Driver PostgreSQL no encontrado: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("❌ Error SQL: " + e.getMessage());
            System.out.println("   SQLState: " + e.getSQLState());
            System.out.println("   ErrorCode: " + e.getErrorCode());
        }
    }
}