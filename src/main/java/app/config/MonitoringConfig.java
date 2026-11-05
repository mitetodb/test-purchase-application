package app.config;

import app.monitoring.HttpServerErrorTracker;
import app.monitoring.HttpServerErrorTrackingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.time.Clock;

@Configuration
public class MonitoringConfig {

    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }

    @Bean
    HttpServerErrorTracker httpServerErrorTracker(Clock clock) {
        return new HttpServerErrorTracker(clock);
    }

    @Bean
    FilterRegistrationBean<HttpServerErrorTrackingFilter> httpServerErrorTrackingFilter(
            HttpServerErrorTracker tracker) {
        FilterRegistrationBean<HttpServerErrorTrackingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new HttpServerErrorTrackingFilter(tracker));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return registration;
    }
}
