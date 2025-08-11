package com.aqryuz.auth.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DeviceDetectionService {

    private static final Pattern BROWSER_PATTERN =
            Pattern.compile("(?i)(firefox|chrome|safari|edge|opera|ie|msie).*?([\\d\\.]+)");

    private static final Pattern OS_PATTERN =
            Pattern.compile("(?i)(windows|mac|linux|android|ios|iphone|ipad)");

    private static final Pattern DEVICE_PATTERN =
            Pattern.compile("(?i)(mobile|tablet|desktop|iphone|ipad|android)");

    public DeviceInfo extractDeviceInfo(String userAgent) {
        if (userAgent == null || userAgent.trim().isEmpty()) {
            return DeviceInfo.builder().deviceName("Unknown Device").deviceType("Unknown")
                    .browserName("Unknown Browser").browserVersion("Unknown")
                    .operatingSystem("Unknown OS").build();
        }

        String browserName = extractBrowserName(userAgent);
        String browserVersion = extractBrowserVersion(userAgent);
        String operatingSystem = extractOperatingSystem(userAgent);
        String deviceType = extractDeviceType(userAgent);
        String deviceName = generateDeviceName(browserName, operatingSystem, deviceType);

        return DeviceInfo.builder().deviceName(deviceName).deviceType(deviceType)
                .browserName(browserName).browserVersion(browserVersion)
                .operatingSystem(operatingSystem).build();
    }

    public String getLocationFromIp(String ipAddress) {
        // Simple implementation - in production, you might use a GeoIP service
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return "Unknown Location";
        }

        // Check for local/private IP addresses
        if (isLocalIpAddress(ipAddress)) {
            return "Local Network";
        }

        // For demonstration purposes, return a generic location
        // In production, integrate with a service like MaxMind GeoIP2 or similar
        return "Unknown Location";
    }

    private String extractBrowserName(String userAgent) {
        if (userAgent.contains("Firefox"))
            return "Firefox";
        if (userAgent.contains("Chrome") && !userAgent.contains("Edge"))
            return "Chrome";
        if (userAgent.contains("Safari") && !userAgent.contains("Chrome"))
            return "Safari";
        if (userAgent.contains("Edge"))
            return "Edge";
        if (userAgent.contains("Opera"))
            return "Opera";
        if (userAgent.contains("MSIE") || userAgent.contains("Trident"))
            return "Internet Explorer";
        return "Unknown Browser";
    }

    private String extractBrowserVersion(String userAgent) {
        try {
            Matcher matcher = BROWSER_PATTERN.matcher(userAgent);
            if (matcher.find()) {
                return matcher.group(2);
            }
        } catch (Exception e) {
            log.warn("Error extracting browser version from user agent: {}", e.getMessage());
        }
        return "Unknown";
    }

    private String extractOperatingSystem(String userAgent) {
        if (userAgent.contains("Windows NT 10.0"))
            return "Windows 10";
        if (userAgent.contains("Windows NT 6.3"))
            return "Windows 8.1";
        if (userAgent.contains("Windows NT 6.2"))
            return "Windows 8";
        if (userAgent.contains("Windows NT 6.1"))
            return "Windows 7";
        if (userAgent.contains("Windows"))
            return "Windows";
        if (userAgent.contains("Mac OS X"))
            return "macOS";
        if (userAgent.contains("Linux"))
            return "Linux";
        if (userAgent.contains("Android"))
            return "Android";
        if (userAgent.contains("iPhone") || userAgent.contains("iOS"))
            return "iOS";
        return "Unknown OS";
    }

    private String extractDeviceType(String userAgent) {
        if (userAgent.contains("Mobile") || userAgent.contains("iPhone")
                || userAgent.contains("Android")) {
            return "Mobile";
        }
        if (userAgent.contains("Tablet") || userAgent.contains("iPad")) {
            return "Tablet";
        }
        return "Desktop";
    }

    private String generateDeviceName(String browser, String os, String deviceType) {
        if ("Mobile".equals(deviceType)) {
            if (os.contains("iOS")) {
                return "iPhone";
            } else if (os.contains("Android")) {
                return "Android Phone";
            }
            return "Mobile Device";
        } else if ("Tablet".equals(deviceType)) {
            if (os.contains("iOS")) {
                return "iPad";
            } else if (os.contains("Android")) {
                return "Android Tablet";
            }
            return "Tablet";
        } else {
            return os + " Computer";
        }
    }

    private boolean isLocalIpAddress(String ipAddress) {
        return ipAddress.startsWith("127.") || ipAddress.startsWith("192.168.")
                || ipAddress.startsWith("10.") || ipAddress.startsWith("172.")
                || ipAddress.equals("::1") || ipAddress.equals("0:0:0:0:0:0:0:1");
    }

    public static class DeviceInfo {
        private String deviceName;
        private String deviceType;
        private String browserName;
        private String browserVersion;
        private String operatingSystem;

        private DeviceInfo(Builder builder) {
            this.deviceName = builder.deviceName;
            this.deviceType = builder.deviceType;
            this.browserName = builder.browserName;
            this.browserVersion = builder.browserVersion;
            this.operatingSystem = builder.operatingSystem;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String getDeviceName() {
            return deviceName;
        }

        public String getDeviceType() {
            return deviceType;
        }

        public String getBrowserName() {
            return browserName;
        }

        public String getBrowserVersion() {
            return browserVersion;
        }

        public String getOperatingSystem() {
            return operatingSystem;
        }

        public static class Builder {
            private String deviceName;
            private String deviceType;
            private String browserName;
            private String browserVersion;
            private String operatingSystem;

            public Builder deviceName(String deviceName) {
                this.deviceName = deviceName;
                return this;
            }

            public Builder deviceType(String deviceType) {
                this.deviceType = deviceType;
                return this;
            }

            public Builder browserName(String browserName) {
                this.browserName = browserName;
                return this;
            }

            public Builder browserVersion(String browserVersion) {
                this.browserVersion = browserVersion;
                return this;
            }

            public Builder operatingSystem(String operatingSystem) {
                this.operatingSystem = operatingSystem;
                return this;
            }

            public DeviceInfo build() {
                return new DeviceInfo(this);
            }
        }
    }
}
