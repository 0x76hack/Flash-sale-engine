package com.flashsale.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ErrorResponse {

    private String code;
    private String message;
    private LocalDateTime timestamp;
}