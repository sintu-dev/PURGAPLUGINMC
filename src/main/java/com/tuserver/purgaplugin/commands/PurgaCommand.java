package com.tuserver.purgaplugin.commands;

import com.tuserver.purgaplugin.PurgaPlugin;
import com.tuserver.purgaplugin.TimeUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PurgaCommand implements CommandExecutor, TabCompleter {

    private final PurgaPlugin plugin;

    public PurgaCommand(PurgaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefijo = plugin.colorize(plugin.getConfig().getString("mensajes.prefijo", "&8[&4&lPURGA&8] "));

        if (!sender.hasPermission("purga.admin")) {
            sender.sendMessage(plugin.colorize(plugin.getConfig().getString("mensajes.sin-permiso", "&cNo tienes permisos.")));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(plugin.colorize("&8&m━━━━━━━━━━&r &4&lCOMANDOS DE PURGA &8&m━━━━━━━━━━"));
            sender.sendMessage(plugin.colorize("&c/purga start [tiempo] &7- Inicia la purga (ej: &e30s&7, &e10m&7, &e2h&7, &e24h&7, &e3d&7, &emanual&7)."));
            sender.sendMessage(plugin.colorize("&c/purga stop &7- Detiene la purga inmediatamente."));
            sender.sendMessage(plugin.colorize("&c/purga status &7- Ver estado actual y estadísticas."));
            sender.sendMessage(plugin.colorize("&c/purga reload &7- Recarga la configuración del plugin."));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start":
            case "on":
                if (plugin.isPurgaActiva()) {
                    sender.sendMessage(plugin.colorize(plugin.getConfig().getString("mensajes.purga-ya-activa", "&c¡La purga ya está activa!")));
                    return true;
                }

                String tiempoArg = plugin.getConfig().getString("duracion-por-defecto", "30m");
                if (args.length >= 2) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 1; i < args.length; i++) {
                        sb.append(args[i]).append(" ");
                    }
                    tiempoArg = sb.toString().trim();
                }

                long segundos;
                try {
                    segundos = TimeUtils.parsearTiempoASegundos(tiempoArg);
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(prefijo + plugin.colorize("&cFormato de tiempo inválido. Ejemplos válidos: &e30s&c, &e15m&c, &e2h&c, &e24h&c, &e3d&c, &emanual&c."));
                    return true;
                }

                plugin.iniciarPurga(sender.getName(), segundos);
                String durMsg = (segundos > 0) ? "&a (" + TimeUtils.formatearTiempo(segundos) + ")" : "&e (modo manual)";
                sender.sendMessage(prefijo + plugin.colorize("&aHas iniciado el evento de purga" + durMsg + "."));
                break;

            case "stop":
            case "off":
                if (!plugin.isPurgaActiva()) {
                    sender.sendMessage(plugin.colorize(plugin.getConfig().getString("mensajes.purga-no-activa", "&cLa purga no está activa.")));
                    return true;
                }
                plugin.detenerPurga(sender.getName(), true);
                sender.sendMessage(prefijo + plugin.colorize("&cHas detenido el evento de purga."));
                break;

            case "status":
                sender.sendMessage(plugin.colorize("&8&m━━━━━━━━━━&r &4&lESTADO DE LA PURGA &8&m━━━━━━━━━━"));
                sender.sendMessage(plugin.colorize("&7Estado: " + (plugin.isPurgaActiva() ? "&a&lACTIVA" : "&c&lINACTIVA")));
                if (plugin.isPurgaActiva()) {
                    long rest = plugin.getTiempoRestanteSegundos();
                    String tiempoStr = TimeUtils.formatearTiempo(rest);
                    sender.sendMessage(plugin.colorize("&7Tiempo restante: &e" + tiempoStr));
                    sender.sendMessage(plugin.colorize("&7Oleada actual: &e" + plugin.getMobManager().getNumeroOleada()));
                    sender.sendMessage(plugin.colorize("&7Bajas cobradas: &c" + plugin.getMuertesEnPurga()));
                }
                break;

            case "reload":
                plugin.reloadConfig();
                sender.sendMessage(prefijo + plugin.colorize(plugin.getConfig().getString("mensajes.config-recargada", "&aConfiguración recargada.")));
                break;

            default:
                sender.sendMessage(prefijo + plugin.colorize("&cSubcomando desconocido. Usa &e/purga&c."));
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("purga.admin")) return new ArrayList<>();

        if (args.length == 1) {
            return Arrays.asList("start", "stop", "status", "reload");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            return Arrays.asList("30s", "5m", "10m", "30m", "1h", "2h", "6h", "12h", "24h", "3d", "7d", "manual");
        }
        return new ArrayList<>();
    }
}
