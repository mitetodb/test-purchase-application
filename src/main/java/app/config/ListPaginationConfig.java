package app.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

@Configuration
@RequiredArgsConstructor
public class ListPaginationConfig {

    private final AppListPaginationProperties paginationProperties;

    @Bean
    PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer() {
        return resolver -> {
            resolver.setMaxPageSize(paginationProperties.getMaxPageSize());
            resolver.setFallbackPageable(PageRequest.of(
                    0,
                    paginationProperties.getDefaultPageSize(),
                    Sort.by(Sort.Direction.DESC, "createdOn")));
        };
    }
}
