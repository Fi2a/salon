package com.salon.config;

import com.salon.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final CustomUserDetailsService customUserDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http

                .formLogin(form -> form

                        .loginPage("/login") // 커스텀 로그인 페이지 주소
                        .loginProcessingUrl("/login") // form action
                        .failureUrl("/login?error")// 로그인실패시 어떻게?
                        .usernameParameter("loginId") // input name
                        .passwordParameter("password")
                        .defaultSuccessUrl("/", true) // 성공시 리다이렉트
                        .permitAll() //로그인 페이지 모두에게 접속 허용
                )
                .rememberMe(rememberMe -> rememberMe
                        .key("unique-remember-me-key")
                        .rememberMeParameter("remember-me")
                        .tokenValiditySeconds(60 * 60 * 24 * 15)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/") // 로그아웃 성공시 메인페이지 이동
                        .invalidateHttpSession(true) // 로그아웃시 회원 세션 모두 삭제
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/","/shop/**","/shopList/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/css/**", "/images/**", "/javascript/**").permitAll()
                        .requestMatchers("/manage/**").hasAnyRole("DESIGNER", "MAIN_DESIGNER") // 디자이너 페이지
                        .requestMatchers("/master/**").hasRole("MAIN_DESIGNER") // 메인디자이너 페이지
                        .requestMatchers("/admin/**", "admin/cs/**", "admin/anc/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .userDetailsService(customUserDetailsService)
                .csrf(
                        cr -> cr
                                .ignoringRequestMatchers(
                                        "/salon/auth/email/check",
                                        "/salon/auth/email/send",
                                        "/salon/auth/email/verify",
                                        "/salon/auth/email/reset-complete",
                                        "/salon/auth/email/find-id",
                                        "/salon/master/shop-edit/update",
                                        "/salon/ws/**",  // 웹소켓
                                        "/salon/master/add-designer"
                                )
                                .csrfTokenRepository(
                                                CookieCsrfTokenRepository.withHttpOnlyFalse())
                );

        //http.formLogin(Customizer.withDefaults());


        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }




}
