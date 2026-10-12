package com.crustcrunch.mapper;

import com.crustcrunch.dto.UserDtos.AddressResponse;
import com.crustcrunch.dto.UserDtos.UserResponse;
import com.crustcrunch.entity.Address;
import com.crustcrunch.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getPhone(), u.getRole(), u.isEnabled(),
                u.getCreatedAt());
    }

    public AddressResponse toResponse(Address a) {
        return new AddressResponse(a.getId(), a.getLabel(), a.getLine1(), a.getLine2(), a.getCity(), a.getPostalCode(),
                a.getInstructions(), a.isDefaultAddress());
    }
}
