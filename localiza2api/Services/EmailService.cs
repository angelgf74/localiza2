using System.Text;
using System.Text.Json;

namespace localiza2api.Services;

public class EmailService(IConfiguration config, IHttpClientFactory httpClientFactory, ILogger<EmailService> logger)
{
    private static readonly JsonSerializerOptions JsonOptions = new() { PropertyNamingPolicy = JsonNamingPolicy.CamelCase };

    public async Task SendConfirmationEmailAsync(string toEmail, string name, string token)
    {
        var baseUrl = config["App:BaseUrl"];
        var link = $"{baseUrl}/api/auth/confirm/{token}";
        var html = $"""
            <h2 style="margin:0 0 16px">Bienvenido a localiza2, {name}</h2>
            <p>Gracias por registrarte. Para activar tu cuenta y empezar a usar localiza2, confirma tu dirección de correo electrónico pulsando el botón de abajo.</p>
            <h3 style="margin:24px 0 8px;font-size:15px;color:#1e293b">¿Qué es localiza2?</h3>
            <p>localiza2 es una aplicación gratuita para <strong>compartir tu ubicación en tiempo real</strong> con las personas de tu confianza: familia, amigos o compañeros de trabajo.</p>
            <p>Con localiza2 puedes:</p>
            <ul style="padding-left:20px;margin:8px 0 16px">
              <li>Ver en el mapa dónde están tus contactos en cada momento.</li>
              <li>Compartir tu posición solo con quienes tú elijas, de forma segura y privada.</li>
              <li>Invitar a nuevos contactos directamente desde la app con un simple enlace por email.</li>
              <li>Controlar en todo momento si quieres estar visible o no — la privacidad siempre es tuya.</li>
            </ul>
            <p>Puedes usar localiza2 desde la <strong>app Android</strong> (con actualización de ubicación en segundo plano) o directamente desde el <strong>navegador web</strong>, sin necesidad de instalar nada.</p>
            <p style="margin:24px 0">
              <a href="{link}" style="background:#3b82f6;color:#fff;padding:12px 24px;border-radius:8px;text-decoration:none;font-weight:600;display:inline-block">Confirmar mi cuenta</a>
            </p>
            <p>Si el botón no funciona, copia y pega este enlace en tu navegador:</p>
            <p style="word-break:break-all;font-size:13px;color:#64748b">{link}</p>
            <p style="margin-top:24px;color:#64748b">El enlace es válido durante <strong>24 horas</strong>. Si no creaste esta cuenta, puedes ignorar este correo sin problema.</p>
            """;
        var text = $"""
            Bienvenido a localiza2, {name}

            Gracias por registrarte. Para activar tu cuenta confirma tu dirección de correo accediendo al siguiente enlace:

            {link}

            ¿Qué es localiza2?
            localiza2 es una aplicación gratuita para compartir tu ubicación en tiempo real con las personas de tu confianza: familia, amigos o compañeros de trabajo.

            Con localiza2 puedes:
            - Ver en el mapa dónde están tus contactos en cada momento.
            - Compartir tu posición solo con quienes tú elijas, de forma segura y privada.
            - Invitar a nuevos contactos directamente desde la app con un simple enlace por email.
            - Controlar en todo momento si quieres estar visible o no.

            Disponible como app Android y desde el navegador web, sin instalar nada.

            El enlace de confirmación es válido durante 24 horas. Si no creaste esta cuenta, ignora este correo.
            """;
        await SendEmailAsync(toEmail, "Confirma tu cuenta en localiza2", html, text);
    }

    public async Task SendContactInvitationEmailAsync(string toEmail, string inviterName, string token, bool isRegistered)
    {
        var baseUrl = config["App:BaseUrl"];
        var webUrl = config["App:WebUrl"] ?? baseUrl.Replace("/localiza2api", "/localiza2");
        var link = $"{webUrl}/pair.html?token={token}";
        string html, text;
        if (isRegistered)
        {
            html = $"""
                <h2 style="margin:0 0 16px">{inviterName} quiere compartir su ubicación contigo</h2>
                <p>{inviterName} te ha invitado a conectar en localiza2 para que podáis ver vuestras ubicaciones en tiempo real.</p>
                <p>Al aceptar la invitación, {inviterName} podrá ver tu posición en el mapa y tú podrás ver la suya. Tú decides en todo momento si quieres compartir tu ubicación.</p>
                <p style="margin:24px 0">
                  <a href="{link}" style="background:#3b82f6;color:#fff;padding:12px 24px;border-radius:8px;text-decoration:none;font-weight:600;display:inline-block">Aceptar invitación</a>
                </p>
                <p style="word-break:break-all;font-size:13px;color:#64748b">{link}</p>
                <p style="margin-top:24px;color:#64748b">Si rechazas la invitación, {inviterName} no podrá ver tu posición. La invitación caduca en 7 días.</p>
                """;
            text = $"""
                {inviterName} quiere compartir su ubicación contigo en localiza2

                {inviterName} te ha invitado a conectar en localiza2. Al aceptar, podréis ver vuestras ubicaciones en tiempo real.

                Acepta la invitación aquí:
                {link}

                Si rechazas, {inviterName} no podrá ver tu posición. La invitación caduca en 7 días.
                """;
        }
        else
        {
            html = $"""
                <h2 style="margin:0 0 16px">{inviterName} te invita a usar localiza2</h2>
                <p>localiza2 es una aplicación gratuita para compartir tu ubicación en tiempo real con personas de confianza: familia, amigos o compañeros.</p>
                <p>{inviterName} quiere conectar contigo para poder ver vuestras posiciones en el mapa. Tú decides en todo momento si quieres compartir tu ubicación y con quién.</p>
                <p style="margin:24px 0">
                  <a href="{link}" style="background:#3b82f6;color:#fff;padding:12px 24px;border-radius:8px;text-decoration:none;font-weight:600;display:inline-block">Unirme y aceptar invitación</a>
                </p>
                <p style="word-break:break-all;font-size:13px;color:#64748b">{link}</p>
                <p style="margin-top:24px;color:#64748b">La invitación caduca en 7 días.</p>
                """;
            text = $"""
                {inviterName} te invita a usar localiza2

                localiza2 es una app gratuita para compartir tu ubicación en tiempo real con personas de confianza.

                {inviterName} quiere conectar contigo. Únete aquí:
                {link}

                La invitación caduca en 7 días.
                """;
        }
        await SendEmailAsync(toEmail, $"{inviterName} te invita a localiza2", html, text);
    }

