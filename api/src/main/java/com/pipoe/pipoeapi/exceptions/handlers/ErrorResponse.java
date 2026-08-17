package com.pipoe.pipoeapi.exceptions.handlers;

import java.time.LocalDateTime;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ErrorResponse {
    private Integer status;
    private Object message;
    private LocalDateTime timestamp;
}
