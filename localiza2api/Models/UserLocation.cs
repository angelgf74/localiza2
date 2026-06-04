namespace localiza2api.Models;

public class UserLocation
{
    public int Id { get; set; }
    public int UserId { get; set; }
    public User User { get; set; } = null!;
    public double Latitude { get; set; }
    public double Longitude { get; set; }
    public double? Accuracy { get; set; }
    public int? BatteryLevel { get; set; }
    public DateTime Timestamp { get; set; } = DateTime.UtcNow;
}
