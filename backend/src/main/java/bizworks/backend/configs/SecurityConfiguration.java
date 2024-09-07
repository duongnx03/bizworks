package bizworks.backend.configs;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(request -> request
                        .requestMatchers("/api/auth/authenticate").permitAll()
                        .requestMatchers("/api/auth/forgot-password").permitAll()
                        .requestMatchers("/api/auth/forgot-password/reset").permitAll()
                        .requestMatchers("/api/auth/register").hasAnyAuthority("ADMIN", "MANAGE", "LEADER")
                        .requestMatchers("/api/auth/logout").hasAnyAuthority("ADMIN", "EMPLOYEE", "LEADER", "MANAGE")
                        .requestMatchers("/api/auth/refresh").hasAnyAuthority("ADMIN", "EMPLOYEE", "LEADER", "MANAGE")
                        .requestMatchers("/api/verify/**").hasAnyAuthority("ADMIN", "EMPLOYEE", "LEADER", "MANAGE")
                        .requestMatchers("/api/auth/reset-password").hasAnyAuthority("ADMIN", "EMPLOYEE", "LEADER", "MANAGE")
                        .requestMatchers("/api/employee/**").hasAnyAuthority("ADMIN", "EMPLOYEE", "LEADER", "MANAGE")
                        .requestMatchers("/api/attendance/**").hasAnyAuthority("ADMIN", "EMPLOYEE", "LEADER", "MANAGE")
                        .requestMatchers("/api/violation-types/**").hasAnyAuthority("ADMIN", "EMPLOYEE", "LEADER", "MANAGE")
                        .requestMatchers("/api/violations/**").hasAnyAuthority("ADMIN", "EMPLOYEE", "LEADER", "MANAGE")
                        .requestMatchers("/api/salaries/**").hasAnyAuthority("ADMIN", "EMPLOYEE", "LEADER", "MANAGE")
                        .requestMatchers("/api/departments/**").hasAnyAuthority("ADMIN", "LEADER", "MANAGE")
                        .requestMatchers("/api/positions/**").hasAnyAuthority("ADMIN", "LEADER", "MANAGE")
                        .requestMatchers("/api/complaint/**").hasAnyAuthority("ADMIN", "EMPLOYEE", "LEADER", "MANAGE")
                        .requestMatchers("/api/overtime/**").hasAnyAuthority("ADMIN", "EMPLOYEE", "LEADER", "MANAGE")
                        .requestMatchers("/api/emp-queue/**").hasAnyAuthority("ADMIN", "LEADER", "MANAGE")
                        .anyRequest().authenticated())
                .sessionManagement(manager -> manager.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return httpSecurity.build();
    }
}
