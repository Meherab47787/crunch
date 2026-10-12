package com.crustcrunch.service;

import com.crustcrunch.dto.OrderDtos.OrderSummaryResponse;
import com.crustcrunch.dto.PageResponse;
import com.crustcrunch.dto.UserDtos.*;
import com.crustcrunch.entity.User;
import com.crustcrunch.enums.Role;
import com.crustcrunch.exception.BusinessRuleException;
import com.crustcrunch.exception.ConflictException;
import com.crustcrunch.exception.NotFoundException;
import com.crustcrunch.mapper.OrderMapper;
import com.crustcrunch.mapper.UserMapper;
import com.crustcrunch.repository.OrderRepository;
import com.crustcrunch.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;

@Service
@Transactional
public class AdminUserService {

    private final UserRepository users;
    private final OrderRepository orders;
    private final PasswordEncoder encoder;
    private final UserMapper userMapper;
    private final OrderMapper orderMapper;

    public AdminUserService(UserRepository users, OrderRepository orders, PasswordEncoder encoder,
                            UserMapper userMapper, OrderMapper orderMapper) {
        this.users = users;
        this.orders = orders;
        this.encoder = encoder;
        this.userMapper = userMapper;
        this.orderMapper = orderMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> customers(String q, int page, int size) {
        return PageResponse.of(users.search(EnumSet.of(Role.CUSTOMER), blank(q),
                        PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Order.desc("createdAt")))),
                this::toCustomer);
    }

    @Transactional(readOnly = true)
    public CustomerResponse customer(Long id) {
        return toCustomer(load(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> customerOrders(Long id, int page, int size) {
        load(id);
        return PageResponse.of(orders.findByUserIdOrderByCreatedAtDesc(id,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))), orderMapper::toSummary);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> staff(String q, int page, int size) {
        return PageResponse.of(users.search(EnumSet.of(Role.ADMIN, Role.KITCHEN_STAFF), blank(q),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("fullName"))), userMapper::toResponse);
    }

    public UserResponse createStaff(StaffCreateRequest req) {
        if (req.role() == Role.CUSTOMER) {
            throw new BusinessRuleException("Staff accounts must be ADMIN or KITCHEN_STAFF");
        }
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        User u = new User();
        u.setEmail(email);
        u.setFullName(req.fullName().trim());
        u.setPhone(AuthService.blankToNull(req.phone()));
        u.setPasswordHash(encoder.encode(req.password()));
        u.setRole(req.role());
        return userMapper.toResponse(users.save(u));
    }

    /** Changes role / enabled flag. Guards against an admin locking everyone out. */
    public UserResponse update(Long id, Long actingAdminId, UserAdminUpdateRequest req) {
        User u = load(id);
        boolean losingAdmin = u.getRole() == Role.ADMIN && (req.role() != Role.ADMIN || !req.enabled());
        if (losingAdmin && id.equals(actingAdminId)) {
            throw new BusinessRuleException("You cannot remove your own admin access");
        }
        if (losingAdmin && users.countByRole(Role.ADMIN) <= 1) {
            throw new BusinessRuleException("At least one administrator is required");
        }
        u.setRole(req.role());
        u.setEnabled(req.enabled());
        return userMapper.toResponse(u);
    }

    private CustomerResponse toCustomer(User u) {
        return new CustomerResponse(userMapper.toResponse(u), orders.countByUserId(u.getId()),
                orders.totalSpentByUser(u.getId()));
    }

    private User load(Long id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
    }

    private static String blank(String q) {
        return q == null || q.isBlank() ? null : q.trim();
    }
}
