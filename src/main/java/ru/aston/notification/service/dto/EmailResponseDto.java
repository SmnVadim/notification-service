package ru.aston.notification.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmailResponseDto {
    private String email;
    private UserEvent.Operation operation;
    private String status;
    private LocalDateTime sentAt;
}
