package com.alquilereventos.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/** Filtro reactivo del Gateway: rechaza JWT invalido antes de rutear. */
@Configuration(proxyBeanMethods = false)
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Bean
    ReactiveJwtDecoder jwtDecoder(@Value("${app.security.jwt.secret}") String secret,
                                  @Value("${app.security.jwt.issuer}") String issuer,
                                  @Value("${app.security.jwt.algorithm:HS256}") String algorithm) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("SECURITY_JWT_SECRET es obligatorio");
        }
        MacAlgorithm mac = macAlgorithm(algorithm);
        SecretKey key = new SecretKeySpec(Base64.getDecoder().decode(secret), mac.getName());
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withSecretKey(key).macAlgorithm(mac).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }

    private static MacAlgorithm macAlgorithm(String algorithm) {
        return switch (algorithm == null ? "HS256" : algorithm.trim().toUpperCase()) {
            case "HS256" -> MacAlgorithm.HS256;
            case "HS384" -> MacAlgorithm.HS384;
            case "HS512" -> MacAlgorithm.HS512;
            default -> throw new IllegalStateException("Algoritmo JWT no soportado: " + algorithm);
        };
    }

    /**
     * CORS: la UI (React) llama directo al Gateway desde otro origen (por ejemplo
     * http://localhost:5173), asi que el Gateway responde el preflight y agrega las cabeceras
     * CORS en todas las respuestas, incluidos los 401/403 del filtro JWT. Los origenes
     * permitidos salen de CORS_ALLOWED_ORIGINS (separados por coma).
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:5173}") String allowedOrigins) {
        List<String> origins = Arrays.stream(allowedOrigins.split(",")).map(String::trim)
                .filter(o -> !o.isEmpty()).toList();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http,
                                                     GatewaySecurityErrorHandler errorHandler) {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);

        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(org.springframework.security.config.Customizer.withDefaults())
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/api/auth/login", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
                                "/seguridad/v3/api-docs/**", "/compras/v3/api-docs/**", "/alquiler/v3/api-docs/**",
                                "/actuator/health").permitAll()
                        .pathMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        .anyExchange().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(
                        new ReactiveJwtAuthenticationConverterAdapter(converter))))
                .build();
    }
}
