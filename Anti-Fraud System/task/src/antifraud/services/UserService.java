package antifraud.services;

import antifraud.dao.UserDAO;
import antifraud.models.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class UserService {

    final UserRepository repository;
    final PasswordEncoder encoder;

    public UserService(UserRepository repository, PasswordEncoder encoder) {
        this.repository = repository;
        this.encoder = encoder;
    }

    public boolean isUserExist(String username) {
        return repository.findByUsername(username).isPresent();
    }

    public List<UserDAO> getAllUsers() {
        return repository.findAll().stream()
                .peek(user -> {
                    user.setPassword("");
                    user.setOperation("");
                })
                .toList();
    }

    public UserDAO saveUser(UserDAO user, boolean update) {
        if (!update) {
            user.setPassword(encoder.encode(user.getPassword()));
            String role = repository.count() == 0 ? Role.ADMINISTRATOR.name() : Role.MERCHANT.name();
            user.setRole(role);
            user.setOperation(role.equals(Role.ADMINISTRATOR.name()) ? "UNLOCK" : "LOCK");
        }
        UserDAO userDAO = repository.saveAndFlush(user);
        userDAO.setPassword("");
        userDAO.setOperation("");
        return userDAO;
    }

    public void deleteUser(String username) {
        repository.deleteById(Objects.requireNonNull(repository.findByUsername(username).orElse(null)).getId());
    }

    public UserDAO getUserByUsername(String username) {
        return repository.findByUsername(username).orElse(null);
    }
}
