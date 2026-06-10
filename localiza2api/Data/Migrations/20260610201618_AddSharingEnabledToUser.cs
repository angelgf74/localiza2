using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace localiza2api.Data.Migrations
{
    /// <inheritdoc />
    public partial class AddSharingEnabledToUser : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<bool>(
                name: "SharingEnabled",
                table: "Users",
                type: "boolean",
                nullable: false,
                defaultValue: false);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "SharingEnabled",
                table: "Users");
        }
    }
}
