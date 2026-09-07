package com.tuserver.purgaplugin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtils {

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)\\s*([a-zA-Z]+)?");

    /**
     * Parsea strings de tiempo como: "30s", "10m", "2h", "1h30m", "24h", "3d", "7d", "manual", "0"
     * Devuelve la cantidad en segundos. Si es "0" o "manual", devuelve 0.
     * Si no tiene unidad (ej: "30"), se asume minutos.
     */
    public static long parsearTiempoASegundos(String input) throws IllegalArgumentException {
        if (input == null || input.trim().isEmpty()) {
            return 0;
        }

        String limpio = input.trim().toLowerCase();
        if (limpio.equals("manual") || limpio.equals("off") || limpio.equals("0") || limpio.equals("infinito") || limpio.equals("indefinido")) {
            return 0;
        }

        // Si es solo un número sin letras (ej: "30"), se asume minutos por defecto
        if (limpio.matches("^\\d+$")) {
            return Long.parseLong(limpio) * 60;
        }

        long totalSegundos = 0;
        Matcher matcher = TIME_PATTERN.matcher(limpio);
        boolean encontroCoincidencia = false;

        while (matcher.find()) {
            encontroCoincidencia = true;
            long valor = Long.parseLong(matcher.group(1));
            String unidad = matcher.group(2);

            if (unidad == null || unidad.isEmpty() || unidad.startsWith("m")) {
                totalSegundos += valor * 60; // minutos (m, min, mins, minutos)
            } else if (unidad.startsWith("s") && !unidad.startsWith("sem")) {
                totalSegundos += valor; // segundos (s, seg, segs, seconds)
            } else if (unidad.startsWith("h")) {
                totalSegundos += valor * 3600; // horas (h, hr, hrs, horas, hours)
            } else if (unidad.startsWith("d")) {
                totalSegundos += valor * 86400; // días (d, dia, dias, days)
            } else if (unidad.startsWith("w") || unidad.startsWith("sem")) {
                totalSegundos += valor * 604800; // semanas (w, weeks, sem, semanas)
            } else {
                throw new IllegalArgumentException("Unidad de tiempo desconocida: " + unidad);
            }
        }

        if (!encontroCoincidencia) {
            throw new IllegalArgumentException("Formato de tiempo inválido: " + input);
        }

        return totalSegundos;
    }

    /**
     * Formatea segundos a texto legible para Discord o mensajes en chat (ej: "24h 30m", "10m 00s", "3d 12h").
     */
    public static String formatearTiempo(long segundosTotales) {
        if (segundosTotales <= 0) {
            return "Indefinida (Manual)";
        }

        long dias = segundosTotales / 86400;
        long horas = (segundosTotales % 86400) / 3600;
        long minutos = (segundosTotales % 3600) / 60;
        long segundos = segundosTotales % 60;

        StringBuilder sb = new StringBuilder();
        if (dias > 0) sb.append(dias).append(" día(s) ");
        if (horas > 0) sb.append(horas).append(" hora(s) ");
        if (minutos > 0) sb.append(minutos).append(" minuto(s) ");
        if (dias == 0 && horas == 0 && segundos > 0) sb.append(segundos).append(" segundo(s)");

        return sb.toString().trim();
    }

    /**
     * Formato conciso para BossBar (ej: "23:59:12" o "14:32" o "3d 12h 30m").
     */
    public static String formatearParaBossBar(long segundosTotales) {
        if (segundosTotales <= 0) {
            return "Manual";
        }

        long dias = segundosTotales / 86400;
        long horas = (segundosTotales % 86400) / 3600;
        long minutos = (segundosTotales % 3600) / 60;
        long segundos = segundosTotales % 60;

        if (dias > 0) {
            return String.format("%dd %02dh %02dm", dias, horas, minutos);
        } else if (horas > 0) {
            return String.format("%02d:%02d:%02d", horas, minutos, segundos);
        } else {
            return String.format("%02d:%02d", minutos, segundos);
        }
    }
}
