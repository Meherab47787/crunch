package com.crustcrunch.service;

import com.crustcrunch.dto.UserDtos.*;
import com.crustcrunch.entity.Address;
import com.crustcrunch.entity.User;
import com.crustcrunch.exception.BadRequestException;
import com.crustcrunch.exception.BusinessRuleException;
import com.crustcrunch.exception.NotFoundException;
import com.crustcrunch.mapper.UserMapper;
import com.crustcrunch.repository.AddressRepository;
import com.crustcrunch.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private static final int MAX_ADDRESSES = 10;

    private final UserRepository users;
    private final AddressRepository addresses;
    private final PasswordEncoder encoder;
    private final UserMapper mapper;

    public UserService(UserRepository users, AddressRepository addresses, PasswordEncoder encoder, UserMapper mapper) {
        this.users = users;
        this.addresses = addresses;
        this.encoder = encoder;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long userId) {
        return mapper.toResponse(load(userId));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest req) {
        User u = load(userId);
        u.setFullName(req.fullName().trim());
        u.setPhone(AuthService.blankToNull(req.phone()));
        return mapper.toResponse(u);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest req) {
        User u = load(userId);
        if (!encoder.matches(req.currentPassword(), u.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        u.setPasswordHash(encoder.encode(req.newPassword()));
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> listAddresses(Long userId) {
        return addresses.findByUserIdOrderByDefaultAddressDescIdAsc(userId).stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public AddressResponse createAddress(Long userId, AddressRequest req) {
        List<Address> existing = addresses.findByUserIdOrderByDefaultAddressDescIdAsc(userId);
        if (existing.size() >= MAX_ADDRESSES) {
            throw new BusinessRuleException("You can save up to " + MAX_ADDRESSES + " addresses");
        }
        Address a = new Address();
        a.setUser(load(userId));
        apply(a, req);
        boolean makeDefault = req.defaultAddress() || existing.isEmpty();
        if (makeDefault) {
            addresses.clearDefault(userId);
        }
        a.setDefaultAddress(makeDefault);
        return mapper.toResponse(addresses.save(a));
    }

    @Transactional
    public AddressResponse updateAddress(Long userId, Long addressId, AddressRequest req) {
        Address a = addresses.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new NotFoundException("Address not found"));
        apply(a, req);
        if (req.defaultAddress() && !a.isDefaultAddress()) {
            addresses.clearDefault(userId);
            a.setDefaultAddress(true);
        }
        return mapper.toResponse(addresses.save(a));
    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        Address a = addresses.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new NotFoundException("Address not found"));
        addresses.delete(a);
    }

    private void apply(Address a, AddressRequest req) {
        a.setLabel(req.label().trim());
        a.setLine1(req.line1().trim());
        a.setLine2(AuthService.blankToNull(req.line2()));
        a.setCity(req.city().trim());
        a.setPostalCode(AuthService.blankToNull(req.postalCode()));
        a.setInstructions(AuthService.blankToNull(req.instructions()));
    }

    private User load(Long id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
    }
}
