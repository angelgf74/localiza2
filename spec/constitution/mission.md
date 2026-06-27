# Misión

_Define la razón de ser del proyecto. Es la referencia que decide si una feature "encaja" o no._

## Qué construimos

Localiza2 es una plataforma de compartición de ubicación en tiempo real que permite a grupos de amigos, familias o equipos verse mutuamente en un mapa. Resuelve el problema de coordinar encuentros, cuidar el bienestar de seres queridos y colaborar sabiendo dónde está cada quién, sin depender de apps de redes sociales ni compartir perfiles públicos.

1. **App Android** — Cliente nativo con GPS en segundo plano, resiliencia offline y optimización de batería.
2. **API REST (.NET)** — Backend con autenticación JWT, emparejamiento bilateral, historial paginado y poda automática.
3. **Dashboard web** — Interfaz SPA con mapa interactivo, sin necesidad de instalar app.

## Para quién

- **Usuarios primarios:** Amigos y familias que quieren verse en un mapa en tiempo real sin compartir perfiles públicos.
- **Cuidadores:** Padres de adolescentes o hijos de ancianos que necesitan saber dónde están sus seres queridos.
- **Equipos colaborativos:** Grupos de trabajo en campo (logística, construcción, delivery) que necesitan coordinar posiciones.
- **Desarrollador:** Proyecto personal para ejercitar full-stack (Android + .NET + web).

## Principios

- **Privacidad por defecto** — Solo ves contactos que aceptaron mutuamente tu invitación. No hay perfiles públicos ni feeds. Control total sobre quién ve tu ubicación.
- **Simplicidad y eficiencia** — Interfaz minimalista. Recolecta solo lo necesario (lat/lon/batería). No hay mensajería, fotos, ni funcionalidades secundarias. Una tarea, bien hecha.
- **Resiliencia sin intervención** — App sobrevive a caídas de red, boot del teléfono, optimización de batería OEM. Se recupera automáticamente sin que el usuario reabra la app.
- **Datos comprimidos inteligentemente** — Retiene historial con resolución escalonada: últimas 3h precisas (1 pt/min), después se comprime automáticamente. Economiza almacenamiento sin perder datos recientes.
- **Acceso equitativo** — Funciona en navegador para usuarios que no instalen la app. Compartición pública temporal (sin login) para casos puntuales.

## Qué NO es

- No es una red social — sin perfiles públicos, sin feeds, sin "descubrimiento" de usuarios.
- No es mensajería — ubicación sola, no hay chats ni notificaciones.
- No es rastreador comercial — sin publicidad, sin venta de datos, sin propósitos de marketing.
- No es SaaS empresarial — producto personal, sin soporte pago, sin SLA garantizado.
- No es centro de datos — almacenamiento limitado, poda agresiva de historial antiguos, privacidad sobre escalabilidad.
