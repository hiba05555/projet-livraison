package com.vrp.auth.mapper;

import com.vrp.auth.dto.response.UserResponse;
import com.vrp.auth.entity.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

/**
 * MapStruct mapper – converts between {@link UserProfile} entities and DTOs.
 * {@code componentModel = "spring"} generates a Spring bean automatically.
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserMapper {

    UserResponse toResponse(UserProfile profile);

    List<UserResponse> toResponseList(List<UserProfile> profiles);
}
