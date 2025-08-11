package com.aqryuz.auth.dto;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Device session information")
public class DeviceSessionDto {

    @Schema(description = "Session ID", example = "1")
    private Long sessionId;

    @Schema(description = "Device name", example = "iPhone 15")
    private String deviceName;

    @Schema(description = "Device type", example = "Mobile")
    private String deviceType;

    @Schema(description = "Browser name", example = "Safari")
    private String browserName;

    @Schema(description = "Browser version", example = "17.0")
    private String browserVersion;

    @Schema(description = "Operating system", example = "iOS 17.0")
    private String operatingSystem;

    @Schema(description = "IP address", example = "192.168.1.100")
    private String ipAddress;

    @Schema(description = "Location", example = "San Francisco, CA, USA")
    private String location;

    @Schema(description = "Last activity timestamp")
    private LocalDateTime lastActivity;

    @Schema(description = "Session creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Whether this is the current session", example = "true")
    private boolean isCurrentSession;
}
