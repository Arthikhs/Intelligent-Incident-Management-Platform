package com.iimp.incident.mapper;

import com.iimp.incident.domain.Incident;
import com.iimp.incident.dto.IncidentResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface IncidentMapper {
    IncidentResponse toResponse(Incident incident);
}
