namespace localiza2api.Models;

public class ContactInvitation
{
    public int Id { get; set; }
    public int InviterUserId { get; set; }
    public User InviterUser { get; set; } = null!;
    public string InvitedEmail { get; set; } = string.Empty;
    public string Token { get; set; } = string.Empty;
    public DateTime ExpiresAt { get; set; }
}
