using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace localiza2api.Data.Migrations
{
    public partial class AddBatteryLevelToUserLocation : Migration
    {
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<int>(
                name: "BatteryLevel",
                table: "UserLocations",
                type: "integer",
                nullable: true);
        }

        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn("BatteryLevel", "UserLocations");
        }
    }
}
