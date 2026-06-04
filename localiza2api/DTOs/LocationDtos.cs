namespace localiza2api.DTOs;

public record UpdateLocationDto(double Latitude, double Longitude, double? Accuracy, int? BatteryLevel);

public record ContactLocationDto(
    int ContactId,
    string Alias,
    string? PhotoUrl,
    double Latitude,
    double Longitude,
    double? Accuracy,
    DateTime Timestamp,
    int? BatteryLevel
);

public record LocationPointDto(double Latitude, double Longitude, double? Accuracy, DateTime Timestamp);
