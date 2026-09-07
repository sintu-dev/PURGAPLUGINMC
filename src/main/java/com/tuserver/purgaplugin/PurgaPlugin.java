package com.tuserver.purgaplugin;

import com.tuserver.purgaplugin.commands.PurgaCommand;
import com.tuserver.purgaplugin.listeners.PurgaListener;
import com.tuserver.purgaplugin.mobs.PurgaMobManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public class PurgaPlugin extends JavaPlugin {

    private boolean purgaActiva = false;
    private int muertesEnPurga = 0;
    private BossBar bossBar;
    private BukkitTask temporizadorTask;
    private BukkitTask oleadasTask;
    private DiscordWebhook discordWebhook;
    private PurgaMobManager mobManager;

    private long duracionTotalSegundos = 0;
    private long tiempoRestanteSegundos = 0;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.discordWebhook = new DiscordWebhook(this);
        this.mobManager = new PurgaMobManager(this);

        // Registrar comando y tab completer
        PurgaCommand purgaCmd = new PurgaCommand(this);
        if (getCommand("purga") != null) {
            getCommand("purga").setExecutor(purgaCmd);
            getCommand("purga").setTabCompleter(purgaCmd);
        }

        // Registrar listener de eventos
        getServer().getPluginManager().registerEvents(new PurgaListener(this), this);

        getLogger().info("PurgaPlugin activado exitosamente.");
    }

    @Override
    public void onDisable() {
        if (purgaActiva) {
            detenerPurga("Consola / Servidor apagándose", false);
        }
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }

    public void iniciarPurga(String iniciador, long duracionSegundos) {
        if (duracionSegundos < 0) duracionSegundos = 0;
        if (purgaActiva) return;

        this.purgaActiva = true;
        this.muertesEnPurga = 0;
        this.duracionTotalSegundos = duracionSegundos;
        this.tiempoRestanteSegundos = this.duracionTotalSegundos;

        // Sonido y Broadcast
        reproducirSonido("efectos.sonido-inicio");
        enviarBroadcastConfig("mensajes.inicio-broadcast");

        // Configurar BossBar
        if (getConfig().getBoolean("efectos.bossbar.activado", true)) {
            BarColor color = BarColor.valueOf(getConfig().getString("efectos.bossbar.color", "RED").toUpperCase());
            BarStyle style = BarStyle.valueOf(getConfig().getString("efectos.bossbar.estilo", "SOLID").toUpperCase());
            
            String tituloInicial = formatearTextoBossBar(getConfig().getString("efectos.bossbar.texto", "&4&l¡LA PURGA ESTÁ ACTIVA! &7| &cTiempo: &e%tiempo%"));
            if (bossBar == null) {
                bossBar = Bukkit.createBossBar(tituloInicial, color, style);
            } else {
                bossBar.setTitle(tituloInicial);
                bossBar.setColor(color);
                bossBar.setStyle(style);
            }
            bossBar.setProgress(1.0);
            bossBar.setVisible(true);
            for (Player p : Bukkit.getOnlinePlayers()) {
                bossBar.addPlayer(p);
                p.sendTitle(colorize("&4&l¡LA PURGA!"), colorize("&cEl PvP general está activo"), 10, 70, 20);
            }
        }

        // Discord Webhook
        String duracionTexto = TimeUtils.formatearTiempo(duracionSegundos);
        discordWebhook.enviarEmbed(
                "🚨 ¡LA PURGA HA COMENZADO! 🚨",
                "El PvP general ha sido activado en todo el servidor. Las protecciones no te salvarán. ¡Lucha por tu vida!",
                15158332, // Rojo #E74C3C
                "Iniciado por / Duración",
                "👤 **Iniciado por:** " + iniciador + "\n⏱️ **Duración:** " + duracionTexto
        );

        // Resetear oleadas e iniciar monitoreo de Creepers
        mobManager.resetOleadas();
        mobManager.iniciarMonitoreoCreepers();

        // Generar primera oleada de Mobs OP
        mobManager.spawnWaveForAllPlayers();

        // Programar oleadas periódicas si están habilitadas
        boolean oleadasActivas = getConfig().getBoolean("mobs-op.dificultad.oleadas-activadas", getConfig().getBoolean("mobs-op.oleadas-activadas", true));
        if (oleadasActivas) {
            long intervaloSegs = getConfig().getLong("mobs-op.dificultad.intervalo-oleadas-segundos", getConfig().getLong("mobs-op.intervalo-oleadas-segundos", 180));
            long intervaloTicks = intervaloSegs * 20L;
            if (intervaloTicks > 0) {
                this.oleadasTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
                    if (purgaActiva) {
                        mobManager.spawnWaveForAllPlayers();
                    }
                }, intervaloTicks, intervaloTicks);
            }
        }

        // Temporizador si se especificó tiempo
        if (duracionSegundos > 0) {
            this.temporizadorTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
                tiempoRestanteSegundos--;

                // Actualizar BossBar en tiempo real
                if (bossBar != null && getConfig().getBoolean("efectos.bossbar.activado", true)) {
                    String baseTexto = getConfig().getString("efectos.bossbar.texto", "&4&l¡LA PURGA ESTÁ ACTIVA! &7| &cTiempo: &e%tiempo%");
                    bossBar.setTitle(formatearTextoBossBar(baseTexto));
                    double progreso = Math.max(0.0, Math.min(1.0, (double) tiempoRestanteSegundos / duracionTotalSegundos));
                    bossBar.setProgress(progreso);
                }

                // Alertas de tiempo en el chat
                String pref = getConfig().getString("mensajes.prefijo", "&8[&4&lPURGA&8] ");
                if (tiempoRestanteSegundos == 86400) { // 24h
                    Bukkit.broadcastMessage(colorize(pref + "&e¡Quedan 24 horas para que finalice la Purga!"));
                } else if (tiempoRestanteSegundos == 43200) { // 12h
                    Bukkit.broadcastMessage(colorize(pref + "&e¡Quedan 12 horas para que finalice la Purga!"));
                } else if (tiempoRestanteSegundos == 3600) { // 1h
                    Bukkit.broadcastMessage(colorize(pref + "&e¡Queda 1 hora para que finalice la Purga!"));
                } else if (tiempoRestanteSegundos == 1800) { // 30 min
                    Bukkit.broadcastMessage(colorize(pref + "&e¡Quedan 30 minutos de Purga!"));
                } else if (tiempoRestanteSegundos == 600) { // 10 min
                    Bukkit.broadcastMessage(colorize(pref + "&e¡Quedan 10 minutos para que finalice la Purga!"));
                } else if (tiempoRestanteSegundos == 300) { // 5 min
                    Bukkit.broadcastMessage(colorize(pref + "&e¡Quedan 5 minutos para que finalice la Purga!"));
                } else if (tiempoRestanteSegundos == 60) { // 1 min
                    Bukkit.broadcastMessage(colorize(pref + "&c¡Último minuto de Purga!"));
                } else if (tiempoRestanteSegundos == 10 || tiempoRestanteSegundos == 5 || tiempoRestanteSegundos == 3 || tiempoRestanteSegundos == 2 || tiempoRestanteSegundos == 1) {
                    Bukkit.broadcastMessage(colorize(pref + "&cLa Purga termina en &e" + tiempoRestanteSegundos + " &csegundos..."));
                }

                if (tiempoRestanteSegundos <= 0) {
                    detenerPurga("Tiempo límite expirado", true);
                }
            }, 20L, 20L);
        }
    }

    private String formatearTextoBossBar(String textoBase) {
        String tiempoStr = TimeUtils.formatearParaBossBar(tiempoRestanteSegundos);
        return colorize(textoBase.replace("%tiempo%", tiempoStr));
    }

    public void detenerPurga(String finalizador, boolean avisarDiscord) {
        if (!purgaActiva) return;

        this.purgaActiva = false;
        if (temporizadorTask != null) {
            temporizadorTask.cancel();
            temporizadorTask = null;
        }

        if (oleadasTask != null) {
            oleadasTask.cancel();
            oleadasTask = null;
        }

        if (bossBar != null) {
            bossBar.removeAll();
        }

        // Limpiar mobs OP si está configurado
        mobManager.limpiarMobsDePurga();

        reproducirSonido("efectos.sonido-fin");
        enviarBroadcastConfig("mensajes.fin-broadcast");

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(colorize("&a&lPURGA FINALIZADA"), colorize("&7El orden ha sido restaurado"), 10, 70, 20);
        }

        if (avisarDiscord) {
            discordWebhook.enviarEmbed(
                    "🛡️ LA PURGA HA TERMINADO 🛡️",
                    "La purga ha llegado a su fin. Las zonas protegidas vuelven a ser seguras.",
                    3066993, // Verde #2ECC71
                    "Estadísticas de la Purga",
                    "👤 **Finalizado por:** " + finalizador + "\n☠️ **Bajas totales cobradas:** " + muertesEnPurga
            );
        }
    }

    public void registrarMuerte(Player victima, Player asesino) {
        muertesEnPurga++;
        String arma = "manos desnudas";
        if (asesino.getInventory().getItemInMainHand() != null && asesino.getInventory().getItemInMainHand().getType() != null) {
            String tipo = asesino.getInventory().getItemInMainHand().getType().name();
            if (!tipo.equalsIgnoreCase("AIR")) {
                arma = tipo.replace("_", " ").toLowerCase();
            }
        }

        // Mensaje global de muerte
        String msg = getConfig().getString("mensajes.muerte-broadcast", "&4[Purga] &c%victima% &7fue asesinado por &c%asesino% &7usando &e%arma%&7.")
                .replace("%victima%", victima.getName())
                .replace("%asesino%", asesino.getName())
                .replace("%arma%", arma);
        Bukkit.broadcastMessage(colorize(msg));

        // Discord Webhook de muerte
        discordWebhook.enviarEmbed(
                "⚔️ ¡SE HA COBRADO UNA VIDA EN LA PURGA!",
                "**" + asesino.getName() + "** ha ejecutado a **" + victima.getName() + "**.",
                10038562, // Rojo oscuro #992D22
                "Detalles del Enfrentamiento",
                "🗡️ **Arma utilizada:** " + arma + "\n🌍 **Mundo:** " + victima.getWorld().getName() + "\n📍 **Coordenadas:** X: " + victima.getLocation().getBlockX() + " Y: " + victima.getLocation().getBlockY() + " Z: " + victima.getLocation().getBlockZ()
        );
    }

    private void reproducirSonido(String configPath) {
        String soundName = getConfig().getString(configPath);
        if (soundName == null || soundName.trim().isEmpty()) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            PurgaMobManager.playSoundSafely(p.getWorld(), p.getLocation(), 1.0f, 1.0f, soundName);
        }
    }

    public void enviarBroadcastConfig(String configPath) {
        List<String> lineas = getConfig().getStringList(configPath);
        for (String linea : lineas) {
            Bukkit.broadcastMessage(colorize(linea));
        }
    }

    public String colorize(String str) {
        return ChatColor.translateAlternateColorCodes('&', str);
    }

    public boolean isPurgaActiva() { return purgaActiva; }
    public int getMuertesEnPurga() { return muertesEnPurga; }
    public BossBar getBossBar() { return bossBar; }
    public long getTiempoRestanteSegundos() { return tiempoRestanteSegundos; }
    public PurgaMobManager getMobManager() { return mobManager; }
}
