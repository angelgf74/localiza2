namespace localiza2api.DTOs;

public record InviteContactDto(string Email, string Alias);
public record UpdateContactDto(string Alias, string? PhotoUrl);
public record AcceptInvitationDto(string Token);

public record ContactDto(
    int Id,
    string Email,
    string Alias,
    string? PhotoUrl,
    string Status,
    bool LocationPermissionGranted,
    int? ContactUserId
);

public record AcceptPairingDto(string Token);
