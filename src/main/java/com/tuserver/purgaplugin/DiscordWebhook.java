package com.tuserver.purgaplugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DiscordWebhook {

    private final PurgaPlugin plugin;

    public DiscordWebhook(PurgaPlugin plugin) {
        this.plugin = plugin;
    }

    public void enviarEmbed(String titulo, String descripcion, int colorHex, String campoNombre, String campoValor) {
        if (!plugin.getConfig().getBoolean("discord.activado", false)) {
            return;
        }

        String webhookUrl = plugin.getConfig().getString("discord.webhook-url", "");
        if (webhookUrl.isEmpty() || webhookUrl.contains("TU_WEBHOOK_AQUI")) {
            plugin.getLogger().warning("Discord Webhook URL no configurada en config.yml");
            return;
        }

        // Ejecutar de forma asíncrona para no congelar el servidor
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("username", plugin.getConfig().getString("discord.nombre-bot", "Sistema de Purga"));
                json.addProperty("avatar_url", plugin.getConfig().getString("discord.avatar-url", ""));

                if (plugin.getConfig().getBoolean("discord.mencionar-everyone", false) && (titulo.contains("COMENZADO") || titulo.contains("INICIAD") || titulo.contains("PURGA"))) {
                    json.addProperty("content", "@everyone");
                }

                JsonArray embeds = new JsonArray();
                JsonObject embed = new JsonObject();
                embed.addProperty("title", titulo);
                embed.addProperty("description", descripcion);
                embed.addProperty("color", colorHex);

                if (campoNombre != null && campoValor != null) {
                    JsonArray fields = new JsonArray();
                    JsonObject field = new JsonObject();
                    field.addProperty("name", campoNombre);
                    field.addProperty("value", campoValor);
                    field.addProperty("inline", false);
                    fields.add(field);
                    embed.add("fields", fields);
                }

                JsonObject footer = new JsonObject();
                footer.addProperty("text", "Purga System • Servidor de Minecraft");
                embed.add("footer", footer);

                embeds.add(embed);
                json.add("embeds", embeds);

                URL url = java.net.URI.create(webhookUrl).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("User-Agent", "Minecraft-PurgaPlugin");
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = json.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode < 200 || responseCode >= 300) {
                    plugin.getLogger().warning("Error enviando Webhook a Discord. Código HTTP: " + responseCode);
                }
                conn.disconnect();
            } catch (Exception e) {
                plugin.getLogger().warning("No se pudo enviar el Webhook a Discord: " + e.getMessage());
            }
        });
    }
}
