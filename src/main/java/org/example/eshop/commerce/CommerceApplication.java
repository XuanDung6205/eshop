package org.example.eshop.commerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import java.time.Clock;

@SpringBootApplication(scanBasePackages = "org.example.eshop.commerce")
@EntityScan(basePackages = "org.example.eshop.commerce")
@EnableJpaRepositories(basePackages = "org.example.eshop.commerce")
public class CommerceApplication {
    public static void main(String[] args) {
        var application = new SpringApplication(CommerceApplication.class);
        application.setAdditionalProfiles("commerce");
        application.run(args);
    }
    @Bean Clock commerceClock() { return Clock.systemUTC(); }
}
