package com.example.steamlogin;

import Database.PostgresJDBC;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SteamLoginApplication {

    public static void main(String[] args) {
        SpringApplication.run(SteamLoginApplication.class, args);
    }

    /**
     * This runs automatically when the application starts
     */
    @Bean
    public PostgresJDBC database() {
        PostgresJDBC db = new PostgresJDBC();
        db.establishConnection();
        db.createTable();
        return db;
    }

    /**
     * Shutdown hook to close database connection
     */
    @Bean
    public CommandLineRunner shutdownHook(PostgresJDBC database) {
        return args -> {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down application...");
                database.closeConnection();
            }));
        };
    }
}