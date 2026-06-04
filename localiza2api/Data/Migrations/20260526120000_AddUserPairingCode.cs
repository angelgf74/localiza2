using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace localiza2api.Data.Migrations
{
    public partial class AddUserPairingCode : Migration
    {
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<string>(
                name: "PairingCode",
                table: "Users",
                type: "character varying(32)",
                maxLength: 32,
                nullable: true);

            migrationBuilder.AddColumn<DateTime>(
                name: "PairingCodeExpiry",
                table: "Users",
                type: "timestamp with time zone",
                nullable: true);

            migrationBuilder.CreateIndex(
                name: "IX_Users_PairingCode",
                table: "Users",
                column: "PairingCode",
                unique: true,
                filter: "\"PairingCode\" IS NOT NULL");
        }

        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropIndex("IX_Users_PairingCode", "Users");
            migrationBuilder.DropColumn("PairingCode", "Users");
            migrationBuilder.DropColumn("PairingCodeExpiry", "Users");
        }
    }
}
