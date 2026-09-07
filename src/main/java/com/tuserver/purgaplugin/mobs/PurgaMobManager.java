package com.tuserver.purgaplugin.mobs;

import com.tuserver.purgaplugin.PurgaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import org.bukkit.entity.Cat;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.entity.Zombie;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PurgaMobManager {

    private final PurgaPlugin plugin;
    private final NamespacedKey mobKey;
    private final NamespacedKey creeperKey;
    private final NamespacedKey playerZombieKey;
    private final NamespacedKey playerVictimNameKey;
    private final NamespacedKey danisKey;
    private final NamespacedKey justKey;

    private final Set<UUID> activePurgaMobs = ConcurrentHashMap.newKeySet();
    private final Set<UUID> activeHostileMobs = ConcurrentHashMap.newKeySet();
    private final Set<UUID> creeperMobs = ConcurrentHashMap.newKeySet();
    private final Map<UUID, LivingEntity> activeEntitiesCache = new ConcurrentHashMap<>();
    private final Map<UUID, List<ItemStack>> playerZombieDrops = new ConcurrentHashMap<>();
    private final Set<UUID> danisCats = ConcurrentHashMap.newKeySet();
    private final Set<UUID> justWolves = ConcurrentHashMap.newKeySet();
    private final Map<UUID, UUID> justOwners = new ConcurrentHashMap<>();
    private final Set<UUID> bypassTotemVictims = ConcurrentHashMap.newKeySet();
    private final Random random = new Random();

    private int numeroOleada = 0;
    private long tickCounter = 0;
    private BukkitTask mainProximityTask;

    public PurgaMobManager(PurgaPlugin plugin) {
        this.plugin = plugin;
        this.mobKey = new NamespacedKey(plugin, "purga_op_mob");
        this.creeperKey = new NamespacedKey(plugin, "purga_creeper");
        this.playerZombieKey = new NamespacedKey(plugin, "purga_player_zombie");
        this.playerVictimNameKey = new NamespacedKey(plugin, "purga_player_victim_name");
        this.danisKey = new NamespacedKey(plugin, "purga_danis_cat");
        this.justKey = new NamespacedKey(plugin, "purga_just_wolf");
    }

    public LivingEntity getLivingEntity(UUID id) {
        if (id == null) return null;
        LivingEntity le = activeEntitiesCache.get(id);
        if (le != null && le.isValid() && !le.isDead()) {
            return le;
        }
        Entity e = Bukkit.getEntity(id);
        if (e instanceof LivingEntity && e.isValid() && !e.isDead()) {
            le = (LivingEntity) e;
            activeEntitiesCache.put(id, le);
            return le;
        }
        activeEntitiesCache.remove(id);
        return null;
    }

    public void iniciarMonitoreoCreepers() {
        detenerMonitoreoCreepers();
        this.mainProximityTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!plugin.isPurgaActiva()) return;
            tickCounter++;

            // 1. LIMPIEZA PERIÓDICA DE MOBS ABANDONADOS (CADA 2 SEGUNDOS PARA PROTEGER LA CPU)
            if (tickCounter % 4 == 0) {
                double despawnDistSq = Math.pow(plugin.getConfig().getDouble("mobs-op.distancia-despawn-mobs", 64.0), 2);
                for (java.util.Iterator<UUID> it = activeHostileMobs.iterator(); it.hasNext();) {
                    UUID hostId = it.next();
                    LivingEntity hostLiving = getLivingEntity(hostId);
                    if (hostLiving == null || !hostLiving.isValid() || hostLiving.isDead()) {
                        it.remove();
                        activePurgaMobs.remove(hostId);
                        creeperMobs.remove(hostId);
                        activeEntitiesCache.remove(hostId);
                        continue;
                    }

                    // Verificar si está lejos de todos los jugadores
                    boolean cercaDeJugador = false;
                    for (Player p : hostLiving.getWorld().getPlayers()) {
                        if (p.isValid() && !p.isDead() && hostLiving.getLocation().distanceSquared(p.getLocation()) <= despawnDistSq) {
                            cercaDeJugador = true;
                            break;
                        }
                    }

                    if (!cercaDeJugador) {
                        hostLiving.remove();
                        it.remove();
                        activePurgaMobs.remove(hostId);
                        creeperMobs.remove(hostId);
                        activeEntitiesCache.remove(hostId);
                    }
                }
            }

            // 2. MONITOREO OPTIMIZADO DE CREEPERS (DETONACIÓN INSTANTÁNEA EN MEMORIA)
            boolean creeperExplosionActiva = plugin.getConfig().getBoolean("mobs-op.habilidades-especiales.creeper.explosion-instantanea-cerca-jugador", true);
            if (creeperExplosionActiva && !creeperMobs.isEmpty()) {
                double distExplosion = plugin.getConfig().getDouble("mobs-op.habilidades-especiales.creeper.distancia-explosion", 3.5);
                double distSq = distExplosion * distExplosion;
                float poder = (float) plugin.getConfig().getDouble("mobs-op.habilidades-especiales.creeper.poder-explosion", 4.0);

                for (java.util.Iterator<UUID> it = creeperMobs.iterator(); it.hasNext();) {
                    UUID mobId = it.next();
                    LivingEntity entity = getLivingEntity(mobId);
                    if (entity instanceof Creeper && entity.isValid() && !entity.isDead()) {
                        Creeper creeper = (Creeper) entity;
                        Location cLoc = creeper.getLocation();
                        World world = cLoc.getWorld();
                        if (world == null) continue;

                        for (Player player : world.getPlayers()) {
                            if (player.isValid() && !player.isDead()) {
                                if (cLoc.distanceSquared(player.getLocation()) <= distSq) {
                                    spawnParticleSafely(world, cLoc, 1, 0, 0, 0, 0, "EXPLOSION", "EXPLOSION_NORMAL", "EXPLOSION_LARGE");
                                    playSoundSafely(world, cLoc, 1.5f, 1.0f, "ENTITY_GENERIC_EXPLODE", "EXPLODE");
                                    world.createExplosion(cLoc, poder, false, false);
                                    creeper.remove();
                                    it.remove();
                                    activePurgaMobs.remove(mobId);
                                    activeHostileMobs.remove(mobId);
                                    activeEntitiesCache.remove(mobId);
                                    break;
                                }
                            }
                        }
                    } else {
                        it.remove();
                        activePurgaMobs.remove(mobId);
                        activeHostileMobs.remove(mobId);
                        activeEntitiesCache.remove(mobId);
                    }
                }
            }

            // 3. MONITOREO OPTIMIZADO DEL LOBO "JUST" (CURACIÓN Y COMBATE)
            if (!justWolves.isEmpty()) {
                boolean curacionActiva = plugin.getConfig().getBoolean("mobs-especiales.just.curacion-al-dueno.activado", true);
                double maxCuracionSq = Math.pow(plugin.getConfig().getDouble("mobs-especiales.just.curacion-al-dueno.radio-maximo", 12.0), 2);
                int nivelCuracion = Math.max(0, plugin.getConfig().getInt("mobs-especiales.just.curacion-al-dueno.nivel-regeneracion", 2) - 1);
                int durSecCuracion = plugin.getConfig().getInt("mobs-especiales.just.curacion-al-dueno.duracion-segundos", 5);

                for (java.util.Iterator<UUID> it = justWolves.iterator(); it.hasNext();) {
                    UUID wolfId = it.next();
                    LivingEntity entity = getLivingEntity(wolfId);
                    if (entity instanceof Wolf && entity.isValid() && !entity.isDead()) {
                        Wolf wolf = (Wolf) entity;
                        if (wolf.isTamed() && wolf.getOwner() instanceof Player) {
                            Player owner = (Player) wolf.getOwner();
                            if (owner.isOnline() && wolf.getWorld().equals(owner.getWorld())) {
                                double dSq = wolf.getLocation().distanceSquared(owner.getLocation());

                                // Curación periódica cada 6 segundos
                                if (tickCounter % 12 == 0 && dSq <= maxCuracionSq && curacionActiva) {
                                    owner.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, durSecCuracion * 20, nivelCuracion, false, true, true));
                                    spawnParticleSafely(wolf.getWorld(), owner.getLocation().add(0, 1.5, 0), 4, 0.3, 0.3, 0.3, 0.05, "HEART");
                                }

                                // Ayudar al dueño atacando mobs hostiles de la purga
                                if (tickCounter % 2 == 0 && (wolf.getTarget() == null || !wolf.getTarget().isValid() || wolf.getTarget().isDead())) {
                                    for (UUID hostId : activeHostileMobs) {
                                        LivingEntity hostLiving = getLivingEntity(hostId);
                                        if (hostLiving != null && hostLiving.isValid() && !hostLiving.isDead()) {
                                            if (hostLiving.getWorld().equals(wolf.getWorld()) && hostLiving.getLocation().distanceSquared(wolf.getLocation()) <= 225.0) {
                                                wolf.setTarget(hostLiving);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        it.remove();
                        activePurgaMobs.remove(wolfId);
                        activeEntitiesCache.remove(wolfId);
                    }
                }
            }

            // 4. MONITOREO DE ALTO RENDIMIENTO DEL GATO "DANIS"
            if (!danisCats.isEmpty()) {
                double radioAhuyentar = plugin.getConfig().getDouble("mobs-especiales.just.ahuyentar-danis.radio-proteccion", 8.0);
                double radioAhuyentarSq = radioAhuyentar * radioAhuyentar;
                int umbralDetonacion = plugin.getConfig().getInt("mobs-especiales.danis.mobs-restantes-para-detonar", 1);
                double distDetonacion = plugin.getConfig().getDouble("mobs-especiales.danis.distancia-detonacion", 2.0);
                boolean bypassTotem = plugin.getConfig().getBoolean("mobs-especiales.danis.bypass-totem", true);
                String msgInsta = plugin.getConfig().getString("mobs-especiales.danis.mensaje-insta-kill",
                        "&4&l☠ ¡DANIS TE HA TRAICIONADO! &cLa explosión fulminante atravesó tu tótem...");

                for (java.util.Iterator<UUID> it = danisCats.iterator(); it.hasNext();) {
                    UUID catId = it.next();
                    LivingEntity entity = getLivingEntity(catId);
                    if (entity instanceof Cat && entity.isValid() && !entity.isDead()) {
                        Cat cat = (Cat) entity;
                        World world = cat.getWorld();

                        Player targetPlayer = null;
                        double closestDistSq = Double.MAX_VALUE;
                        for (Player p : world.getPlayers()) {
                            if (p.isValid() && !p.isDead()) {
                                double dSq = cat.getLocation().distanceSquared(p.getLocation());
                                if (dSq < closestDistSq && dSq <= (40.0 * 40.0)) {
                                    closestDistSq = dSq;
                                    targetPlayer = p;
                                }
                            }
                        }

                        if (targetPlayer != null) {
                            // Verificar si hay un lobo JUST domesticado cerca que ahuyente a DANIS
                            boolean protegidoPorJust = false;
                            for (UUID wolfId : justWolves) {
                                LivingEntity wEntity = getLivingEntity(wolfId);
                                if (wEntity instanceof Wolf && wEntity.isValid() && !wEntity.isDead()) {
                                    Wolf wolf = (Wolf) wEntity;
                                    if (wolf.isTamed() && wolf.getWorld().equals(world)) {
                                        if (wolf.getLocation().distanceSquared(cat.getLocation()) <= radioAhuyentarSq
                                                || wolf.getLocation().distanceSquared(targetPlayer.getLocation()) <= radioAhuyentarSq) {
                                            protegidoPorJust = true;
                                            break;
                                        }
                                    }
                                }
                            }

                            if (protegidoPorJust) {
                                // JUST asusta a DANIS -> Huye y NO puede explotar
                                playSoundSafely(world, cat.getLocation(), 1.0f, 1.2f, "ENTITY_CAT_HISS", "CAT_HISS");
                                spawnParticleSafely(world, cat.getLocation().add(0, 0.5, 0), 5, 0.2, 0.2, 0.2, 0.05, "SMOKE", "SMOKE_NORMAL");
                                Location fleeLoc = calcularUbicacionSegura(cat.getLocation(), 4, 8);
                                if (fleeLoc != null) {
                                    cat.teleport(fleeLoc);
                                }
                            } else {
                                // Contar mobs hostiles de la purga vivos cerca del jugador (O(1) memory lookup)
                                int hostilesRestantes = 0;
                                for (UUID hostId : activeHostileMobs) {
                                    LivingEntity le = getLivingEntity(hostId);
                                    if (le != null && le.isValid() && !le.isDead()) {
                                        if (le.getWorld().equals(world)) {
                                            if (le.getLocation().distanceSquared(targetPlayer.getLocation()) <= (35.0 * 35.0)) {
                                                hostilesRestantes++;
                                            }
                                        }
                                    }
                                }

                                if (hostilesRestantes <= umbralDetonacion) {
                                    // FASE DE DETONACIÓN INSTA-KILL
                                    double distAlJugador = cat.getLocation().distance(targetPlayer.getLocation());

                                    if (distAlJugador <= distDetonacion) {
                                        // DETONACIÓN INSTA-KILL QUE ATRAVIESA TÓTEMS
                                        if (bypassTotem) {
                                            bypassTotemVictims.add(targetPlayer.getUniqueId());
                                        }

                                        spawnParticleSafely(world, cat.getLocation(), 2, 0, 0, 0, 0, "EXPLOSION_EMITTER", "EXPLOSION_HUGE", "EXPLOSION_LARGE");
                                        playSoundSafely(world, cat.getLocation(), 1.5f, 0.8f, "ENTITY_CAT_DEATH", "CAT_DEATH");
                                        playSoundSafely(world, cat.getLocation(), 1.5f, 1.0f, "ENTITY_GENERIC_EXPLODE", "EXPLODE");

                                        targetPlayer.sendMessage(plugin.colorize(msgInsta));
                                        targetPlayer.setHealth(0.0); // Muerte directa e inmediata
                                        cat.remove();
                                        it.remove();
                                        activePurgaMobs.remove(catId);
                                        activeEntitiesCache.remove(catId);
                                    } else {
                                        // Teletransportarse directamente al rango del jugador para detonar
                                        Location moveLoc = targetPlayer.getLocation().add(
                                                (random.nextDouble() - 0.5) * 1.0,
                                                0,
                                                (random.nextDouble() - 0.5) * 1.0
                                        );
                                        cat.teleport(moveLoc);
                                    }
                                } else {
                                    // MODO TRANQUILO: Solo teletransportar si se aleja mucho (> 16 bloques)
                                    double d = cat.getLocation().distance(targetPlayer.getLocation());
                                    if (d > 16.0) {
                                        cat.teleport(targetPlayer.getLocation().add(1.0, 0, 1.0));
                                    }
                                }
                            }
                        }
                    } else {
                        it.remove();
                        activePurgaMobs.remove(catId);
                        activeEntitiesCache.remove(catId);
                    }
                }
            }
        }, 10L, 10L);
    }

    public void detenerMonitoreoCreepers() {
        if (mainProximityTask != null) {
            mainProximityTask.cancel();
            mainProximityTask = null;
        }
    }

    public void resetOleadas() {
        this.numeroOleada = 0;
    }

    public int getNumeroOleada() {
        return numeroOleada;
    }

    public boolean hasLiveDanisNear(Player player) {
        for (UUID catId : danisCats) {
            LivingEntity entity = getLivingEntity(catId);
            if (entity instanceof Cat && entity.isValid() && !entity.isDead()) {
                if (entity.getWorld().equals(player.getWorld()) && entity.getLocation().distanceSquared(player.getLocation()) <= (40.0 * 40.0)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Genera una oleada de mobs OP cerca de todos los jugadores en línea en mundos permitidos.
     */
    public void spawnWaveForAllPlayers() {
        if (!plugin.getConfig().getBoolean("mobs-op.activado", true)) {
            return;
        }

        numeroOleada++;

        List<String> mundosPermitidos = plugin.getConfig().getStringList("mundos-activos");
        int cantidadPorJugador = plugin.getConfig().getInt("mobs-op.mobs-por-jugador", 3);
        int maxMobsTotalesPorJugador = plugin.getConfig().getInt("mobs-op.max-mobs-vivos-por-jugador", 6);
        int minDist = plugin.getConfig().getInt("mobs-op.distancia-minima", 8);
        int maxDist = plugin.getConfig().getInt("mobs-op.distancia-maxima", 22);

        List<String> tiposConfig = plugin.getConfig().getStringList("mobs-op.tipos-mobs-permitidos");
        List<EntityType> tiposPermitidos = new ArrayList<>();

        for (String tipoStr : tiposConfig) {
            try {
                EntityType type = EntityType.valueOf(tipoStr.toUpperCase());
                tiposPermitidos.add(type);
            } catch (Exception ignored) {}
        }

        if (tiposPermitidos.isEmpty()) {
            tiposPermitidos.add(EntityType.ZOMBIE);
            tiposPermitidos.add(EntityType.SKELETON);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!mundosPermitidos.contains(player.getWorld().getName())) continue;

            // Contar mobs hostiles vivos cercanos para respetar el límite de rendimiento
            int hostilesCercanos = 0;
            for (UUID hId : activeHostileMobs) {
                LivingEntity le = getLivingEntity(hId);
                if (le != null && le.isValid() && !le.isDead() && le.getWorld().equals(player.getWorld())) {
                    if (le.getLocation().distanceSquared(player.getLocation()) <= (48.0 * 48.0)) {
                        hostilesCercanos++;
                    }
                }
            }

            int aGenerar = Math.max(0, Math.min(cantidadPorJugador, maxMobsTotalesPorJugador - hostilesCercanos));

            // 1. Spawning de Mobs Hostiles OP
            for (int i = 0; i < aGenerar; i++) {
                EntityType tipoSeleccionado = tiposPermitidos.get(random.nextInt(tiposPermitidos.size()));
                Location spawnLoc = calcularUbicacionSegura(player.getLocation(), minDist, maxDist);
                if (spawnLoc != null) {
                    spawnOpMob(spawnLoc, tipoSeleccionado, player);
                }
            }

            // 2. Spawning del Gato "DANIS" (1 por jugador si no tiene ya uno cerca)
            if (plugin.getConfig().getBoolean("mobs-especiales.danis.activado", true)) {
                if (!hasLiveDanisNear(player)) {
                    Location danisLoc = calcularUbicacionSegura(player.getLocation(), 3, 6);
                    if (danisLoc != null) {
                        spawnDanisCat(danisLoc, player);
                    }
                }
            }

            // 3. Spawning del Lobo "JUST" (Máximo 1 por jugador en total)
            if (plugin.getConfig().getBoolean("mobs-especiales.just.activado", true)) {
                if (!hasLiveJustNear(player)) {
                    Location justLoc = calcularUbicacionSegura(player.getLocation(), 4, 8);
                    if (justLoc != null) {
                        spawnWildJustWolf(justLoc, player);
                    }
                }
            }
        }
    }

    /**
     * Spawnea y equipa a un mob OP con escalado de oleada.
     */
    public void spawnOpMob(Location loc, EntityType type, Player target) {
        World world = loc.getWorld();
        if (world == null) return;

        LivingEntity entity;
        try {
            entity = (LivingEntity) world.spawnEntity(loc, type);
        } catch (Exception e) {
            entity = (LivingEntity) world.spawnEntity(loc, EntityType.ZOMBIE);
        }

        // Marcar entidad con PersistentDataContainer
        entity.getPersistentDataContainer().set(mobKey, PersistentDataType.BYTE, (byte) 1);
        activePurgaMobs.add(entity.getUniqueId());
        activeHostileMobs.add(entity.getUniqueId());
        activeEntitiesCache.put(entity.getUniqueId(), entity);

        // Configurar nombre con número de oleada
        String nombre = plugin.getConfig().getString("mobs-op.nombre-mob", "&4&l☠ ASESINO DE LA PURGA &7(Oleada %oleada%) ☠")
                .replace("%oleada%", String.valueOf(numeroOleada));
        entity.setCustomName(plugin.colorize(nombre));
        entity.setCustomNameVisible(true);

        // Escalado de atributos (Vida)
        aplicarEscaladoVida(entity);

        // Equipamiento OP según el tipo de mob
        equiparMob(entity);

        // Habilidad específica de Creeper
        if (entity instanceof Creeper) {
            Creeper creeper = (Creeper) entity;
            creeper.getPersistentDataContainer().set(creeperKey, PersistentDataType.BYTE, (byte) 1);
            creeperMobs.add(creeper.getUniqueId());
            boolean electrico = plugin.getConfig().getBoolean("mobs-op.habilidades-especiales.creeper.cargado-electrico", true);
            if (electrico) {
                creeper.setPowered(true);
            }
        }

        // Efectos de poción escalados (Velocidad, Fuerza, Resistencia, etc.)
        aplicarEfectos(entity);

        // Fijar como objetivo al jugador más cercano
        if (entity instanceof Mob && target != null && target.isOnline()) {
            ((Mob) entity).setTarget(target);
        }
    }

    private void aplicarEscaladoVida(LivingEntity entity) {
        boolean escaladoActivo = plugin.getConfig().getBoolean("mobs-op.dificultad.escalado-por-oleadas.activado", true);
        if (!escaladoActivo) return;

        int maxOleadas = plugin.getConfig().getInt("mobs-op.dificultad.escalado-por-oleadas.maximo-oleadas-escalado", 10);
        int oleadasEfectivas = Math.min(numeroOleada, maxOleadas);
        double vidaExtraPorOleada = plugin.getConfig().getDouble("mobs-op.dificultad.escalado-por-oleadas.vida-extra-por-oleada", 10.0);

        double vidaExtra = Math.max(0, (oleadasEfectivas - 1) * vidaExtraPorOleada);
        if (vidaExtra > 0) {
            try {
                AttributeInstance maxHealthAttr = getAttributeInstance(entity, "GENERIC_MAX_HEALTH", "MAX_HEALTH");
                if (maxHealthAttr != null) {
                    double nuevaVidaMax = maxHealthAttr.getBaseValue() + vidaExtra;
                    maxHealthAttr.setBaseValue(nuevaVidaMax);
                    entity.setHealth(nuevaVidaMax);
                }
            } catch (Exception ignored) {}
        }
    }

    private void equiparMob(LivingEntity entity) {
        EntityEquipment eq = entity.getEquipment();
        if (eq == null) return;

        String tier = plugin.getConfig().getString("mobs-op.tier-armadura", "NETHERITE").toUpperCase();
        if (tier.equals("GOLD")) {
            tier = "GOLDEN";
        } else if (tier.equals("CHAIN")) {
            tier = "CHAINMAIL";
        }

        Material helmetMat = Material.matchMaterial(tier + "_HELMET");
        Material chestMat = Material.matchMaterial(tier + "_CHESTPLATE");
        Material legsMat = Material.matchMaterial(tier + "_LEGGINGS");
        Material bootsMat = Material.matchMaterial(tier + "_BOOTS");
        Material swordMat = Material.matchMaterial(tier + "_SWORD");

        if (helmetMat == null) helmetMat = Material.DIAMOND_HELMET;
        if (chestMat == null) chestMat = Material.DIAMOND_CHESTPLATE;
        if (legsMat == null) legsMat = Material.DIAMOND_LEGGINGS;
        if (bootsMat == null) bootsMat = Material.DIAMOND_BOOTS;
        if (swordMat == null) swordMat = Material.DIAMOND_SWORD;

        // Equipamiento según la naturaleza del mob
        EntityType type = entity.getType();

        if (type == EntityType.SKELETON || type == EntityType.STRAY) {
            ItemStack bow = new ItemStack(Material.BOW);
            encantar(bow, "POWER", 10);
            encantar(bow, "FLAME", 1);
            encantar(bow, "PUNCH", 2);
            encantar(bow, "UNBREAKING", 10);
            eq.setItemInMainHand(bow);
            eq.setHelmet(crearItemProtegido(helmetMat));
            eq.setChestplate(crearItemProtegido(chestMat));
            eq.setLeggings(crearItemProtegido(legsMat));
            eq.setBoots(crearItemProtegido(bootsMat));
        } else if (type == EntityType.PILLAGER) {
            ItemStack crossbow = new ItemStack(Material.CROSSBOW);
            encantar(crossbow, "QUICK_CHARGE", 3);
            encantar(crossbow, "MULTISHOT", 1);
            encantar(crossbow, "PIERCING", 4);
            encantar(crossbow, "UNBREAKING", 10);
            eq.setItemInMainHand(crossbow);
            eq.setHelmet(crearItemProtegido(helmetMat));
            eq.setChestplate(crearItemProtegido(chestMat));
            eq.setLeggings(crearItemProtegido(legsMat));
            eq.setBoots(crearItemProtegido(bootsMat));
        } else if (type == EntityType.DROWNED) {
            ItemStack trident = new ItemStack(Material.TRIDENT);
            encantar(trident, "DAMAGE_ALL", 255);
            encantar(trident, "SHARPNESS", 255);
            encantar(trident, "IMPALING", 5);
            encantar(trident, "LOYALTY", 3);
            eq.setItemInMainHand(trident);
            eq.setHelmet(crearItemProtegido(helmetMat));
            eq.setChestplate(crearItemProtegido(chestMat));
            eq.setLeggings(crearItemProtegido(legsMat));
            eq.setBoots(crearItemProtegido(bootsMat));
        } else if (type == EntityType.CREEPER || type == EntityType.SPIDER || type == EntityType.CAVE_SPIDER
                || type == EntityType.ENDERMAN || type == EntityType.RAVAGER || type == EntityType.WITCH) {
            // Mobs no humanoides no usan slots de armadura estándar
        } else {
            // Humanoides (Zombie, Husk, Vindicator, Wither Skeleton, Piglin Brute, etc.)
            ItemStack weapon = (type == EntityType.VINDICATOR || type == EntityType.PIGLIN_BRUTE)
                    ? crearItemProtegido(Material.matchMaterial(tier + "_AXE") != null ? Material.matchMaterial(tier + "_AXE") : Material.DIAMOND_AXE)
                    : crearItemProtegido(swordMat);

            encantar(weapon, "DAMAGE_ALL", 255);
            encantar(weapon, "SHARPNESS", 255);
            encantar(weapon, "FIRE_ASPECT", 2);

            eq.setHelmet(crearItemProtegido(helmetMat));
            eq.setChestplate(crearItemProtegido(chestMat));
            eq.setLeggings(crearItemProtegido(legsMat));
            eq.setBoots(crearItemProtegido(bootsMat));
            eq.setItemInMainHand(weapon);
        }

        // IMPEDIR QUE SUELTEN ARMADURAS O ARMAS OP AL MORIR (DROP CHANCE 0.0%)
        eq.setHelmetDropChance(0.0f);
        eq.setChestplateDropChance(0.0f);
        eq.setLeggingsDropChance(0.0f);
        eq.setBootsDropChance(0.0f);
        eq.setItemInMainHandDropChance(0.0f);
        eq.setItemInOffHandDropChance(0.0f);
    }

    private ItemStack crearItemProtegido(Material mat) {
        if (mat == null) mat = Material.DIAMOND_SWORD;
        ItemStack item = new ItemStack(mat);
        encantar(item, "PROTECTION_ENVIRONMENTAL", 10);
        encantar(item, "PROTECTION", 10);
        encantar(item, "DURABILITY", 10);
        encantar(item, "UNBREAKING", 10);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(true);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void encantar(ItemStack item, String enchantmentName, int level) {
        if (item == null || enchantmentName == null) return;
        try {
            // 1. Intentar por nombre directo Bukkit
            Enchantment ench = Enchantment.getByName(enchantmentName.toUpperCase());

            // 2. Mapear nombres comunes de Minecraft a Bukkit
            if (ench == null) {
                String u = enchantmentName.toUpperCase();
                if (u.equals("POWER")) ench = Enchantment.getByName("ARROW_DAMAGE");
                else if (u.equals("FLAME")) ench = Enchantment.getByName("ARROW_FIRE");
                else if (u.equals("PUNCH")) ench = Enchantment.getByName("ARROW_KNOCKBACK");
                else if (u.equals("SHARPNESS")) ench = Enchantment.getByName("DAMAGE_ALL");
                else if (u.equals("PROTECTION")) ench = Enchantment.getByName("PROTECTION_ENVIRONMENTAL");
                else if (u.equals("UNBREAKING")) ench = Enchantment.getByName("DURABILITY");
                else if (u.equals("INFINITY")) ench = Enchantment.getByName("ARROW_INFINITE");
            }

            // 3. Intentar por NamespacedKey
            if (ench == null) {
                try {
                    ench = Enchantment.getByKey(NamespacedKey.minecraft(enchantmentName.toLowerCase()));
                } catch (Throwable ignored) {}
            }

            if (ench != null) {
                item.addUnsafeEnchantment(ench, level);
            }
        } catch (Exception ignored) {}
    }

    private void aplicarEfectos(LivingEntity entity) {
        int fuerzaAmp = 4;
        int speedAmp = 1;
        int resistAmp = 1;

        // Escalado de pociones según oleadas
        boolean escaladoActivo = plugin.getConfig().getBoolean("mobs-op.dificultad.escalado-por-oleadas.activado", true);
        if (escaladoActivo) {
            int intervaloPociones = plugin.getConfig().getInt("mobs-op.dificultad.escalado-por-oleadas.aumento-pociones-cada-x-oleadas", 2);
            int bonus = (intervaloPociones > 0) ? (numeroOleada / intervaloPociones) : 0;
            fuerzaAmp = Math.min(10, fuerzaAmp + bonus);
            speedAmp = Math.min(4, speedAmp + (bonus > 1 ? 1 : 0));
            resistAmp = Math.min(4, resistAmp + (bonus > 1 ? 1 : 0));
        }

        agregarEfectoSeguro(entity, "SPEED", speedAmp);
        agregarEfectoSeguro(entity, "FIRE_RESISTANCE", 0);
        agregarEfectoSeguro(entity, "INCREASE_DAMAGE", fuerzaAmp);
        agregarEfectoSeguro(entity, "DAMAGE_RESISTANCE", resistAmp);
    }

    private void agregarEfectoSeguro(LivingEntity entity, String effectName, int amplifier) {
        try {
            PotionEffectType type = PotionEffectType.getByName(effectName);
            if (type != null) {
                entity.addPotionEffect(new PotionEffect(type, Integer.MAX_VALUE, amplifier, false, false));
            }
        } catch (Exception ignored) {}
    }

    /**
     * Teletransporta al Creeper a una ubicación táctica cuando recibe daño.
     */
    public void teletransportarCreeper(Creeper creeper, Entity atacante) {
        if (creeper == null || !creeper.isValid() || creeper.isDead()) return;

        int dist = plugin.getConfig().getInt("mobs-op.habilidades-especiales.creeper.distancia-teletransporte", 6);
        Location baseLoc = (atacante != null) ? atacante.getLocation() : creeper.getLocation();
        World world = baseLoc.getWorld();
        if (world == null) return;

        // Partículas y sonido antes del teletransporte
        spawnParticleSafely(world, creeper.getLocation(), 20, 0.5, 1.0, 0.5, 0.1, "PORTAL");
        playSoundSafely(world, creeper.getLocation(), 1.0f, 1.0f, "ENTITY_ENDERMAN_TELEPORT", "ENDERMAN_TELEPORT");

        Location dest = calcularUbicacionSegura(baseLoc, 2, Math.max(3, dist));
        if (dest != null) {
            creeper.teleport(dest);
            spawnParticleSafely(world, dest, 20, 0.5, 1.0, 0.5, 0.1, "PORTAL");
            playSoundSafely(world, dest, 1.0f, 1.0f, "ENTITY_ENDERMAN_TELEPORT", "ENDERMAN_TELEPORT");

            if (atacante instanceof LivingEntity) {
                creeper.setTarget((LivingEntity) atacante);
            }
        }
    }

    private Location calcularUbicacionSegura(Location center, int minDist, int maxDist) {
        World world = center.getWorld();
        if (world == null) return null;

        int dist = minDist + random.nextInt(Math.max(1, maxDist - minDist + 1));
        double angle = random.nextDouble() * 2 * Math.PI;

        int x = center.getBlockX() + (int) (dist * Math.cos(angle));
        int z = center.getBlockZ() + (int) (dist * Math.sin(angle));
        int chunkX = x >> 4;
        int chunkZ = z >> 4;

        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            world.getChunkAt(chunkX, chunkZ);
        }

        int centerY = center.getBlockY();
        int bestY = centerY;
        boolean foundFloor = false;
        int minH = getMinHeightSafely(world);
        int maxH = world.getMaxHeight();

        for (int dy = 0; dy <= 5; dy++) {
            int checkDownY = centerY - dy;
            if (checkDownY > minH && checkDownY < maxH - 2) {
                Material floor = world.getBlockAt(x, checkDownY, z).getType();
                Material feet = world.getBlockAt(x, checkDownY + 1, z).getType();
                Material head = world.getBlockAt(x, checkDownY + 2, z).getType();
                if (floor.isSolid() && !floor.name().contains("LAVA") && !feet.isSolid() && !head.isSolid()) {
                    bestY = checkDownY + 1;
                    foundFloor = true;
                    break;
                }
            }
            if (dy > 0) {
                int checkUpY = centerY + dy;
                if (checkUpY > minH && checkUpY < maxH - 2) {
                    Material floor = world.getBlockAt(x, checkUpY, z).getType();
                    Material feet = world.getBlockAt(x, checkUpY + 1, z).getType();
                    Material head = world.getBlockAt(x, checkUpY + 2, z).getType();
                    if (floor.isSolid() && !floor.name().contains("LAVA") && !feet.isSolid() && !head.isSolid()) {
                        bestY = checkUpY + 1;
                        foundFloor = true;
                        break;
                    }
                }
            }
        }

        if (!foundFloor) {
            int highest = world.getHighestBlockYAt(x, z);
            if (highest > minH && highest < 315) {
                bestY = highest;
            }
        }

        return new Location(world, x + 0.5, bestY, z + 0.5);
    }

    public boolean isPurgaMob(LivingEntity entity) {
        if (entity == null) return false;
        if (entity.getPersistentDataContainer().has(mobKey, PersistentDataType.BYTE)) {
            return true;
        }
        return activePurgaMobs.contains(entity.getUniqueId());
    }

    public boolean isPurgaCreeper(LivingEntity entity) {
        if (entity == null) return false;
        if (entity.getPersistentDataContainer().has(creeperKey, PersistentDataType.BYTE)) {
            return true;
        }
        return isPurgaMob(entity) && (entity instanceof Creeper);
    }

    /**
     * Spawnea un Zombie con la cabeza y nombre de un jugador que acaba de morir.
     */
    public Zombie spawnZombieJugadorMuerto(Player victim, Location loc, List<ItemStack> drops, Player target) {
        World world = loc.getWorld();
        if (world == null) return null;

        Zombie zombie = (Zombie) world.spawnEntity(loc, EntityType.ZOMBIE);

        zombie.getPersistentDataContainer().set(mobKey, PersistentDataType.BYTE, (byte) 1);
        zombie.getPersistentDataContainer().set(playerZombieKey, PersistentDataType.BYTE, (byte) 1);
        zombie.getPersistentDataContainer().set(playerVictimNameKey, PersistentDataType.STRING, victim.getName());
        activePurgaMobs.add(zombie.getUniqueId());

        String nombreConfig = plugin.getConfig().getString("zombie-jugador-muerto.nombre-zombie", "&4&l☠ %jugador% (Zombificado) ☠")
                .replace("%jugador%", victim.getName());
        zombie.setCustomName(plugin.colorize(nombreConfig));
        zombie.setCustomNameVisible(true);

        EntityEquipment eq = zombie.getEquipment();
        if (eq != null) {
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta sm = (SkullMeta) skull.getItemMeta();
            if (sm != null) {
                sm.setOwningPlayer(victim);
                skull.setItemMeta(sm);
            }
            eq.setHelmet(skull);
            eq.setHelmetDropChance(0.0f);

            eq.setChestplate(crearItemProtegido(Material.DIAMOND_CHESTPLATE));
            eq.setLeggings(crearItemProtegido(Material.DIAMOND_LEGGINGS));
            eq.setBoots(crearItemProtegido(Material.DIAMOND_BOOTS));
            eq.setItemInMainHand(crearItemProtegido(Material.DIAMOND_SWORD));

            eq.setChestplateDropChance(0.0f);
            eq.setLeggingsDropChance(0.0f);
            eq.setBootsDropChance(0.0f);
            eq.setItemInMainHandDropChance(0.0f);
            eq.setItemInOffHandDropChance(0.0f);
        }

        aplicarEfectos(zombie);

        // Guardar copia del inventario si está configurado
        if (plugin.getConfig().getBoolean("zombie-jugador-muerto.guardar-inventario-en-zombie", true) && drops != null) {
            List<ItemStack> copia = new ArrayList<>();
            for (ItemStack it : drops) {
                if (it != null && it.getType() != Material.AIR) {
                    copia.add(it.clone());
                }
            }
            playerZombieDrops.put(zombie.getUniqueId(), copia);
        }

        if (target != null && target.isOnline()) {
            zombie.setTarget(target);
        }

        return zombie;
    }

    public boolean isPlayerZombie(LivingEntity entity) {
        if (entity == null) return false;
        if (entity.getPersistentDataContainer().has(playerZombieKey, PersistentDataType.BYTE)) {
            return true;
        }
        return playerZombieDrops.containsKey(entity.getUniqueId());
    }

    public String obtenerVictimaZombie(LivingEntity entity) {
        if (entity == null) return null;
        if (entity.getPersistentDataContainer().has(playerVictimNameKey, PersistentDataType.STRING)) {
            return entity.getPersistentDataContainer().get(playerVictimNameKey, PersistentDataType.STRING);
        }
        return null;
    }

    public List<ItemStack> obtenerYRemoverBotinZombie(UUID zombieId) {
        return playerZombieDrops.remove(zombieId);
    }

    public void spawnDanisCat(Location loc, Player target) {
        World world = loc.getWorld();
        if (world == null) return;

        Cat cat = (Cat) world.spawnEntity(loc, EntityType.CAT);
        cat.getPersistentDataContainer().set(mobKey, PersistentDataType.BYTE, (byte) 1);
        cat.getPersistentDataContainer().set(danisKey, PersistentDataType.BYTE, (byte) 1);
        activePurgaMobs.add(cat.getUniqueId());
        danisCats.add(cat.getUniqueId());
        activeEntitiesCache.put(cat.getUniqueId(), cat);

        String nombre = plugin.getConfig().getString("mobs-especiales.danis.nombre", "&d&l✦ DANIS ✦");
        cat.setCustomName(plugin.colorize(nombre));
        cat.setCustomNameVisible(true);
        cat.setAdult();

        double vidaMax = plugin.getConfig().getDouble("mobs-especiales.danis.vida-maxima", 120.0);
        try {
            AttributeInstance maxHealthAttr = getAttributeInstance(cat, "GENERIC_MAX_HEALTH", "MAX_HEALTH");
            if (maxHealthAttr != null) {
                maxHealthAttr.setBaseValue(vidaMax);
                cat.setHealth(vidaMax);
            }
        } catch (Exception ignored) {}

        agregarEfectoSeguro(cat, "DAMAGE_RESISTANCE", 2);
        agregarEfectoSeguro(cat, "SPEED", 1);
    }

    public void spawnWildJustWolf(Location loc, Player target) {
        World world = loc.getWorld();
        if (world == null) return;

        Wolf wolf = (Wolf) world.spawnEntity(loc, EntityType.WOLF);
        wolf.getPersistentDataContainer().set(mobKey, PersistentDataType.BYTE, (byte) 1);
        wolf.getPersistentDataContainer().set(justKey, PersistentDataType.BYTE, (byte) 1);
        activePurgaMobs.add(wolf.getUniqueId());
        justWolves.add(wolf.getUniqueId());
        activeEntitiesCache.put(wolf.getUniqueId(), wolf);

        String nombre = plugin.getConfig().getString("mobs-especiales.just.nombre-salvaje", "&b&l🐺 JUST 🐺 &7(Salvaje)");
        wolf.setCustomName(plugin.colorize(nombre));
        wolf.setCustomNameVisible(true);
        wolf.setAdult();

        double vidaMax = plugin.getConfig().getDouble("mobs-especiales.just.vida-maxima", 200.0);
        try {
            AttributeInstance maxHealthAttr = getAttributeInstance(wolf, "GENERIC_MAX_HEALTH", "MAX_HEALTH");
            if (maxHealthAttr != null) {
                maxHealthAttr.setBaseValue(vidaMax);
                wolf.setHealth(vidaMax);
            }
        } catch (Exception ignored) {}

        double dano = plugin.getConfig().getDouble("mobs-especiales.just.dano-ataque", 12.0);
        try {
            AttributeInstance attackAttr = getAttributeInstance(wolf, "GENERIC_ATTACK_DAMAGE", "ATTACK_DAMAGE");
            if (attackAttr != null) {
                attackAttr.setBaseValue(dano);
            }
        } catch (Exception ignored) {}

        agregarEfectoSeguro(wolf, "DAMAGE_RESISTANCE", 1);
        agregarEfectoSeguro(wolf, "SPEED", 1);
    }

    public boolean hasLiveTamedJust(UUID playerUUID) {
        UUID wolfId = justOwners.get(playerUUID);
        if (wolfId != null) {
            LivingEntity e = getLivingEntity(wolfId);
            if (e instanceof Wolf && e.isValid() && !e.isDead() && ((Wolf) e).isTamed()) {
                return true;
            }
        }
        for (UUID wId : justWolves) {
            LivingEntity e = getLivingEntity(wId);
            if (e instanceof Wolf && e.isValid() && !e.isDead() && ((Wolf) e).isTamed()) {
                Wolf w = (Wolf) e;
                if (w.getOwner() != null && w.getOwner().getUniqueId().equals(playerUUID)) {
                    justOwners.put(playerUUID, wId);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasLiveJustNear(Player player) {
        if (player == null || player.getWorld() == null) return false;
        if (hasLiveTamedJust(player.getUniqueId())) return true;
        for (UUID wolfId : justWolves) {
            LivingEntity entity = getLivingEntity(wolfId);
            if (entity instanceof Wolf && entity.isValid() && !entity.isDead()) {
                if (entity.getWorld().equals(player.getWorld()) && entity.getLocation().distanceSquared(player.getLocation()) <= (40.0 * 40.0)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void registrarDuenoJust(UUID playerUUID, UUID wolfUUID) {
        justOwners.put(playerUUID, wolfUUID);
    }

    public boolean isDanisCat(LivingEntity entity) {
        if (entity == null) return false;
        if (entity.getPersistentDataContainer().has(danisKey, PersistentDataType.BYTE)) {
            return true;
        }
        return danisCats.contains(entity.getUniqueId());
    }

    public boolean isJustWolf(LivingEntity entity) {
        if (entity == null) return false;
        if (entity.getPersistentDataContainer().has(justKey, PersistentDataType.BYTE)) {
            return true;
        }
        return justWolves.contains(entity.getUniqueId());
    }

    public boolean isHostilePurgaMob(LivingEntity entity) {
        if (entity == null) return false;
        return isPurgaMob(entity) && !isJustWolf(entity) && !isDanisCat(entity) && !isPlayerZombie(entity);
    }

    public boolean isBypassTotemVictim(UUID playerUUID) {
        return bypassTotemVictims.contains(playerUUID);
    }

    public void removerBypassTotemVictim(UUID playerUUID) {
        bypassTotemVictims.remove(playerUUID);
    }

    public void agregarBypassTotemVictim(UUID playerUUID) {
        bypassTotemVictims.add(playerUUID);
    }

    public void removerMob(UUID mobId) {
        if (mobId == null) return;
        activePurgaMobs.remove(mobId);
        activeHostileMobs.remove(mobId);
        creeperMobs.remove(mobId);
        danisCats.remove(mobId);
        justWolves.remove(mobId);
        playerZombieDrops.remove(mobId);
        activeEntitiesCache.remove(mobId);
    }

    public void limpiarMobsDePurga() {
        detenerMonitoreoCreepers();
        playerZombieDrops.clear();
        danisCats.clear();
        creeperMobs.clear();
        activeHostileMobs.clear();
        bypassTotemVictims.clear();

        boolean persistirJust = plugin.getConfig().getBoolean("mobs-especiales.just.persistir-tras-purga", true);

        if (!plugin.getConfig().getBoolean("mobs-op.eliminar-mobs-al-finalizar", true)) {
            activePurgaMobs.clear();
            activeEntitiesCache.clear();
            return;
        }

        for (UUID mobId : new java.util.HashSet<>(activePurgaMobs)) {
            LivingEntity entity = getLivingEntity(mobId);
            if (entity != null && entity.isValid()) {
                if (entity instanceof Wolf && ((Wolf) entity).isTamed() && persistirJust) {
                    continue;
                }
                entity.remove();
            }
        }
        activePurgaMobs.clear();
        activeEntitiesCache.clear();
    }

    private static AttributeInstance getAttributeInstance(LivingEntity entity, String... names) {
        if (entity == null) return null;
        for (String name : names) {
            try {
                Attribute attr = Attribute.valueOf(name);
                if (attr != null) {
                    AttributeInstance inst = entity.getAttribute(attr);
                    if (inst != null) return inst;
                }
            } catch (Throwable ignored) {}
        }
        return null;
    }

    public static int getMinHeightSafely(World world) {
        if (world == null) return 0;
        try {
            return world.getMinHeight();
        } catch (Throwable ignored) {
            return 0;
        }
    }

    public static void spawnParticleSafely(World world, Location loc, int count, double ox, double oy, double oz, double speed, String... particleNames) {
        if (world == null || loc == null) return;
        for (String name : particleNames) {
            try {
                Particle p = Particle.valueOf(name.toUpperCase());
                if (p != null) {
                    world.spawnParticle(p, loc, count, ox, oy, oz, speed);
                    return;
                }
            } catch (Throwable ignored) {}
        }
    }

    public static void playSoundSafely(World world, Location loc, float volume, float pitch, String... soundNames) {
        if (world == null || loc == null) return;
        for (String name : soundNames) {
            try {
                Sound s = Sound.valueOf(name.toUpperCase());
                if (s != null) {
                    world.playSound(loc, s, volume, pitch);
                    return;
                }
            } catch (Throwable ignored) {}
        }
    }
}
