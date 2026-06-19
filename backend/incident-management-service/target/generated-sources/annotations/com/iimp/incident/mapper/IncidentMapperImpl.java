package com.iimp.incident.mapper;

import com.iimp.incident.domain.Incident;
import com.iimp.incident.dto.IncidentResponse;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-19T10:45:12+0530",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class IncidentMapperImpl implements IncidentMapper {

    @Override
    public IncidentResponse toResponse(Incident incident) {
        if ( incident == null ) {
            return null;
        }

        IncidentResponse.IncidentResponseBuilder incidentResponse = IncidentResponse.builder();

        incidentResponse.acknowledgedAt( incident.getAcknowledgedAt() );
        List<String> list = incident.getAffectedServices();
        if ( list != null ) {
            incidentResponse.affectedServices( new ArrayList<String>( list ) );
        }
        incidentResponse.alertCount( incident.getAlertCount() );
        incidentResponse.createdAt( incident.getCreatedAt() );
        incidentResponse.description( incident.getDescription() );
        incidentResponse.detectedAt( incident.getDetectedAt() );
        incidentResponse.errorCount( incident.getErrorCount() );
        incidentResponse.id( incident.getId() );
        incidentResponse.impactedUsers( incident.getImpactedUsers() );
        incidentResponse.mttrSeconds( incident.getMttrSeconds() );
        incidentResponse.resolvedAt( incident.getResolvedAt() );
        incidentResponse.severity( incident.getSeverity() );
        incidentResponse.status( incident.getStatus() );
        List<String> list1 = incident.getTags();
        if ( list1 != null ) {
            incidentResponse.tags( new ArrayList<String>( list1 ) );
        }
        incidentResponse.title( incident.getTitle() );
        incidentResponse.updatedAt( incident.getUpdatedAt() );

        return incidentResponse.build();
    }
}
