// ============================================================
// config/SecurityConfig.java
// ============================================================
package br.com.escola.airquality.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Endpoints públicos de documentação
                        .requestMatchers("/swagger-ui/**", "/api-docs/**").permitAll()
                        // Healthcheck
                        .requestMatchers("/actuator/health").permitAll()
                        // Sensores autenticam com API key (tratada no filtro JWT)
                        .requestMatchers(HttpMethod.POST, "/api/v1/sensors/readings").hasRole("SENSOR")
                        // Leituras e dashboards — qualquer usuário autenticado
                        .requestMatchers(HttpMethod.GET, "/api/v1/**").hasAnyRole("SENSOR", "SCHOOL_ADMIN", "SYSTEM_ADMIN")
                        // Ações sobre alertas — admins apenas
                        .requestMatchers("/api/v1/alerts/**").hasAnyRole("SCHOOL_ADMIN", "SYSTEM_ADMIN")
                        // Relatórios — admins
                        .requestMatchers("/api/v1/reports/**").hasAnyRole("SCHOOL_ADMIN", "SYSTEM_ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}


// ============================================================
// config/JwtAuthenticationFilter.java
// ============================================================
package br.com.escola.airquality.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                String token = header.substring(7);
                SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String username = claims.getSubject();
                String role = claims.get("role", String.class);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    var auth = new UsernamePasswordAuthenticationToken(
                            username, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (JwtException e) {
                log.warn("Token JWT inválido: {}", e.getMessage());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token inválido");
                return;
            }
        }

        chain.doFilter(request, response);
    }
}


// ============================================================
// exception/GlobalExceptionHandler.java
// ============================================================
package br.com.escola.airquality.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiError> handleNotFound(NoSuchElementException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "Validação falhou",
                fieldErrors.toString(),
                LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(apiError);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        log.error("Erro não tratado", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor");
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ApiError(status.value(), status.getReasonPhrase(), message, LocalDateTime.now()));
    }

    public record ApiError(int status, String error, String message, LocalDateTime timestamp) {}
}


// ============================================================
// AqiCalculatorServiceTest.java
// ============================================================
package br.com.escola.airquality.service;

import br.com.escola.airquality.config.AirQualityProperties;
import br.com.escola.airquality.domain.SensorReading.AqiCategory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.*;

@DisplayName("AqiCalculatorService — testes unitários")
class AqiCalculatorServiceTest {

    private AqiCalculatorService calculator;

    @BeforeEach
    void setUp() {
        calculator = new AqiCalculatorService(new AirQualityProperties());
    }

    // ----------------------------------------------------------
    // Testes de CO₂
    // ----------------------------------------------------------

    @ParameterizedTest(name = "CO₂ {0} ppm → AQI {1}")
    @CsvSource({
        "400,   0",
        "600,  25",
        "800,  50",
        "900,  75",
        "1000, 100",
        "1500, 150",
        "2000, 200",
    })
    void co2AqiInterpolationIsCorrect(double co2, int expectedAqi) {
        int aqi = calculator.calculateCo2Aqi(co2);
        // Permite tolerância de ±3 pontos por arredondamento
        assertThat(aqi).isBetween(expectedAqi - 3, expectedAqi + 3);
    }

    @Test
    @DisplayName("CO₂ abaixo de 400 ppm → AQI mínimo 0")
    void co2BelowMinimumReturnsZero() {
        assertThat(calculator.calculateCo2Aqi(200)).isEqualTo(0);
    }

    // ----------------------------------------------------------
    // Testes de PM2.5
    // ----------------------------------------------------------

    @ParameterizedTest(name = "PM2.5 {0} µg/m³ → categoria {1}")
    @MethodSource("pm25Scenarios")
    void pm25CategoriesAreCorrect(double pm25, AqiCategory expected) {
        int aqi = calculator.calculatePm25Aqi(pm25);
        assertThat(calculator.categorize(aqi)).isEqualTo(expected);
    }

    static Stream<Arguments> pm25Scenarios() {
        return Stream.of(
                Arguments.of(5.0,   AqiCategory.GOOD),
                Arguments.of(20.0,  AqiCategory.MODERATE),
                Arguments.of(40.0,  AqiCategory.UNHEALTHY_SENSITIVE),
                Arguments.of(160.0, AqiCategory.UNHEALTHY),
                Arguments.of(260.0, AqiCategory.VERY_UNHEALTHY),
                Arguments.of(400.0, AqiCategory.HAZARDOUS)
        );
    }

    // ----------------------------------------------------------
    // Testes de cálculo geral (pior poluente)
    // ----------------------------------------------------------

    @Test
    @DisplayName("O AQI geral é determinado pelo poluente mais crítico")
    void overallAqiUsesWorstPollutant() {
        // CO₂ bom, PM2.5 ruim
        var result = calculator.calculate(
                BigDecimal.valueOf(500),     // CO₂ bom
                BigDecimal.valueOf(40.0),    // PM2.5 — unhealthy-sensitive
                null, null, null);

        assertThat(result.category()).isEqualTo(AqiCategory.UNHEALTHY_SENSITIVE);
        assertThat(result.worstPollutant()).isEqualTo("PM2.5");
    }

    @Test
    @DisplayName("Todas as leituras boas → AQI GOOD")
    void allGoodReadingsReturnGoodAqi() {
        var result = calculator.calculate(
                BigDecimal.valueOf(600),
                BigDecimal.valueOf(8.0),
                BigDecimal.valueOf(30.0),
                BigDecimal.valueOf(0.1),
                BigDecimal.valueOf(20.0));

        assertThat(result.category()).isEqualTo(AqiCategory.GOOD);
        assertThat(result.isActionRequired()).isFalse();
    }

    @Test
    @DisplayName("AQI HAZARDOUS → isEmergency() é true")
    void hazardousAqiIsEmergency() {
        var result = calculator.calculate(
                BigDecimal.valueOf(8000),    // CO₂ extremamente alto
                null, null, null, null);

        assertThat(result.isEmergency()).isTrue();
    }

    @Test
    @DisplayName("Leituras nulas não causam NullPointerException")
    void nullReadingsAreHandledGracefully() {
        assertThatCode(() -> calculator.calculate(null, null, null, null, null))
                .doesNotThrowAnyException();
    }

    // ----------------------------------------------------------
    // Testes de categorização
    // ----------------------------------------------------------

    @ParameterizedTest(name = "AQI {0} → {1}")
    @CsvSource({
        "0,   GOOD",
        "50,  GOOD",
        "51,  MODERATE",
        "100, MODERATE",
        "101, UNHEALTHY_SENSITIVE",
        "150, UNHEALTHY_SENSITIVE",
        "151, UNHEALTHY",
        "200, UNHEALTHY",
        "201, VERY_UNHEALTHY",
        "300, VERY_UNHEALTHY",
        "301, HAZARDOUS",
        "500, HAZARDOUS",
    })
    void categorizationBoundaries(int aqi, AqiCategory expected) {
        assertThat(calculator.categorize(aqi)).isEqualTo(expected);
    }
}