    public async Task SendPasswordResetEmailAsync(string toEmail, string name, string token)
    {
        var webUrl = config["App:WebUrl"] ?? config["App:BaseUrl"]!.Replace("/localiza2api", "/localiza2");
        var link = $"{webUrl}/pair.html?reset={token}";
        var html = $"""
            <h2 style="margin:0 0 16px">Restablecer contraseña — localiza2</h2>
            <p>Hola {name}, recibimos una solicitud para restablecer la contraseña de tu cuenta. Si fuiste tú, pulsa el botón de abajo para establecer una nueva contraseña.</p>
            <p style="margin:24px 0">
              <a href="{link}" style="background:#3b82f6;color:#fff;padding:12px 24px;border-radius:8px;text-decoration:none;font-weight:600;display:inline-block">Establecer nueva contraseña</a>
            </p>
            <p style="word-break:break-all;font-size:13px;color:#64748b">{link}</p>
            <p style="margin-top:24px;color:#64748b">El enlace es válido durante <strong>1 hora</strong>. Si no solicitaste este cambio, ignora este correo; tu contraseña actual no se ha modificado.</p>
            """;
        var text = $"""
            Restablecer contraseña — localiza2

            Hola {name}, recibimos una solicitud para restablecer tu contraseña.

            Establece una nueva contraseña aquí:
            {link}

            El enlace es válido durante 1 hora. Si no solicitaste este cambio, ignora este correo.
            """;
        await SendEmailAsync(toEmail, "Restablecer contraseña en localiza2", html, text);
    }

    private static string WrapHtml(string body) => $"""
        <div style="font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;max-width:520px;margin:0 auto;color:#1e293b;font-size:15px;line-height:1.6">
          {body}
          <hr style="margin:32px 0;border:none;border-top:1px solid #e2e8f0"/>
          <p style="font-size:12px;color:#94a3b8;line-height:1.5">
            Este mensaje ha sido enviado automáticamente por localiza2.<br/>
            Por favor, <strong>no respondas a este correo</strong> — la dirección de envío no está supervisada y los mensajes recibidos no serán leídos.<br/>
            Para cualquier consulta escríbenos a <a href="mailto:angelgf@gmail.com" style="color:#3b82f6">angelgf@gmail.com</a>.
          </p>
        </div>
        """;

    private static string WrapText(string body) => $"""
        {body}

        ---
        Este mensaje ha sido enviado automáticamente por localiza2.
        Por favor, no respondas a este correo — la dirección de envío no está supervisada.
        Para cualquier consulta: angelgf@gmail.com
        """;

    private async Task SendEmailAsync(string toEmail, string subject, string htmlBody, string textBody)
    {
        try
        {
            var payload = new
            {
                sender = new
                {
                    name = config["Email:FromName"] ?? "localiza2",
                    email = config["Email:FromAddress"]
                },
                to = new[] { new { email = toEmail } },
                subject,
                htmlContent = WrapHtml(htmlBody),
                textContent = WrapText(textBody)
            };

            var json = JsonSerializer.Serialize(payload, JsonOptions);
            var request = new HttpRequestMessage(HttpMethod.Post, "https://api.brevo.com/v3/smtp/email")
            {
                Content = new StringContent(json, Encoding.UTF8, "application/json")
            };
            request.Headers.Add("api-key", config["Email:BrevoApiKey"]);

            var client = httpClientFactory.CreateClient();
            var response = await client.SendAsync(request);

            if (!response.IsSuccessStatusCode)
            {
                var error = await response.Content.ReadAsStringAsync();
                logger.LogError("Brevo API error {Status} sending to {Email}: {Error}", response.StatusCode, toEmail, error);
                throw new InvalidOperationException($"Brevo API returned {response.StatusCode}");
            }
        }
        catch (Exception ex) when (ex is not InvalidOperationException)
        {
            logger.LogError(ex, "Error sending email to {Email}", toEmail);
            throw;
        }
    }
}
