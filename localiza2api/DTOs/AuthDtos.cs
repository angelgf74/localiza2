namespace localiza2api.DTOs;

public record RegisterDto(string Email, string Password, string Name);
public record LoginDto(string Email, string Password);
public record LoginResponseDto(string Token, int UserId, string Name, string Email, string Role);
public record ForgotPasswordDto(string Email);
public record ResetPasswordDto(string Token, string NewPassword);
public record SetSharingDto(bool SharingEnabled);
public record SharingStatusDto(bool SharingEnabled);
