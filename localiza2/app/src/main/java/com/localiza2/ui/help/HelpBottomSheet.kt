package com.localiza2.ui.help

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.localiza2.BuildConfig
import com.localiza2.databinding.BottomSheetHelpBinding
import com.localiza2.databinding.HelpSectionBinding

class HelpBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetHelpBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetHelpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Expandir el sheet al máximo al abrirse
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }

        fillSection(binding.sectionQuees,
            title = "¿Qué es localiza2?",
            body  = "localiza2 es una aplicación para compartir tu ubicación en tiempo real " +
                    "con personas de confianza.\n\n" +
                    "Tú decides con quién compartes tu posición. Solo tus contactos aceptados " +
                    "pueden verte en el mapa, y tú solo ves a quienes también te han aceptado a ti.\n\n" +
                    "El servicio se ejecuta en segundo plano para que tu posición esté siempre " +
                    "actualizada, incluso con la pantalla apagada.\n\n" +
                    "En la lista de contactos verás para cada persona:\n" +
                    "● Indicador de frescura — un punto de color que muestra cuándo fue la última " +
                    "actualización: verde (< 5 min), ámbar (< 30 min) o gris (> 30 min).\n" +
                    "● Distancia — kilómetros o metros que os separan en este momento.\n" +
                    "● Batería — nivel de batería actual de su dispositivo.\n" +
                    "● Ruta — pulsa el icono ⏱ para ver el historial de movimiento del contacto " +
                    "en el mapa."
        )

        fillSection(binding.sectionInfo,
            title = "Información que se maneja",
            body  = "• Correo electrónico — para identificarte y gestionar tu cuenta.\n\n" +
                    "• Nombre o alias — para que tus contactos te reconozcan.\n\n" +
                    "• Ubicación GPS — latitud, longitud y precisión, enviadas periódicamente " +
                    "mientras el servicio está activo.\n\n" +
                    "• Nivel de batería — el porcentaje de batería del dispositivo en el momento " +
                    "de cada actualización de posición, visible para tus contactos.\n\n" +
                    "Las posiciones se conservan con la siguiente política de retención:\n" +
                    "  – Últimas 3 h: 1 punto por minuto\n" +
                    "  – De 3 h a 24 h: 1 punto cada 5 minutos\n" +
                    "  – De 1 día a 30 días: 1 punto cada 30 minutos\n" +
                    "  – Más de 30 días: se eliminan automáticamente\n\n" +
                    "Ningún dato se vende ni se comparte con terceros. El servidor es propio " +
                    "y no usa servicios de analítica externos."
        )

        fillSection(binding.sectionPair,
            title = "Sistema de emparejamiento",
            body  = "Para compartir ubicación con alguien, ambas personas deben aceptar el " +
                    "emparejamiento. Hay dos formas de iniciarlo:\n\n" +
                    "📱 Código QR — pulsa el botón \"+\" en la pantalla de contactos y " +
                    "selecciona «Código QR». Muestra el código al otro usuario para que lo " +
                    "escanee. El código caduca en 15 minutos.\n\n" +
                    "✉️ Por email — introduce el correo de tu contacto y un alias. Le llegará " +
                    "un correo con un enlace para aceptar la invitación. El enlace lleva a la " +
                    "aplicación web donde puede registrarse o iniciar sesión.\n\n" +
                    "Hasta que ambas partes acepten, ninguna puede ver la ubicación de la otra."
        )

        fillSection(binding.sectionWeb,
            title = "Aplicación web",
            body  = "localiza2 también tiene versión web, accesible desde cualquier " +
                    "navegador:\n\n" +
                    "🌐 localiza2-app.angelgf.com.es\n\n" +
                    "Desde la web puedes:\n" +
                    "• Ver el mapa con las posiciones de tus contactos.\n" +
                    "• Gestionar contactos y aceptar invitaciones.\n" +
                    "• Usar la misma cuenta que en la app móvil.\n\n" +
                    "El enlace de invitación por email lleva directamente a la web, " +
                    "donde el nuevo usuario puede crear su cuenta y aceptar el emparejamiento."
        )

        fillSection(binding.sectionPrivacy,
            title = "Privacidad y seguridad",
            body  = "• Las comunicaciones entre la app y el servidor usan HTTPS.\n\n" +
                    "• Las contraseñas se guardan cifradas con bcrypt.\n\n" +
                    "• Puedes dejar de compartir tu ubicación en cualquier momento " +
                    "desactivando el servicio desde la notificación.\n\n" +
                    "• Puedes eliminar un contacto en cualquier momento; dejará de ver " +
                    "tu posición de forma inmediata.\n\n" +
                    "• Consulta la política de privacidad completa en:\n" +
                    "localiza2-app.angelgf.com.es/privacy.html"
        )

        binding.tvVersion.text = "localiza2 v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    }

    private fun fillSection(section: HelpSectionBinding, title: String, body: String) {
        section.tvSectionTitle.text = title
        section.tvSectionBody.text = body
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = HelpBottomSheet()
    }
}
