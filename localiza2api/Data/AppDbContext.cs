using localiza2api.Models;
using Microsoft.EntityFrameworkCore;

namespace localiza2api.Data;

public class AppDbContext(DbContextOptions<AppDbContext> options) : DbContext(options)
{
    public DbSet<User> Users => Set<User>();
    public DbSet<PendingRegistration> PendingRegistrations => Set<PendingRegistration>();
    public DbSet<Contact> Contacts => Set<Contact>();
    public DbSet<ContactInvitation> ContactInvitations => Set<ContactInvitation>();
    public DbSet<UserLocation> UserLocations => Set<UserLocation>();
    public DbSet<LocationShareLink> LocationShareLinks => Set<LocationShareLink>();
    public DbSet<RefreshToken> RefreshTokens => Set<RefreshToken>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<User>(e =>
        {
            e.HasIndex(u => u.Email).IsUnique();
            e.Property(u => u.Email).HasMaxLength(256);
            e.Property(u => u.Name).HasMaxLength(100);
            e.Property(u => u.PairingCode).HasMaxLength(32);
            e.HasIndex(u => u.PairingCode).IsUnique().HasFilter("\"PairingCode\" IS NOT NULL");
            e.Property(u => u.PasswordResetToken).HasMaxLength(64);
            e.HasIndex(u => u.PasswordResetToken).IsUnique().HasFilter("\"PasswordResetToken\" IS NOT NULL");
            e.Property(u => u.Role).HasConversion<string>().HasMaxLength(20);
        });

        modelBuilder.Entity<Contact>(e =>
        {
            e.HasOne(c => c.User)
                .WithMany(u => u.Contacts)
                .HasForeignKey(c => c.UserId)
                .OnDelete(DeleteBehavior.Cascade);

            e.HasOne(c => c.ContactUser)
                .WithMany()
                .HasForeignKey(c => c.ContactUserId)
                .OnDelete(DeleteBehavior.SetNull);

            e.HasIndex(c => new { c.UserId, c.Email }).IsUnique();
            e.Property(c => c.Email).HasMaxLength(256);
            e.Property(c => c.Alias).HasMaxLength(100);
            e.Property(c => c.Status).HasConversion<string>();
        });

        modelBuilder.Entity<ContactInvitation>(e =>
        {
            e.HasOne(i => i.InviterUser)
                .WithMany()
                .HasForeignKey(i => i.InviterUserId)
                .OnDelete(DeleteBehavior.Cascade);

            e.Property(i => i.InvitedEmail).HasMaxLength(256);
        });

        modelBuilder.Entity<UserLocation>(e =>
        {
            e.HasOne(l => l.User)
                .WithMany(u => u.Locations)
                .HasForeignKey(l => l.UserId)
                .OnDelete(DeleteBehavior.Cascade);

            e.HasIndex(l => new { l.UserId, l.Timestamp });
        });

        modelBuilder.Entity<PendingRegistration>(e =>
        {
            e.HasIndex(p => p.Token).IsUnique();
            e.HasIndex(p => p.Email).IsUnique();
            e.Property(p => p.Email).HasMaxLength(256);
        });

        modelBuilder.Entity<LocationShareLink>(e =>
        {
            e.HasIndex(l => l.Token).IsUnique();
            e.Property(l => l.Token).HasMaxLength(64);
            e.HasOne(l => l.User)
                .WithMany()
                .HasForeignKey(l => l.UserId)
                .OnDelete(DeleteBehavior.Cascade);
        });

        modelBuilder.Entity<RefreshToken>(e =>
        {
            e.HasIndex(r => r.TokenHash).IsUnique();
            e.Property(r => r.TokenHash).HasMaxLength(64);
            e.Property(r => r.ReplacedByTokenHash).HasMaxLength(64);
            e.HasIndex(r => new { r.UserId, r.RevokedAt }); // Consulta de "tokens activos de un usuario" en cada refresh/reset.
            e.HasOne(r => r.User)
                .WithMany()
                .HasForeignKey(r => r.UserId)
                .OnDelete(DeleteBehavior.Cascade);
        });
    }
}
