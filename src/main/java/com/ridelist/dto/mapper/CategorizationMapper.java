package com.ridelist.dto.mapper;

import com.ridelist.dto.response.MakeResponse;
import com.ridelist.dto.response.ModelYearResponse;
import com.ridelist.dto.response.VehicleModelResponse;
import com.ridelist.model.Make;
import com.ridelist.model.ModelYear;
import com.ridelist.model.VehicleModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategorizationMapper {

    MakeResponse toMakeResponse(Make make);

    @Mapping(target = "make", source = "make")
    VehicleModelResponse toVehicleModelResponse(VehicleModel vehicleModel);

    @Mapping(target = "vehicleModel", source = "vehicleModel")
    ModelYearResponse toModelYearResponse(ModelYear modelYear);
}
