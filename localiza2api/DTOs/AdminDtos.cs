namespace localiza2api.DTOs;

public record AdminUserDto(
    int Id,
    string Email,
    string Name,
    DateTime CreatedAt,
    bool SharingEnabled,
    DateTime? LastLocationAt
);

public record AdminLocationPointDto(
    double Latitude,
    double Longitude,
    double? Accuracy,
    int? BatteryLevel,
    DateTime Timestamp
);
