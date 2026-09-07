# PurgaPlugin 🩸

**PurgaPlugin** es un plugin avanzado para Minecraft (Spigot / Paper / Purpur) pensado para crear eventos de Purga en servidores PvP y PvE. Durante un tiempo determinado, los jugadores se enfrentan en combate libre sin protecciones de zona, mientras aparecen oleadas de mobs asesinos especiales, se activan mecánicas de supervivencia y se transmiten los sucesos en tiempo real mediante Webhooks de Discord.

El plugin está pensado para ser 100% configurable, ligero y adaptable al estilo de cualquier modalidad (Survival, Factions, Anarquía, RPG o PvP).

---

## 📑 Tabla de Contenidos

1. [Características Principales](#-características-principales)
2. [Sistema de Purga y Temporizador](#-sistema-de-purga-y-temporizador)
3. [PvP Global y Anulación de Protecciones](#-pvp-global-y-anulación-de-protecciones)
4. [Bloqueo de Comandos de Escape](#-bloqueo-de-comandos-de-escape)
5. [Protección de Animales Pasivos](#-protección-de-animales-pasivos)
6. [Monstruos Asesinos de la Purga (Mobs OP)](#-monstruos-asesinos-de-la-purga-mobs-op)
7. [Creeper Táctico](#-creeper-táctico)
8. [El Gato Traicionero: DANIS](#-el-gato-traicionero-danis)
9. [El Lobo Guardián: JUST](#-el-lobo-guardián-just)
10. [Zombie del Jugador Caído (Espectro Vengador)](#-zombie-del-jugador-caído-espectro-vengador)
11. [Mecánica de Fallo de Tótems](#-mecánica-de-fallo-de-tótems)
12. [Tabla de Recompensas y Drops](#-tabla-de-recompensas-y-drops)
13. [Integración con Discord Webhooks](#-integración-con-discord-webhooks)
14. [Comandos y Permisos](#-comandos-y-permisos)
15. [Compatibilidad](#-compatibilidad)
16. [Estructura del Proyecto](#-estructura-del-proyecto)

---

## 🌟 Características Principales

* ⏱️ **Temporizador Flexible:** Soporta tiempos concisos o combinados (`30s`, `10m`, `1h`, `1h 30m`, `24h`, `3d`, `7d` o modo `manual`).
* 📊 **BossBar Dinámica:** Barra superior con cálculo de porcentaje en tiempo real y tiempo formateado.
* ⚔️ **PvP Forzado:** Anulación automática de cancelaciones de daño de plugins de reclamos (WorldGuard, Lands, GriefPrevention, Towny).
* 🚫 **Anti-Escape:** Bloqueo configurable de comandos de teletransporte durante el evento.
* 🐾 **Protección de Fauna:** Prohibición estricta de atacar animales durante la Purga.
* ☠️ **Oleadas Progresivas de Mobs OP:** Escalado de vida, pociones y armas chetadas con bloqueo de drop de equipamiento OP.
* 🧨 **Creeper Táctico:** Teletransporte al recibir daño y detonación instantánea por proximidad.
* 🐱 **DANIS:** Mascota engañosa que acompaña al jugador y detona con Insta-Kill que atraviesa tótems.
* 🐺 **JUST:** Lobo guardián domesticable con Netherite y Diamantes que ahuyenta a DANIS y cura a su dueño.
* 🧟 **Zombie de Jugador Muerto:** Custodia el botín del jugador caído y ciega a los compañeros cercanos.
* 🛡️ **Fallo de Tótems:** Probabilidad configurable de neutralizar el Tótem de Inmortalidad.
* 🤖 **Discord Webhooks Asíncronos:** Embeds de inicio, fin y asesinatos en tiempo real con coordenadas, mundo y arma.
* ⚡ **Optimización de Rendimiento:** Mob Cap por jugador, auto-despawn a más de 64 bloques y tareas en segundo plano.

---

## ⏱️ Sistema de Purga y Temporizador

Puedes iniciar una Purga con la duración que prefieras utilizando el comando `/purga start <tiempo>`:

```text
/purga start 30s
/purga start 10m
/purga start 1h
/purga start 1h 30m
/purga start 24h
/purga start 3d
/purga start 7d
/purga start manual
```

* Mientras el evento está activo, la **BossBar** muestra el tiempo restante con cuenta regresiva en vivo y alertas automáticas en el chat (a las 24h, 12h, 1h, 30m, 10m, 5m, 1m y cuenta final de 10 segundos).
* Al finalizar el tiempo, la Purga concluye automáticamente y restaura la paz.

---

## 🛡️ PvP Global y Anulación de Protecciones

Durante la Purga, el plugin fuerza el daño PvP entre jugadores incluso si otro plugin de protección normalmente cancela el daño.

Compatible de forma nativa con:
* **WorldGuard**
* **Lands**
* **GriefPrevention**
* **Towny**

> [!NOTE]
> Se protege de forma automática a los jugadores que se encuentren en modo **Creativo** o **Espectador** para evitar inconvenientes administrativos.

---

## 🚫 Bloqueo de Comandos de Escape

Para evitar que los jugadores evadan el combate teletransportándose a zonas seguras, el plugin bloquea comandos de escape durante la Purga:

```yaml
bloquear-comandos-durante-purga:
  - "spawn"
  - "tpa"
  - "tpaccept"
  - "tpahere"
  - "home"
  - "sethome"
  - "warp"
  - "back"
  - "rtp"
  - "wild"
```

Los administradores con el permiso `purga.bypass.commands` o con rango OP pueden omitir esta restricción.

---

## 🐮 Protección de Animales Pasivos

La Purga es un evento de enfrentamiento humano y supervivencia. Si la opción `proteger-animales: true` está activada:
* Se cancela cualquier daño (cuerpo a cuerpo o por proyectil) dirigido hacia animales pasivos (vacas, ovejas, cerdos, caballos, etc.).
* El atacante recibe un mensaje de advertencia.

---

## ☠️ Monstruos Asesinos de la Purga (Mobs OP)

Durante el evento, se generan oleadas de monstruos con equipamiento letal alrededor de los jugadores:

* **Tipos Permitidos:** Zombie, Skeleton, Zombie Villager, Vindicator, Wither Skeleton, Piglin Brute, Pillager, Witch, Creeper, Spider, Cave Spider, Enderman, Drowned, Husk, Stray, Ravager.
* **1-Hit Kill:** Los monstruos hostiles infligen daño instantáneo letal.
* **Equipamiento Indestructible:** Armaduras de Netherite/Diamante con encantamientos masivos (`Sharpness 255`, `Protection 10`, etc.).
* **Bloqueo de Drops OP:** La probabilidad de soltar sus armas y armaduras está fijada en `0.0%` para mantener la economía y el balance del servidor.
* **Escalado Progresivo:** En cada oleada aumenta la vida máxima del mob (+10 HP por oleada) y el nivel de sus pociones de Fuerza, Velocidad y Resistencia.
* **Mob Cap y Auto-Despawn:** Límite configurable de mobs vivos por jugador (evita saturación de CPU) y auto-eliminación si el jugador se aleja a más de 64 bloques.

---

## 🧨 Creeper Táctico

El Creeper de la Purga cuenta con habilidades especiales de combate:

* **Cargado Eléctrico:** Spawnea electrificado (*Charged Creeper*).
* **Teletransporte Defensivo:** Si recibe daño de un jugador o proyectil, se teletransporta inmediatamente a una posición táctica cercana.
* **Detonación Suicida Instantánea:** Si un jugador se acerca a menos de 3.5 bloques, el Creeper detona de inmediato sin tiempo de mecha.

---

## 🐱 El Gato Traicionero: DANIS

**DANIS** es uno de los mobs especiales más peligrosos del evento:

* **120 Puntos de Vida** con efectos permanentes de Resistencia y Velocidad.
* **Modo Camaleón:** Mientras la oleada está activa y hay monstruos alrededor, acompaña pacíficamente al jugador.
* **Detonación Fulminante:** Cuando queda solo 1 monstruo hostil en la zona, entra en fase de ejecución, se teletransporta al jugador y detona.
* **Bypass de Tótem:** Su explosión aplica muerte directa (`setHealth(0.0)`), **anulando totalmente la resurrección del Tótem de Inmortalidad**.
* **Vulnerabilidad:** Le teme al lobo **JUST**. Si JUST está cerca, DANIS bufa, huye y no puede detonar.

---

## 🐺 El Lobo Guardián: JUST

**JUST** es el compañero protector definitivo para sobrevivir a la Purga:

### Estadísticas
```text
Vida Máxima: 200 HP
Daño de Ataque: 12.0
```

### Domesticación Especial
Para domesticar a JUST, el jugador debe interactuar con él teniendo en su inventario:
* **2x Lingotes de Netherite**
* **2x Diamantes**

### Habilidades y Funciones
* **Límite Estricto:** Máximo 1 compañero JUST por jugador.
* **Escudo contra DANIS:** Ahuyenta al gato DANIS en un radio de 8 bloques, impidiendo que detone.
* **Regeneración de Vida:** Aplica Regeneración II periódica a su dueño en combate.
* **Asistencia en Combate:** Ataca activamente a los monstruos de la Purga.
* **Persistencia Total:** No desaparece cuando la Purga termina; permanece como mascota fiel del jugador.

---

## 🧟 Zombie del Jugador Caído (Espectro Vengador)

Cuando un jugador muere a manos de un mob o del entorno:

1. Se genera un Zombie con la **cabeza (`PLAYER_HEAD`)** y el **nombre del jugador**.
2. **Custodia de Inventario:** Si está activado, el Zombie guarda las pertenencias del jugador caído. Sus compañeros deben derrotar al Zombie para recuperar los objetos.
3. **Ola de Pánico:** Aplica efectos negativos (Ceguera, Lentitud II, Oscuridad y Debilidad) junto al sonido de latido del Warden a todos los aliados cercanos.

---

## 🛡️ Mecánica de Fallo de Tótems

El poder de la Purga puede desestabilizar los Tótems de Inmortalidad (*Totem of Undying*):

* **Probabilidad Configurable:** Por ejemplo, `40.0%` de probabilidad de fallo.
* **Efecto de Fallo:** Si el tótem falla, se consume, se cancela la resurrección (el jugador muere), se reproduce el sonido de ancla de reaparición agotada y se emite humo negro.

---

## 💎 Tabla de Recompensas y Drops

Al derrotar a los Asesinos de la Purga, los jugadores pueden obtener recompensas personalizables con probabilidades individuales:

```yaml
items:
  diamante:
    material: "DIAMOND"
    probabilidad: 60.0
    cantidad-min: 1
    cantidad-max: 3
    nombre: "&b&l✦ Diamante Purificado"
  manzana_dorada:
    material: "GOLDEN_APPLE"
    probabilidad: 35.0
    cantidad-min: 1
    cantidad-max: 2
  fragmento_netherite:
    material: "NETHERITE_SCRAP"
    probabilidad: 20.0
    cantidad-min: 1
    cantidad-max: 1
    nombre: "&6&l✦ Fragmento de la Purga"
  totem:
    material: "TOTEM_OF_UNDYING"
    probabilidad: 8.0
    cantidad-min: 1
    cantidad-max: 1
    nombre: "&e&l✦ Tótem de Supervivencia"
```

---

## 🤖 Integración con Discord Webhooks

PurgaPlugin envía notificaciones ricas mediante Webhooks de Discord de forma 100% asíncrona:

* 🚨 **Inicio del Evento:** Embed rojo con tiempo de duración, iniciador y mención `@everyone`.
* 🛡️ **Fin del Evento:** Embed verde con resumen de estadísticas y total de bajas cobradas.
* ⚔️ **Registro de Asesinatos en Vivo:** Embed con asesino, víctima, arma utilizada, mundo y coordenadas (X, Y, Z).

---

## 💻 Comandos y Permisos

| Comando | Descripción | Permiso |
| :--- | :--- | :--- |
| `/purga start <tiempo>` | Inicia la Purga con duración personalizada (ej: `30s`, `10m`, `1h 30m`, `24h`, `3d`, `manual`). | `purga.admin` |
| `/purga stop` | Detiene la Purga y elimina los monstruos hostiles del evento. | `purga.admin` |
| `/purga status` | Muestra el estado del evento, tiempo restante, oleada y bajas registradas. | `purga.admin` |
| `/purga reload` | Recarga `config.yml` en caliente sin reiniciar el servidor. | `purga.admin` |

Todos los comandos cuentan con **Tab Completion** inteligente.

---

## 🌐 Compatibilidad

### Versiones de Minecraft Soportadas

* 🟢 **Minecraft 1.21.x** (1.21.0, 1.21.1, 1.21.2, 1.21.3, 1.21.4)
* 🟢 **Minecraft 1.20.x** (1.20.0, 1.20.1, 1.20.2, 1.20.4, 1.20.5, 1.20.6)
* 🟢 **Minecraft 1.19.x** (1.19.0, 1.19.1, 1.19.2, 1.19.3, 1.19.4)
* 🟢 **Minecraft 1.18.x** (1.18.1, 1.18.2)
* 🟢 **Minecraft 1.17.x** (1.17.0, 1.17.1)
* 🟢 **Minecraft 1.16.x** (1.16.1, 1.16.2, 1.16.3, 1.16.4, 1.16.5)
* 🔴 *Versiones 1.15 e inferiores no son compatibles (requiere mecánicas de Netherite y PersistentDataContainer de 1.16+).*

### Motores de Servidor
* **Paper** *(Recomendado para máximo rendimiento)*
* **Purpur**
* **Spigot**
* **Pufferfish**
* **Tuinity**

### Versiones de Java
* **Java 17** *(Recomendado para 1.16.5 - 1.20.4)*
* **Java 21** *(Recomendado para 1.20.5+ y 1.21+)*
* **Java 25**

---

## 📂 Estructura del Proyecto

```text
PurgaPlugin/
├── pom.xml                               # Configuración de dependencias y compilación Maven
├── README.md                             # Documentación completa
├── config.yml                            # Configuración principal (>330 líneas comentadas)
├── skript/
│   └── purga.sk                          # Versión alternativa para servidores con Skript 2.6+
└── src/
    ├── main/
    │   ├── java/com/tuserver/purgaplugin/
    │   │   ├── PurgaPlugin.java          # Clase principal del plugin y gestión del ciclo de vida
    │   │   ├── TimeUtils.java            # Utilidad de parseo y formateo de tiempos
    │   │   ├── DiscordWebhook.java       # Cliente HTTP asíncrono para Webhooks de Discord
    │   │   ├── commands/
    │   │   │   └── PurgaCommand.java     # Ejecutor de comandos /purga y tab completion
    │   │   ├── listeners/
    │   │   │   └── PurgaListener.java    # Listeners de eventos de daño, muerte y domesticación
    │   │   └── mobs/
    │   │       └── PurgaMobManager.java  # Gestor de oleadas, DANIS, JUST y Creeper táctico
    │   └── resources/
    │       ├── plugin.yml                # Metadata y permisos del plugin
    │       └── config.yml                # Archivo de configuración por defecto
    └── test/
        └── java/
            └── PurgaSystemTest.java      # Suite con 203 pruebas unitarias y de estrés
```

---

## 🛠️ Instalación y Compilación

### Instalación en el Servidor
1. Coloca `PurgaPlugin-1.0.0.jar` dentro del directorio `/plugins` de tu servidor.
2. Inicia o reinicia el servidor.
3. Configura tu Webhook de Discord y ajusta los parámetros en `/plugins/PurgaPlugin/config.yml`.
4. Aplica los cambios en cualquier momento con `/purga reload`.


## 📄 Licencia

Este proyecto está disponible bajo la licencia de código abierto para la comunidad de Minecraft. Desarrollado con enfoque en rendimiento, balance y calidad de juego.
