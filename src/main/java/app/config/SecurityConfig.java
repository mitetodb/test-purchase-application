package app.config;

import app.security.CustomUserDetailsService;
import app.security.InactiveUserSessionFilter;
import app.security.LoginAuthenticationFailureHandler;
import app.security.LoginAuthenticationSuccessHandler;
import app.security.LoginAwareDaoAuthenticationProvider;
import app.security.LoginRateLimitFilter;
import app.security.NotFoundOrLoginAuthenticationEntryPoint;
import app.security.TwoFactorGateFilter;
import app.repository.UserRepository;
import app.service.LoginSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;

@EnableMethodSecurity(prePostEnabled = true)
@Configuration
@EnableConfigurationProperties({LoginSecurityProperties.class, PasswordPolicyProperties.class, TotpProperties.class, TwoFactorSecurityProperties.class})
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final LoginRateLimitFilter loginRateLimitFilter;
    private final LoginAuthenticationFailureHandler loginAuthenticationFailureHandler;
    private final LoginAuthenticationSuccessHandler loginAuthenticationSuccessHandler;
    private final NotFoundOrLoginAuthenticationEntryPoint notFoundOrLoginAuthenticationEntryPoint;

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public InactiveUserSessionFilter inactiveUserSessionFilter(
            UserRepository userRepository,
            org.springframework.transaction.support.TransactionTemplate transactionTemplate) {
        return new InactiveUserSessionFilter(userRepository, transactionTemplate);
    }

    @Bean
    public LoginAwareDaoAuthenticationProvider loginAwareDaoAuthenticationProvider(
            PasswordEncoder passwordEncoder,
            UserRepository userRepository,
            LoginSecurityService loginSecurityService) {
        LoginAwareDaoAuthenticationProvider provider = new LoginAwareDaoAuthenticationProvider(
                passwordEncoder, userRepository, loginSecurityService);
        provider.setUserDetailsService(customUserDetailsService);
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            LoginAwareDaoAuthenticationProvider loginAwareDaoAuthenticationProvider,
            SessionRegistry sessionRegistry,
            @Nullable InactiveUserSessionFilter inactiveUserSessionFilter,
            TwoFactorGateFilter twoFactorGateFilter) throws Exception {

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
                        .requestMatchers("/audit","/audit/**").hasRole("ADMIN")
                        .requestMatchers("/users","/users/**").hasRole("ADMIN")
                        .requestMatchers("/customers","/customers/**").hasAnyRole("ADMIN","ACCOUNT_MANAGER","SALES_MANAGER")
                        .requestMatchers("/shops","/shops/**").hasAnyRole("ADMIN","ACCOUNT_MANAGER")
                        .requestMatchers("/invites","/invites/**").hasAnyRole("ADMIN","ACCOUNT_MANAGER")
                        .requestMatchers("/testpurchases/*/assign-mystery-shopper").hasAnyRole("ADMIN","ACCOUNT_MANAGER")
                        .requestMatchers("/testpurchases/**").authenticated()
                        .requestMatchers("/profile").authenticated()
                        .requestMatchers("/dashboard").authenticated()
                        .requestMatchers("/attachments/upload/**").authenticated()
                        .requestMatchers("/attachments/**").authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(notFoundOrLoginAuthenticationEntryPoint)
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
                .authenticationProvider(loginAwareDaoAuthenticationProvider)
                .sessionManagement(session -> session
                        .maximumSessions(-1)
                        .maxSessionsPreventsLogin(false)
                        .sessionRegistry(sessionRegistry))
                .addFilterBefore(loginRateLimitFilter, UsernamePasswordAuthenticationFilter.class);
        if (inactiveUserSessionFilter != null) {
            http.addFilterAfter(inactiveUserSessionFilter, UsernamePasswordAuthenticationFilter.class);
        }
        http.addFilterAfter(twoFactorGateFilter, UsernamePasswordAuthenticationFilter.class);
        return http
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(loginAuthenticationSuccessHandler)
                        .failureHandler(loginAuthenticationFailureHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(SecurityHeadersPolicy.CONTENT_SECURITY_POLICY))
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)
                        .contentTypeOptions(contentType -> {})
                        .referrerPolicy(referrer -> referrer.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .permissionsPolicy(permissions -> permissions.policy(
                                SecurityHeadersPolicy.PERMISSIONS_POLICY))
                )
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            LoginAwareDaoAuthenticationProvider loginAwareDaoAuthenticationProvider) {
        return new ProviderManager(loginAwareDaoAuthenticationProvider);
    }
}
