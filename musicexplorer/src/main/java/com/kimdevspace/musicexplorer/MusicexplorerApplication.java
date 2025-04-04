package com.kimdevspace.musicexplorer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@SpringBootApplication
public class MusicexplorerApplication {

	public static void main(String[] args) {
		SpringApplication.run(MusicexplorerApplication.class, args);
	}

}
