namespace localiza2api.Models;

public class Contact
{
    public int Id { get; set; }
    public int UserId { get; set; }
    public User User { get; set; } = null!;
    public int? ContactUserId { get; set; }
    public User? ContactUser { get; set; }
    public string Email { get; set; } = string.Empty;
    public string Alias { get; set; } = string.Empty;
    public string? PhotoUrl { get; set; }
    public ContactStatus Status { get; set; } = ContactStatus.Pending;
    public bool LocationPermissionGranted { get; set; } = false;
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}

public enum ContactStatus
{
    Pending,
    Accepted,
    Declined
}
