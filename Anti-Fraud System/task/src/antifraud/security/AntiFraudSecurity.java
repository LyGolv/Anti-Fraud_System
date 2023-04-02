package antifraud.security;

import antifraud.models.Role;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;

@EnableWebSecurity
public class AntiFraudSecurity extends WebSecurityConfigurerAdapter {

    final AuthenticationEntryPoint entryPoint;
    final UserDetailsService service;
    final PasswordEncoder encoder;

    public AntiFraudSecurity(AuthenticationEntryPoint entryPoint, UserDetailsService service, PasswordEncoder encoder) {
        this.entryPoint = entryPoint;
        this.service = service;
        this.encoder = encoder;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(service).passwordEncoder(encoder);
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.httpBasic()
                .authenticationEntryPoint(entryPoint) // Handles auth error
                .and()
                .csrf().disable().headers().frameOptions().disable() // for Postman, the H2 console
                .and()
                .authorizeRequests() // manage access
                .antMatchers(HttpMethod.POST, "/api/auth/user/**").permitAll()
                .antMatchers(HttpMethod.POST, "/api/antifraud/transaction/**").hasRole(Role.MERCHANT.name())
                .antMatchers(HttpMethod.POST, "/api/antifraud/suspicious-ip/**", "/api/antifraud/stolencard/**").hasRole(Role.SUPPORT.name())
                .antMatchers(HttpMethod.DELETE, "/api/antifraud/suspicious-ip/**", "/api/antifraud/stolencard/**").hasRole(Role.SUPPORT.name())
                .antMatchers(HttpMethod.GET, "/api/antifraud/suspicious-ip/**", "/api/antifraud/stolencard/**").hasRole(Role.SUPPORT.name())
                .antMatchers(HttpMethod.GET, "/api/antifraud/history/**").hasRole(Role.SUPPORT.name())
                .antMatchers(HttpMethod.PUT, "/api/antifraud/transaction/**").hasRole(Role.SUPPORT.name())
                .antMatchers(HttpMethod.GET,"/api/auth/list/**").hasAnyRole(Role.ADMINISTRATOR.name(), Role.SUPPORT.name())
                .antMatchers(HttpMethod.DELETE, "/api/auth/user/**").hasRole(Role.ADMINISTRATOR.name())
                .antMatchers(HttpMethod.PUT, "/api/auth/access/**").hasRole(Role.ADMINISTRATOR.name())
                .antMatchers(HttpMethod.PUT, "/api/auth/role/**").hasRole(Role.ADMINISTRATOR.name())
                .antMatchers("/actuator/shutdown").permitAll() // needs to run test
                .anyRequest().denyAll()
                // other matchers
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS); // no session
    }
}