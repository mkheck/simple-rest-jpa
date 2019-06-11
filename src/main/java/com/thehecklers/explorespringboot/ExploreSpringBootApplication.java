package com.thehecklers.explorespringboot;

import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.annotation.Bean;
import org.springframework.data.repository.CrudRepository;
import org.springframework.web.bind.annotation.*;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Optional;
import java.util.stream.Stream;

@SpringBootApplication
public class ExploreSpringBootApplication {
    @Bean
    CommandLineRunner saveDogs(DogRepo repo) {
        return args -> {
            Stream.of("Golden Retriever", "Cocker Spaniel", "Dachshund")
                    .map(Dog::new)
                    .forEach(repo::save);

            repo.findAll()
                    .forEach(System.out::println);
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(ExploreSpringBootApplication.class, args);
    }

}

@EnableBinding(Sink.class)
@AllArgsConstructor
class DogCatcher {
    private final DogRepo repo;

    @StreamListener(Sink.INPUT)
    void saveDog(Dog dog) {
        Dog existingDog = repo.findByType(dog.getType());
        if (null == existingDog) {
            existingDog = repo.save(new Dog(dog.getType()));
        } 

        System.out.println(existingDog);
    }
}

@RestController
@RequestMapping("/dogs")
class SimpleController {
    private static final Logger logger = LoggerFactory.getLogger(ExploreSpringBootApplication.class);
    private final DogRepo repo;

    @Value("${server.port:8080}")
    private int i;

    SimpleController(DogRepo repo) {
        this.repo = repo;
    }

    @GetMapping("/hello")
    String home() {
        logger.info("WARN : Calling home() {}", i);
        return "Hola mundo!";
    }

    @GetMapping
    Iterable<Dog> getDogs() {
        return repo.findAll();
    }

    @GetMapping("/{id}")
    Optional<Dog> getDogById(@PathVariable Long id) {
        return repo.findById(id);
    }

    @PostMapping("/{type}")
    Dog saveNewDog(@PathVariable String type) {
        Dog existingDog = repo.findByType(type);
        if (null == existingDog) {
            repo.save(new Dog(type));
        }

        return repo.findByType(type);
    }
}

interface DogRepo extends CrudRepository<Dog, Long> {
    Dog findByType(String type);
}

@Entity
@Data
@NoArgsConstructor
@RequiredArgsConstructor
class Dog {
    @Id
    @GeneratedValue
    private Long id;
    @NonNull
    private String type;
}