package com.ridelist.dto.mapper;

import com.ridelist.dto.response.ContactRequestResponse;
import com.ridelist.model.ContactRequest;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = {ListingMapper.class, UserMapper.class})
public interface ContactRequestMapper {

    ContactRequestResponse toResponse(ContactRequest contactRequest);

    List<ContactRequestResponse> toResponseList(List<ContactRequest> contactRequests);
}
