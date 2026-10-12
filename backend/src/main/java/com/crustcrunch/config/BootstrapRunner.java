package com.crustcrunch.config;

import com.crustcrunch.entity.User;
import com.crustcrunch.enums.Role;
import com.crustcrunch.repository.UserRepository;
import com.crustcrunch.service.DemoDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the initial admin / kitchen accounts from environment variables on first start.
 * Credentials are never hardcoded; if the variables are absent no account is created.
 */
@Component
public class BootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapRunner.class);

    private final AppProperties props;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final DemoDataService demoData;

    public BootstrapRunner(AppProperties props, UserRepository users, PasswordEncoder encoder,
                           DemoDataService demoData) {
        this.props = props;
        this.users = users;
        this.encoder = encoder;
        this.demoData = demoData;
    }

    /**
     * Deliberately not one transaction: each account commits on its own (UserRepository.save),
     * so staff can sign in even while demo data — which runs in its own transaction — is generating.
     */
    @Override
    public void run(ApplicationArguments args) {
        AppProperties.Bootstrap b = props.bootstrap();
        if (b == null) {
            return;
        }
        ensureUser(b.adminEmail(), b.adminPassword(), b.adminName(), Role.ADMIN);
        ensureUser(b.kitchenEmail(), b.kitchenPassword(), "Kitchen Display", Role.KITCHEN_STAFF);
        if (b.demoData()) {
            demoData.generateIfEmpty();
        }
    }

    private void ensureUser(String email, String password, String name, Role role) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return;
        }
        if (users.existsByEmailIgnoreCase(email)) {
            return;
        }
        if (password.length() < 8) {
            log.warn("Bootstrap password for {} is shorter than 8 characters; account not created", email);
            return;
        }
        User u = new User();
        u.setEmail(email.trim().toLowerCase());
        u.setFullName(name);
        u.setPasswordHash(encoder.encode(password));
        u.setRole(role);
        users.save(u);
        log.info("Created bootstrap {} account {}", role, email);
    }
}
