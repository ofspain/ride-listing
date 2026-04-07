package com.ridelist.dto.mapper;

import com.ridelist.dto.request.RegisterRequest;
import com.ridelist.dto.response.UserResponse;
import com.ridelist.dto.response.UserSummaryResponse;
import com.ridelist.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "profileImageUrl", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "listings", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(RegisterRequest request);

    UserResponse toResponse(User user);

    UserSummaryResponse toSummaryResponse(User user);
}
