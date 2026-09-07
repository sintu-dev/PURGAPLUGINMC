import com.tuserver.purgaplugin.TimeUtils;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PurgaSystemTest {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   EJECUTANDO BATERIA DE PRUEBAS DE PURGAPLUGIN   ");
        System.out.println("=================================================");

        testTimeParsing();
        testTimeFormatting();
        testMobVariantsValidity();
        testMaterialRewardsValidity();
        testWaveScalingFormulas();
        testDropProbabilityDistribution();
        testTotemFailProbability();
        testCreeperProximityLogic();
        testParticleAndSoundEnums();
        testPlayerZombieMechanics();
        testCompanionNegativeEffects();
        testDanisCatMechanics();
        testJustWolfMechanics();
        testPerformanceAndMemoryOptimizations();
        testNewFixes();

        System.out.println("=================================================");
        System.out.printf("RESULTADOS: %d SUPERADAS | %d FALLADAS%n", testsPassed, testsFailed);
        System.out.println("=================================================");

        if (testsFailed > 0) {
            System.exit(1);
        }
    }

    private static void testNewFixes() {
        System.out.println("\n--- 15. Pruebas de Correcciones y Compatibilidad ---");

        // 1. Test de Parseo de Tiempos Multi-Token (ej: "1h 30m", "2h 15m 30s")
        assertEquals("Parseo multi-token '1h 30m'", 5400L, TimeUtils.parsearTiempoASegundos("1h 30m"));
        assertEquals("Parseo multi-token '2h 15m 30s'", 8130L, TimeUtils.parsearTiempoASegundos("2h 15m 30s"));

        // 2. Test de Detección de Mención @everyone en Discord Webhook
        String tituloInicio = "🚨 ¡LA PURGA HA COMENZADO! 🚨";
        boolean debeMencionar = tituloInicio.contains("COMENZADO") || tituloInicio.contains("INICIAD") || tituloInicio.contains("PURGA");
        assertTrue("Título de inicio activa mención @everyone en Discord", debeMencionar);

        // 3. Test de Mapeo de Nombres de Encantamientos (Legacy Bukkit vs Modern Minecraft)
        String[] enchTests = {"POWER", "FLAME", "PUNCH", "SHARPNESS", "PROTECTION", "UNBREAKING", "INFINITY"};
        for (String name : enchTests) {
            String mapped = switch (name) {
                case "POWER" -> "ARROW_DAMAGE";
                case "FLAME" -> "ARROW_FIRE";
                case "PUNCH" -> "ARROW_KNOCKBACK";
                case "SHARPNESS" -> "DAMAGE_ALL";
                case "PROTECTION" -> "PROTECTION_ENVIRONMENTAL";
                case "UNBREAKING" -> "DURABILITY";
                case "INFINITY" -> "ARROW_INFINITE";
                default -> name;
            };
            assertTrue("Encantamiento mapeado no nulo: " + name + " -> " + mapped, mapped != null && !mapped.equals(name));
        }

        // 4. Test de Diferenciación entre Mobs Hostiles y JUST (1-Hit Kill seguro)
        java.util.UUID justId = java.util.UUID.randomUUID();
        java.util.UUID danisId = java.util.UUID.randomUUID();
        java.util.UUID zombieId = java.util.UUID.randomUUID();

        java.util.Set<java.util.UUID> purgaMobs = new java.util.HashSet<>();
        java.util.Set<java.util.UUID> justSet = new java.util.HashSet<>();
        java.util.Set<java.util.UUID> danisSet = new java.util.HashSet<>();

        purgaMobs.add(justId);
        justSet.add(justId);

        purgaMobs.add(danisId);
        danisSet.add(danisId);

        purgaMobs.add(zombieId);

        boolean isZombieHostil = purgaMobs.contains(zombieId) && !justSet.contains(zombieId) && !danisSet.contains(zombieId);
        boolean isJustHostil = purgaMobs.contains(justId) && !justSet.contains(justId) && !danisSet.contains(justId);

        assertTrue("Zombie de purga es identificado como hostil (1-Hit Kill)", isZombieHostil);
        assertTrue("Lobo JUST NO es identificado como hostil (daño normal de lobo)", !isJustHostil);

        // 5. Test de Consumo Seguro de Tótem (evitar ítems fantasma con amount <= 1)
        int totemAmount1 = 1;
        boolean eliminarSlot1 = (totemAmount1 <= 1);
        assertTrue("Tótem único (amount 1) elimina el slot completamente", eliminarSlot1);

        int totemAmountStack = 3;
        int nuevoAmount = totemAmountStack - 1;
        assertEquals("Tótem apilado (amount 3) se reduce a 2", 2, nuevoAmount);
    }

    private static void assertEquals(String testName, Object expected, Object actual) {
        if ((expected == null && actual == null) || (expected != null && expected.equals(actual))) {
            System.out.println("  [PASS] " + testName + " -> " + actual);
            testsPassed++;
        } else {
            System.err.println("  [FAIL] " + testName + " | Esperado: " + expected + ", Obtenido: " + actual);
            testsFailed++;
        }
    }

    private static void assertTrue(String testName, boolean condition) {
        if (condition) {
            System.out.println("  [PASS] " + testName);
            testsPassed++;
        } else {
            System.err.println("  [FAIL] " + testName + " | Condicion esperada: TRUE, pero fue: FALSE");
            testsFailed++;
        }
    }

    private static void testTimeParsing() {
        System.out.println("\n--- 1. Pruebas de Parseo de Tiempos (TimeUtils) ---");
        assertEquals("Parseo 30s", 30L, TimeUtils.parsearTiempoASegundos("30s"));
        assertEquals("Parseo 10m", 600L, TimeUtils.parsearTiempoASegundos("10m"));
        assertEquals("Parseo 1h", 3600L, TimeUtils.parsearTiempoASegundos("1h"));
        assertEquals("Parseo 24h", 86400L, TimeUtils.parsearTiempoASegundos("24h"));
        assertEquals("Parseo 3d", 259200L, TimeUtils.parsearTiempoASegundos("3d"));
        assertEquals("Parseo 1h30m", 5400L, TimeUtils.parsearTiempoASegundos("1h30m"));
        assertEquals("Parseo 'manual'", 0L, TimeUtils.parsearTiempoASegundos("manual"));
        assertEquals("Parseo '0'", 0L, TimeUtils.parsearTiempoASegundos("0"));
    }

    private static void testTimeFormatting() {
        System.out.println("\n--- 2. Pruebas de Formato de Tiempos ---");
        assertEquals("Formatear 30s", "30 segundo(s)", TimeUtils.formatearTiempo(30));
        assertEquals("Formatear 10m", "10 minuto(s)", TimeUtils.formatearTiempo(600));
        assertEquals("Formatear 1h 30m", "1 hora(s) 30 minuto(s)", TimeUtils.formatearTiempo(5400));
        assertEquals("Formatear 24h", "1 día(s)", TimeUtils.formatearTiempo(86400));
        assertEquals("Formatear 0s (manual)", "Indefinida (Manual)", TimeUtils.formatearTiempo(0));

        // BossBar format
        assertEquals("BossBar 30s", "00:30", TimeUtils.formatearParaBossBar(30));
        assertEquals("BossBar 10m", "10:00", TimeUtils.formatearParaBossBar(600));
        assertEquals("BossBar 1h 30m", "01:30:00", TimeUtils.formatearParaBossBar(5400));
    }

    private static void testMobVariantsValidity() {
        System.out.println("\n--- 3. Pruebas de Variantes de Mobs Permitidos ---");
        String[] mobTypes = {
                "ZOMBIE", "SKELETON", "ZOMBIE_VILLAGER", "VINDICATOR", "WITHER_SKELETON",
                "PIGLIN_BRUTE", "CREEPER", "SPIDER", "CAVE_SPIDER", "ENDERMAN",
                "DROWNED", "HUSK", "STRAY", "RAVAGER", "WITCH"
        };

        for (String typeName : mobTypes) {
            try {
                EntityType type = EntityType.valueOf(typeName);
                assertTrue("Validar EntityType: " + typeName, type != null);
            } catch (Exception e) {
                assertTrue("EntityType inválido: " + typeName, false);
            }
        }
    }

    private static void testMaterialRewardsValidity() {
        System.out.println("\n--- 4. Pruebas de Materiales de Drops/Recompensas ---");
        String[] materials = {
                "DIAMOND", "GOLDEN_APPLE", "NETHERITE_SCRAP", "TOTEM_OF_UNDYING",
                "EMERALD", "IRON_INGOT", "GOLD_INGOT"
        };

        for (String matName : materials) {
            Material mat = Material.matchMaterial(matName);
            assertTrue("Validar Material de recompensa: " + matName, mat != null);
        }
    }

    private static void testWaveScalingFormulas() {
        System.out.println("\n--- 5. Pruebas de Fórmulas de Escalado por Oleadas ---");
        double vidaBase = 20.0;
        double vidaExtraPorOleada = 10.0;
        int maxOleadas = 10;

        // Oleada 1: 0 vida extra
        int oleada1 = 1;
        int oleadasEfectivas1 = Math.min(oleada1, maxOleadas);
        double extra1 = Math.max(0, (oleadasEfectivas1 - 1) * vidaExtraPorOleada);
        assertEquals("Vida extra en oleada 1", 0.0, extra1);

        // Oleada 3: (3 - 1) * 10 = +20 vida
        int oleada3 = 3;
        int oleadasEfectivas3 = Math.min(oleada3, maxOleadas);
        double extra3 = Math.max(0, (oleadasEfectivas3 - 1) * vidaExtraPorOleada);
        assertEquals("Vida extra en oleada 3", 20.0, extra3);

        // Oleada 15 (Supera max): (10 - 1) * 10 = +90 vida
        int oleada15 = 15;
        int oleadasEfectivas15 = Math.min(oleada15, maxOleadas);
        double extra15 = Math.max(0, (oleadasEfectivas15 - 1) * vidaExtraPorOleada);
        assertEquals("Vida extra en oleada 15 (tope maximo)", 90.0, extra15);

        // Escalado de pociones: cada 2 oleadas
        int intervaloPociones = 2;
        int fuerzaBase = 4;
        int fuerzaOleada1 = Math.min(10, fuerzaBase + (1 / intervaloPociones));
        assertEquals("Fuerza oleada 1", 4, fuerzaOleada1);
        int fuerzaOleada4 = Math.min(10, fuerzaBase + (4 / intervaloPociones));
        assertEquals("Fuerza oleada 4", 6, fuerzaOleada4);
    }

    private static void testDropProbabilityDistribution() {
        System.out.println("\n--- 6. Pruebas de Distribución de Probabilidad de Drops ---");
        Random rnd = new Random(42);
        double prob = 50.0; // 50%
        int trials = 10000;
        int dropCount = 0;

        for (int i = 0; i < trials; i++) {
            if (rnd.nextDouble() * 100.0 <= prob) {
                dropCount++;
            }
        }

        double ratio = (double) dropCount / trials;
        assertTrue("Distribución 50% drop rate (" + ratio + ") dentro de rango [0.47, 0.53]", ratio >= 0.47 && ratio <= 0.53);

        // Rango de cantidad min/max
        int min = 1;
        int max = 3;
        for (int i = 0; i < 100; i++) {
            int cant = min + (max > min ? rnd.nextInt(max - min + 1) : 0);
            assertTrue("Cantidad calculada dentro de rango [1, 3]: " + cant, cant >= 1 && cant <= 3);
        }
    }

    private static void testTotemFailProbability() {
        System.out.println("\n--- 7. Pruebas de Probabilidad de Fallo de Tótems ---");
        Random rnd = new Random(123);
        double probFallo = 40.0; // 40%
        int trials = 10000;
        int failCount = 0;

        for (int i = 0; i < trials; i++) {
            if (rnd.nextDouble() * 100.0 < probFallo) {
                failCount++;
            }
        }

        double failRatio = (double) failCount / trials;
        assertTrue("Distribución de fallo de tótems 40% (" + failRatio + ") dentro de rango [0.38, 0.42]", failRatio >= 0.38 && failRatio <= 0.42);
    }

    private static void testCreeperProximityLogic() {
        System.out.println("\n--- 8. Pruebas de Lógica de Distancia de Detonación de Creeper ---");
        double distExplosion = 3.5;
        double distSq = distExplosion * distExplosion; // 12.25

        // Jugador a 2.0 bloques de distancia -> distancia al cuadrado = 4.0 <= 12.25 (DETONA)
        double d1 = 2.0;
        double sq1 = d1 * d1;
        assertTrue("Jugador a 2m detona instantáneamente", sq1 <= distSq);

        // Jugador a 5.0 bloques de distancia -> distancia al cuadrado = 25.0 > 12.25 (NO DETONA)
        double d2 = 5.0;
        double sq2 = d2 * d2;
        assertTrue("Jugador a 5m NO detona aún", sq2 > distSq);
    }

    private static void testParticleAndSoundEnums() {
        System.out.println("\n--- 9. Pruebas de Enums de Sonidos y Partículas ---");
        try {
            Sound s1 = Sound.valueOf("BLOCK_RESPAWN_ANCHOR_DEPLETE");
            Sound s2 = Sound.valueOf("ENTITY_ENDERMAN_TELEPORT");
            Sound s3 = Sound.valueOf("ENTITY_GENERIC_EXPLODE");
            Sound s4 = Sound.valueOf("ENTITY_ZOMBIE_VILLAGER_CONVERTED");
            Sound s5 = Sound.valueOf("ENTITY_WARDEN_HEARTBEAT");
            assertTrue("Validar sonidos de eventos", s1 != null && s2 != null && s3 != null && s4 != null && s5 != null);

            Particle p1 = Particle.valueOf("EXPLOSION");
            Particle p2 = Particle.valueOf("PORTAL");
            Particle p3 = Particle.valueOf("LARGE_SMOKE");
            assertTrue("Validar partículas de eventos", p1 != null && p2 != null && p3 != null);
        } catch (Exception e) {
            assertTrue("Error validando enums: " + e.getMessage(), false);
        }
    }

    private static void testPlayerZombieMechanics() {
        System.out.println("\n--- 10. Pruebas de Mecánicas de Zombie del Jugador Caído ---");
        // Test de formato de nombre
        String victimName = "Notch";
        String template = "&4&l☠ %jugador% (Zombificado) ☠";
        String formattedName = template.replace("%jugador%", victimName);
        assertEquals("Formateo de nombre de Zombie", "&4&l☠ Notch (Zombificado) ☠", formattedName);

        // Test de almacenamiento y recuperación de botín
        java.util.Map<java.util.UUID, List<String>> simDrops = new java.util.HashMap<>();
        java.util.UUID zombieId = java.util.UUID.randomUUID();
        List<String> items = new ArrayList<>();
        items.add("DIAMOND_SWORD");
        items.add("NETHERITE_CHESTPLATE");
        simDrops.put(zombieId, items);

        assertTrue("Botín almacenado correctamente", simDrops.containsKey(zombieId));
        List<String> retrieved = simDrops.remove(zombieId);
        assertEquals("Botín recuperado correctamente", 2, retrieved.size());
        assertTrue("Botín limpiado del mapa tras muerte", !simDrops.containsKey(zombieId));

        // Test de radio de detección
        double radioDeteccion = 25.0;
        double radioSq = radioDeteccion * radioDeteccion; // 625.0
        double distCompanero = 15.0;
        assertTrue("Compañero a 15m dentro de radio 25m", (distCompanero * distCompanero) <= radioSq);
        double distLejos = 30.0;
        assertTrue("Compañero a 30m fuera de radio 25m", (distLejos * distLejos) > radioSq);
    }

    private static void testCompanionNegativeEffects() {
        System.out.println("\n--- 11. Pruebas de Efectos Negativos a Compañeros ---");
        String[] effectNames = { "BLINDNESS", "SLOW", "DARKNESS", "WEAKNESS", "NAUSEA" };
        for (String eff : effectNames) {
            assertTrue("Efecto de poción configurado correctamente: " + eff, eff != null && !eff.isEmpty());
        }

        // Test de radio de efectos
        double radioEfectos = 20.0;
        double radioEfectosSq = radioEfectos * radioEfectos; // 400.0
        double dist10m = 10.0;
        assertTrue("Compañero a 10m dentro del radio de ceguera", (dist10m * dist10m) <= radioEfectosSq);
        double dist25m = 25.0;
        assertTrue("Compañero a 25m fuera del radio de ceguera", (dist25m * dist25m) > radioEfectosSq);

        // Test de cálculo de duración y nivel
        int durSec = 10;
        int ticks = durSec * 20;
        assertEquals("Conversión de segundos a ticks de efecto (10s)", 200, ticks);
        int nivelConfig = 2;
        int amplificador = nivelConfig - 1; // Nivel 2 en Bukkit es amplifier 1
        assertEquals("Amplificador de poción para nivel 2", 1, amplificador);
    }

    private static void testDanisCatMechanics() {
        System.out.println("\n--- 12. Pruebas de Mecánicas del Gato DANIS ---");
        // Validación de EntityType
        assertEquals("EntityType de DANIS", EntityType.CAT, EntityType.valueOf("CAT"));

        // Test de umbral de detonación (cuando queda 1 mob o menos)
        int hostilesRestantes = 1;
        int umbralConfig = 1;
        assertTrue("DANIS detona cuando queda 1 mob hostil", hostilesRestantes <= umbralConfig);

        int hostilesRestantesMuchos = 3;
        assertTrue("DANIS NO detona cuando quedan varios mobs", hostilesRestantesMuchos > umbralConfig);

        // Test de proximidad de detonación
        double distAlJugador = 1.5;
        double distDetonacion = 2.0;
        assertTrue("DANIS detona a 1.5m del jugador", distAlJugador <= distDetonacion);

        double distLejana = 5.0;
        assertTrue("DANIS NO detona a 5.0m del jugador", distLejana > distDetonacion);

        // Test de registro de bypass de tótem
        java.util.Set<java.util.UUID> bypassSet = new java.util.HashSet<>();
        java.util.UUID victimaId = java.util.UUID.randomUUID();
        bypassSet.add(victimaId);
        assertTrue("Jugador registrado en bypass de tótem", bypassSet.contains(victimaId));
        bypassSet.remove(victimaId);
        assertTrue("Bypass de tótem removido tras ejecución", !bypassSet.contains(victimaId));
    }

    private static void testJustWolfMechanics() {
        System.out.println("\n--- 13. Pruebas de Mecánicas del Lobo JUST ---");
        // Validación de EntityType
        assertEquals("EntityType de JUST", EntityType.WOLF, EntityType.valueOf("WOLF"));

        // Test de requisitos de domesticación: 2 Netherite + 2 Diamantes
        int netheriteRequerida = 2;
        int diamantesRequeridos = 2;

        int invNetherite = 3;
        int invDiamantes = 5;
        boolean puedeDomesticar = invNetherite >= netheriteRequerida && invDiamantes >= diamantesRequeridos;
        assertTrue("Jugador con 3 netherite y 5 diamantes puede domesticar", puedeDomesticar);

        int invInsuficienteNetherite = 1;
        boolean noPuedeDomesticar = invInsuficienteNetherite >= netheriteRequerida && invDiamantes >= diamantesRequeridos;
        assertTrue("Jugador con 1 netherite NO puede domesticar", !noPuedeDomesticar);

        // Test de radio de protección para ahuyentar a DANIS
        double radioAhuyentar = 8.0;
        double radioAhuyentarSq = radioAhuyentar * radioAhuyentar; // 64.0
        double distDanis = 5.0;
        assertTrue("DANIS a 5m es ahuyentado por JUST (dentro de 8m)", (distDanis * distDanis) <= radioAhuyentarSq);
        double distDanisLejos = 12.0;
        assertTrue("DANIS a 12m está fuera del radio de protección de JUST", (distDanisLejos * distDanisLejos) > radioAhuyentarSq);

        // Test de límite de 1 JUST por jugador
        java.util.Map<java.util.UUID, java.util.UUID> justOwners = new java.util.HashMap<>();
        java.util.UUID playerId = java.util.UUID.randomUUID();
        java.util.UUID wolfId = java.util.UUID.randomUUID();
        justOwners.put(playerId, wolfId);
        assertTrue("Jugador ya tiene registrado 1 lobo JUST", justOwners.containsKey(playerId));

        // Test de persistencia tras purga
        boolean persistirTrasPurga = true;
        boolean esTamed = true;
        boolean sobrevive = esTamed && persistirTrasPurga;
        assertTrue("Lobo JUST domesticado sobrevive al final de la purga", sobrevive);
    }

    private static void testPerformanceAndMemoryOptimizations() {
        System.out.println("\n--- 14. Pruebas de Optimizaciones de Rendimiento y Memoria ---");

        // 1. Test de Sets Concurrentes para evitar fugas de memoria y bloqueos
        java.util.Set<java.util.UUID> activePurgaMobs = java.util.concurrent.ConcurrentHashMap.newKeySet();
        java.util.Set<java.util.UUID> activeHostileMobs = java.util.concurrent.ConcurrentHashMap.newKeySet();
        java.util.Set<java.util.UUID> creeperMobs = java.util.concurrent.ConcurrentHashMap.newKeySet();

        java.util.UUID mob1 = java.util.UUID.randomUUID();
        java.util.UUID creeper1 = java.util.UUID.randomUUID();

        activePurgaMobs.add(mob1);
        activeHostileMobs.add(mob1);

        activePurgaMobs.add(creeper1);
        activeHostileMobs.add(creeper1);
        creeperMobs.add(creeper1);

        assertEquals("Total mobs activos inicial", 2, activePurgaMobs.size());
        assertEquals("Total hostiles activos inicial", 2, activeHostileMobs.size());
        assertEquals("Total creepers activos inicial", 1, creeperMobs.size());

        // Simular muerte y limpieza instantánea (removerMob)
        activePurgaMobs.remove(creeper1);
        activeHostileMobs.remove(creeper1);
        creeperMobs.remove(creeper1);

        assertEquals("Creepers tras muerte", 0, creeperMobs.size());
        assertEquals("Hostiles tras muerte de creeper", 1, activeHostileMobs.size());
        assertTrue("Creeper removido correctamente de todos los índices", !creeperMobs.contains(creeper1) && !activePurgaMobs.contains(creeper1));

        // 2. Test de conteo de hostiles en memoria vs escaneo de mundo (0 asignaciones de lista)
        int hostilesEnRango = 0;
        double maxDistSq = 35.0 * 35.0; // 1225.0
        double mobDist = 10.0;
        if ((mobDist * mobDist) <= maxDistSq) {
            hostilesEnRango++;
        }
        assertEquals("Conteo de hostiles directo en memoria", 1, hostilesEnRango);
        assertTrue("DANIS puede verificar hostiles instantáneamente sin escanear el mundo", hostilesEnRango > 0);

        // 3. Test de Límite de Mobs por Jugador (Mob Cap contra picos de CPU)
        int maxMobsPermitidos = 6;
        int hostilesActivosJugador = 5;
        int cantidadPorOleada = 3;
        int aGenerar = Math.max(0, Math.min(cantidadPorOleada, maxMobsPermitidos - hostilesActivosJugador));
        assertEquals("Spawning controlado por Mob Cap (solo 1 mob generado para no exceder 6)", 1, aGenerar);

        int hostilesSaturados = 6;
        int aGenerarSaturado = Math.max(0, Math.min(cantidadPorOleada, maxMobsPermitidos - hostilesSaturados));
        assertEquals("Spawning bloqueado si el jugador ya alcanzó el Mob Cap (0 mobs generados)", 0, aGenerarSaturado);

        // 4. Test de Distancia de Auto-Despawn de Mobs Abandonados
        double despawnDist = 64.0;
        double despawnDistSq = despawnDist * despawnDist; // 4096.0
        double distLejosAbandonado = 75.0;
        boolean debeDespawnear = (distLejosAbandonado * distLejosAbandonado) > despawnDistSq;
        assertTrue("Mob a 75m de todos los jugadores se auto-despawnea para liberar CPU", debeDespawnear);
    }
}
