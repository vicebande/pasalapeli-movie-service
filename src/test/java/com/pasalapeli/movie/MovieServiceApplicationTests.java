package com.pasalapeli.movie;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:mysql://localhost:3306/test_db"
})
class MovieServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
