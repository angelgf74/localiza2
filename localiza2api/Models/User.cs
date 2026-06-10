namespace localiza2api.Models;

public class User
{
    public int Id { get; set; }
    public string Email { get; set; } = string.Empty;
    public string PasswordHash { get; set; } = string.Empty;
    public string Name { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public string? PairingCode { get; set; }
    public DateTime? PairingCodeExpiry { get; set; }
    public string? PasswordResetToken { get; set; }
    public DateTime? PasswordResetExpiry { get; set; }
    public bool SharingEnabled { get; set; } = true;

    public ICollection<Contact> Contacts { get; set; } = [];
    public ICollection<UserLocation> Locations { get; set; } = [];
}
