package com.iimp.incident.dto;

import com.iimp.incident.domain.Incident;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateIncidentRequest {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private Incident.Severity severity;

    @NotNull
    private List<String> affectedServices;

    private List<String> tags;
}
