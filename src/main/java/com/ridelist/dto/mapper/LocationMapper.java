package com.ridelist.dto.mapper;

import com.ridelist.dto.response.AreaResponse;
import com.ridelist.dto.response.AxisResponse;
import com.ridelist.dto.response.StateResponse;
import com.ridelist.model.Area;
import com.ridelist.model.Axis;
import com.ridelist.model.State;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LocationMapper {

    StateResponse toStateResponse(State state);

    @Mapping(target = "state", source = "state")
    AxisResponse toAxisResponse(Axis axis);

    @Mapping(target = "axis", source = "axis")
    AreaResponse toAreaResponse(Area area);
}
