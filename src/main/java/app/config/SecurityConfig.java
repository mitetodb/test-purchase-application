package app.config;

import app.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import jakarta.servlet.http.HttpServletResponse;

@EnableMethodSecurity(prePostEnabled = true)
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**")
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/register", "/forgot-password", "/reset-password" ,
                                "/css/**", "/js/**", "/images/**", "/avatars/**",
                                "/access-denied", "/favicon.ico", "/error",
                                "/actuator/health", "/actuator/health/**", "/actuator/info",
                                "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                        .requestMatchers("/users","/users/**").hasRole("ADMIN")
                        .requestMatchers("/customers","/customers/**").hasAnyRole("ADMIN","ACCOUNT_MANAGER","SALES_MANAGER")
                        .requestMatchers("/shops","/shops/**").hasAnyRole("ADMIN","ACCOUNT_MANAGER")
                        .requestMatchers("/invites","/invites/**").hasAnyRole("ADMIN","ACCOUNT_MANAGER")
                        .requestMatchers("/testpurchases/*/assign-mystery-shopper").hasAnyRole("ADMIN","ACCOUNT_MANAGER")
                        .requestMatchers("/testpurchases/**").authenticated()
                        .requestMatchers("/profile").authenticated()
                        .requestMatchers("/dashboard").authenticated()
                        .requestMatchers("/attachments/delete/**").hasRole("ADMIN")
                        .requestMatchers("/attachments/upload").authenticated()
                        .requestMatchers("/attachments/**").authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            String accept = request.getHeader("Accept");
                            if (accept != null && accept.contains("application/json")) {
                                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                                return;
                            }
                            request.getRequestDispatcher("/access-denied").forward(request, response);
                        })
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/home", true)
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}