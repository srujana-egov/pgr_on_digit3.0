package com.digit.librarycheck;

import com.digit.config.ApiConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
public class LibraryCheckApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibraryCheckApplication.class, args);
    }
}
