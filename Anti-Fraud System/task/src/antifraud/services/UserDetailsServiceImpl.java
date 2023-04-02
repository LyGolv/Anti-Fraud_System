package antifraud.services;

import antifraud.dao.UserDAO;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    final UserRepository repository;
    final PasswordEncoder encoder;

    public UserDetailsServiceImpl(UserRepository repository, PasswordEncoder encoder) {
        this.repository = repository;
        this.encoder = encoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<UserDAO> optional = repository.findByUsername(username);
        if (optional.isPresent()) return toUserDetails(optional.get());
        throw new UsernameNotFoundException("user not found");
    }

    private UserDetails toUserDetails(UserDAO user) {
        return User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())))
                .accountLocked(user.getOperation().equals("LOCK"))
                .build();
    }
}
