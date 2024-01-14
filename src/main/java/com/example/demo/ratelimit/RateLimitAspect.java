package com.example.demo.ratelimit;

import com.example.demo.ratelimit.util.HttpUtils;
import org.apache.catalina.connector.RequestFacade;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Aspect
@Component
public class RateLimitAspect {
    @Value("${rate.limit.interval:1000}")
    private int interval;
    @Value("${rate.limit.count:5}")
    private int count;
    private final static String TOO_MANY_REQUEST = "Too many request";
    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<Long>> ipEndpointToTimestamp = new ConcurrentHashMap<>();

    @Around("@annotation(com.example.demo.ratelimit.RateLimit)")
    public Object logBeforeControllerRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        Optional<String> ip = getIp(joinPoint.getArgs());
        if (ip.isPresent()) {
            String key = createKey(ip.get(), joinPoint.getSignature().getName());
            ipEndpointToTimestamp.compute(key, (k, timestamps) -> {
                if (timestamps == null) {
                    timestamps = new ConcurrentLinkedDeque<>(Collections.singleton(Clock.systemUTC().millis()));
                    return timestamps;
                }
                timestamps.removeIf(getFilterOutdatedTimestamps());
                if (timestamps.size() < count) {
                    timestamps.add(Clock.systemUTC().millis());
                }
                return timestamps;
            });
            if (ipEndpointToTimestamp.get(key).size() >= count) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(TOO_MANY_REQUEST);
            }
        }
        return joinPoint.proceed();
    }

    @Scheduled(fixedRateString = "${rate.limit.interval:1000}")
    public void cleaner() {
        ipEndpointToTimestamp.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                entry.getValue().removeIf(getFilterOutdatedTimestamps());
                return entry.getValue().isEmpty();
            }
        });
    }

    private Predicate<Long> getFilterOutdatedTimestamps() {
        return timestamp -> Clock.systemUTC().millis() - timestamp > interval;
    }

    private static String createKey(String ip, String endpoint) {
        return ip + "-" + endpoint;
    }

    private static Optional<String> getIp(Object[] args) {
        return Stream.of(args)
                .filter(e -> e instanceof RequestFacade)
                .map(e -> HttpUtils.getRequestIP((RequestFacade) e))
                .findFirst();
    }
}
