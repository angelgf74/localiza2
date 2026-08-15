namespace localiza2api.Models;

public class RefreshToken
{
    public int Id { get; set; }
    public string TokenHash { get; set; } = string.Empty; // SHA-256 hex del token en claro; nunca se guarda el valor real.
    public int UserId { get; set; }
    public User User { get; set; } = null!;
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime ExpiresAt { get; set; }
    public DateTime? RevokedAt { get; set; }

    // Hash del token que lo sustituyó al rotar. Si alguien reutiliza este token después
    // de rotado, es señal de robo: revoca toda la familia de tokens del usuario.
    public string? ReplacedByTokenHash { get; set; }
}
