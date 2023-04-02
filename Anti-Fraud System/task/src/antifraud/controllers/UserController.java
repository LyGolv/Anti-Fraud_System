package antifraud.controllers;

import antifraud.dao.UserDAO;
import antifraud.dto.OperationDTO;
import antifraud.dto.RecordStatus;
import antifraud.dto.RoleDTO;
import antifraud.models.Role;
import antifraud.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping("/user")
    public ResponseEntity<UserDAO> authenticate(@RequestBody @Valid UserDAO user) {
        if (service.isUserExist(user.getUsername()))
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.saveUser(user, false));
    }

    @GetMapping("/list")
    public List<UserDAO> getUsersList() {
        return service.getAllUsers();
    }

    @DeleteMapping("/user/{username}")
    public ResponseEntity<?> deleteUser(@PathVariable String username) {
        if (!service.isUserExist(username))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        service.deleteUser(username);
        return ResponseEntity.ok(Map.of("username", username, "status", "Deleted successfully!"));
    }

    @PutMapping("/role")
    public ResponseEntity<UserDAO> changeRole(@RequestBody RoleDTO roleDTO) {
        UserDAO userDAO = service.getUserByUsername(roleDTO.username());
        if (userDAO == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        else if (!(roleDTO.role().equals(Role.MERCHANT.name()) || roleDTO.role().equals(Role.SUPPORT.name())))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        else if (userDAO.getRole().equals(roleDTO.role()))
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        userDAO.setRole(roleDTO.role());
        service.saveUser(userDAO, true);
        return ResponseEntity.ok(userDAO);
    }

    @PutMapping("/access")
    public ResponseEntity<RecordStatus> changeRole(@RequestBody OperationDTO operationDTO) {
        UserDAO userDAO = service.getUserByUsername(operationDTO.username());
        if (userDAO == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        else if (userDAO.getRole().equals(Role.ADMINISTRATOR.name()))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        userDAO.setOperation(operationDTO.operation());
        service.saveUser(userDAO, true);
        return ResponseEntity.ok(new RecordStatus("User "
                + userDAO.getUsername() + " "
                + operationDTO.operation().toLowerCase() + "ed!"));
    }
}
