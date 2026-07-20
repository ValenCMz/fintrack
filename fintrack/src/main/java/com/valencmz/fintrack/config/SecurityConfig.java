package com.valencmz.fintrack.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        // Desactivamos CSRF
        // http.csrf(customizer -> customizer.disable());
        // Autorizamos todas las peticiones
        // http.authorizeHttpRequests(request -> request.anyRequest().authenticated());
        // Con esto activamos el login por defecto de Spring Security
        // http.formLogin(Customizer.withDefaults());
        // http.httpBasic(Customizer.withDefaults());

        // Configuracion de estado (Sin estado)
        // http.sessionManagement(session ->
        // session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // O todo junto utilizando el patron dle constructor

        http.csrf(customizer -> customizer.disable())
                .authorizeHttpRequests(request -> request.anyRequest().authenticated())
                .formLogin(Customizer.withDefaults())
                .httpBasic(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

}
