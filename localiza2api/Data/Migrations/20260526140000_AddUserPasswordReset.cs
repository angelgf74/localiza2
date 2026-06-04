using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace localiza2api.Data.Migrations
{
    public partial class AddUserPasswordReset : Migration
    {
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<string>(
                name: "PasswordResetToken",
                table: "Users",
                type: "character varying(64)",
                maxLength: 64,
                nullable: true);

            migrationBuilder.AddColumn<DateTime>(
                name: "PasswordResetExpiry",
                table: "Users",
                type: "timestamp with time zone",
                nullable: true);

            migrationBuilder.CreateIndex(
                name: "IX_Users_PasswordResetToken",
                table: "Users",
                column: "PasswordResetToken",
                unique: true,
                filter: "\"PasswordResetToken\" IS NOT NULL");
        }

        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropIndex("IX_Users_PasswordResetToken", "Users");
            migrationBuilder.DropColumn("PasswordResetToken", "Users");
            migrationBuilder.DropColumn("PasswordResetExpiry", "Users");
        }
    }
}
