package com.geojit.contractnote.service;

import com.geojit.contractnote.dto.request.UserRequest;
import com.geojit.contractnote.entity.User;
import com.geojit.contractnote.exception.*;
import com.geojit.contractnote.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<User> getAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public User getById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", id));
    }

    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    @Transactional
    public User create(UserRequest req) {
        if (userRepository.existsByEmail(req.getEmail()))
            throw new ValidationException("Email already registered: " + req.getEmail());

        User user = User.builder()
                .email(req.getEmail())
                .name(req.getName())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(req.getRole())
                .isActive(true)
                .build();
        return userRepository.save(user);
    }

    @Transactional
    public User update(UUID id, UserRequest req) {
        User user = getById(id);
        user.setName(req.getName());
        user.setRole(req.getRole());
        if (req.getPassword() != null && !req.getPassword().isBlank())
            user.setPassword(passwordEncoder.encode(req.getPassword()));
        return userRepository.save(user);
    }

    @Transactional
    public void deactivate(UUID id) {
        User user = getById(id);
        user.setActive(false);
        userRepository.save(user);
        log.info("User deactivated | userId={} | email={}", id, user.getEmail());
    }
}
