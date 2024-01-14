package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RateLimitApplicationTests {
    private static final int MAX_ATTEMPTS_BEFORE_LOCK = 5;
    private final TestRestTemplate restTemplate = new TestRestTemplate();
    private final ExecutorService executorService = Executors.newFixedThreadPool(MAX_ATTEMPTS_BEFORE_LOCK * 3);
    @LocalServerPort
    private int port;

    @Test
    public void testThrottleController() throws InterruptedException {
        String url = "http://localhost:" + port + "/ping";
        HttpEntity<String> firstIP = getEntityWithRandomIP();
        HttpEntity<String> secondIP = getEntityWithRandomIP();
        HttpEntity<String> thirdIP = getEntityWithRandomIP();

        for (int i = 0; i < MAX_ATTEMPTS_BEFORE_LOCK; i++) {
            executorService.submit(() -> {
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, firstIP, String.class);
                assertEquals(HttpStatus.OK, response.getStatusCode());
            });
            executorService.submit(() -> {
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, secondIP, String.class);
                assertEquals(HttpStatus.OK, response.getStatusCode());
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(1000, TimeUnit.MILLISECONDS);

        ResponseEntity<String> responseFirstIp = restTemplate.exchange(url, HttpMethod.GET, firstIP, String.class);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, responseFirstIp.getStatusCode());

        ResponseEntity<String> responseSecondIp = restTemplate.exchange(url, HttpMethod.GET, secondIP, String.class);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, responseSecondIp.getStatusCode());

        ResponseEntity<String> responseThirdIp = restTemplate.exchange(url, HttpMethod.GET, thirdIP, String.class);
        assertEquals(HttpStatus.OK, responseThirdIp.getStatusCode());
    }


    private static HttpEntity<String> getEntityWithRandomIP() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-FORWARDED-FOR", getRandomIp());
        return new HttpEntity<>(headers);
    }

    private static String getRandomIp() {
        Random r = new Random();
        return r.nextInt(256) + "." + r.nextInt(256) + "." + r.nextInt(256) + "." + r.nextInt(256);
    }
}
