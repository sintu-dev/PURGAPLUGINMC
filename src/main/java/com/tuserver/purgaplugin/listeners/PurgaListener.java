package com.tuserver.purgaplugin.listeners;

import com.tuserver.purgaplugin.PurgaPlugin;
import com.tuserver.purgaplugin.mobs.PurgaMobManager;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PurgaListener implements Listener {

    private final PurgaPlugin plugin;
    private final Random random = new Random();

    public PurgaListener(PurgaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Prioridad HIGHEST e ignoreCancelled = false:
     * 1. Maneja 1-Hit Kill de Mobs OP de la Purga.
     * 2. Maneja Teletransporte del Creeper al recibir daño.
     * 3. Protege a los animales del daño de jugadores durante la purga.
     * 4. Anula cancelaciones de daño de protecciones en PvP entre jugadores.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!plugin.isPurgaActiva()) return;

        List<String> mundosPermitidos = plugin.getConfig().getStringList("mundos-activos");
        if (!mundosPermitidos.contains(event.getEntity().getWorld().getName())) return;

        Player attacker = null;
        LivingEntity damagerLiving = null;

        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
            damagerLiving = attacker;
        } else if (event.getDamager() instanceof LivingEntity) {
            damagerLiving = (LivingEntity) event.getDamager();
        } else if (event.getDamager() instanceof Projectile) {
            Projectile proj = (Projectile) event.getDamager();
            if (proj.getShooter() instanceof Player) {
                attacker = (Player) proj.getShooter();
                damagerLiving = attacker;
            } else if (proj.getShooter() instanceof LivingEntity) {
                damagerLiving = (LivingEntity) proj.getShooter();
            }
        }

        // 1. Verificar si el atacante es un Mob OP Hostil de la Purga (1-Hit Kill)
        if (event.getEntity() instanceof Player && damagerLiving != null) {
            if (plugin.getMobManager().isHostilePurgaMob(damagerLiving)) {
                if (plugin.getConfig().getBoolean("mobs-op.dificultad.un-hit-kill", true) || plugin.getConfig().getBoolean("mobs-op.un-hit-kill", true)) {
                    event.setDamage(1000.0); // Daño letal instantáneo
                }
                return;
            }
        }

        // 2. Habilidad de Creeper de la Purga: Teletransporte al recibir daño
        if (event.getEntity() instanceof Creeper) {
            Creeper creeper = (Creeper) event.getEntity();
            if (plugin.getMobManager().isPurgaCreeper(creeper)) {
                if (plugin.getConfig().getBoolean("mobs-op.habilidades-especiales.creeper.teletransporte-al-dano", true)) {
                    plugin.getMobManager().teletransportarCreeper(creeper, damagerLiving);
                }
            }
        }

        // 3. Prohibir daño a animales durante la purga
        if (plugin.getConfig().getBoolean("proteger-animales", true) && event.getEntity() instanceof Animals) {
            if (attacker != null) {
                event.setCancelled(true);
                String msg = plugin.getConfig().getString("mensajes.animal-protegido", "&c¡Durante la Purga está prohibido atacar a los animales!");
                attacker.sendMessage(plugin.colorize(msg));
                return;
            }
        }

        // 4. Forzar PvP entre jugadores en zonas de protección (si no están en creativo/espectador)
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            if (attacker != null && !attacker.equals(victim)) {
                if (victim.getGameMode() != org.bukkit.GameMode.CREATIVE && victim.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                    if (event.isCancelled()) {
                        event.setCancelled(false);
                    }
                }
            }
        }
    }

    /**
     * Maneja la probabilidad de fallo para los Tótems de Inmortalidad (Totem of Undying)
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityResurrect(EntityResurrectEvent event) {
        if (!plugin.isPurgaActiva()) return;
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        List<String> mundosPermitidos = plugin.getConfig().getStringList("mundos-activos");
        if (!mundosPermitidos.contains(player.getWorld().getName())) return;

        // 1. Bypass absoluto de tótem si fue víctima de la explosión traicionera de DANIS
        if (plugin.getMobManager().isBypassTotemVictim(player.getUniqueId())) {
            event.setCancelled(true);
            plugin.getMobManager().removerBypassTotemVictim(player.getUniqueId());
            return;
        }

        double probFallo = plugin.getConfig().getDouble("totems.probabilidad-fallo", 40.0);
        if (probFallo <= 0) return;

        if (random.nextDouble() * 100.0 < probFallo) {
            // Cancelar la resurrección -> El jugador muere
            event.setCancelled(true);

            // Consumir el tótem si está configurado (evitando ítems fantasma con cantidad 0)
            if (plugin.getConfig().getBoolean("totems.consumir-totem-al-fallar", true)) {
                ItemStack main = player.getInventory().getItemInMainHand();
                ItemStack off = player.getInventory().getItemInOffHand();

                if (main != null && main.getType() == Material.TOTEM_OF_UNDYING) {
                    if (main.getAmount() <= 1) {
                        player.getInventory().setItemInMainHand(null);
                    } else {
                        main.setAmount(main.getAmount() - 1);
                        player.getInventory().setItemInMainHand(main);
                    }
                } else if (off != null && off.getType() == Material.TOTEM_OF_UNDYING) {
                    if (off.getAmount() <= 1) {
                        player.getInventory().setItemInOffHand(null);
                    } else {
                        off.setAmount(off.getAmount() - 1);
                        player.getInventory().setItemInOffHand(off);
                    }
                }
            }

            String soundConfig = plugin.getConfig().getString("totems.sonido-fallo", "BLOCK_RESPAWN_ANCHOR_DEPLETE");
            PurgaMobManager.playSoundSafely(player.getWorld(), player.getLocation(), 1.2f, 0.8f, soundConfig, "BLOCK_RESPAWN_ANCHOR_DEPLETE", "ENTITY_ENDER_EYE_DEATH");
            PurgaMobManager.spawnParticleSafely(player.getWorld(), player.getLocation().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.05, "LARGE_SMOKE", "SMOKE_LARGE");

            String msgFallo = plugin.getConfig().getString("totems.mensaje-fallo", "&4&l☠ ¡TU TÓTEM HA FALLADO! &cLa energía de la Purga neutralizó su poder...");
            player.sendMessage(plugin.colorize(msgFallo));
        }
    }

    /**
     * Limpia armaduras/armas OP y genera la tabla de recompensas y drops con probabilidad,
     * o devuelve el botín del jugador si se derrota a su Zombie caído.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!plugin.isPurgaActiva()) return;

        LivingEntity entity = event.getEntity();

        // 1. Manejo de muerte de Zombie de Jugador Caído
        if (plugin.getMobManager().isPlayerZombie(entity)) {
            event.getDrops().clear();
            event.setDroppedExp(0);

            List<ItemStack> botin = plugin.getMobManager().obtenerYRemoverBotinZombie(entity.getUniqueId());
            if (botin != null && !botin.isEmpty()) {
                event.getDrops().addAll(botin);
            }

            String victimName = plugin.getMobManager().obtenerVictimaZombie(entity);
            if (entity.getKiller() != null) {
                Player killer = entity.getKiller();
                String msgDerrotado = plugin.getConfig().getString("zombie-jugador-muerto.mensaje-zombie-derrotado", "&a&l¡Has derrotado al cadáver de &e%jugador% &a&ly recuperado sus pertenencias!")
                        .replace("%jugador%", victimName != null ? victimName : "tu compañero");
                killer.sendMessage(plugin.colorize(msgDerrotado));
            }
            plugin.getMobManager().removerMob(entity.getUniqueId());
            return;
        }

        // 2. Manejo de mobs de la purga
        if (plugin.getMobManager().isPurgaMob(entity)) {
            // Borrar armaduras y armas chetadas
            event.getDrops().clear();
            event.setDroppedExp(0);

            // Procesar tabla de recompensas configurables
            if (plugin.getConfig().getBoolean("mobs-op.recompensas-drops.activado", true)) {
                boolean soloJugador = plugin.getConfig().getBoolean("mobs-op.recompensas-drops.solo-si-mata-jugador", true);
                Player killer = entity.getKiller();

                if (!soloJugador || killer != null) {
                    int exp = plugin.getConfig().getInt("mobs-op.recompensas-drops.experiencia-al-morir", 25);
                    event.setDroppedExp(exp);

                    ConfigurationSection itemsSec = plugin.getConfig().getConfigurationSection("mobs-op.recompensas-drops.items");
                    if (itemsSec != null) {
                        for (String key : itemsSec.getKeys(false)) {
                            ConfigurationSection itemCfg = itemsSec.getConfigurationSection(key);
                            if (itemCfg == null) continue;

                            String matStr = itemCfg.getString("material");
                            if (matStr == null) continue;
                            Material mat = Material.matchMaterial(matStr.toUpperCase());
                            if (mat == null) continue;

                            double prob = itemCfg.getDouble("probabilidad", 0.0);
                            if (random.nextDouble() * 100.0 <= prob) {
                                int min = itemCfg.getInt("cantidad-min", 1);
                                int max = itemCfg.getInt("cantidad-max", 1);
                                if (max < min) max = min;
                                int cant = min + (max > min ? random.nextInt(max - min + 1) : 0);
                                if (cant <= 0) cant = 1;

                                ItemStack drop = new ItemStack(mat, cant);
                                ItemMeta meta = drop.getItemMeta();
                                if (meta != null) {
                                    if (itemCfg.contains("nombre")) {
                                        meta.setDisplayName(plugin.colorize(itemCfg.getString("nombre")));
                                    }
                                    if (itemCfg.contains("lore")) {
                                        List<String> lore = itemCfg.getStringList("lore");
                                        List<String> coloredLore = new ArrayList<>();
                                        for (String l : lore) {
                                            coloredLore.add(plugin.colorize(l));
                                        }
                                        meta.setLore(coloredLore);
                                    }
                                    drop.setItemMeta(meta);
                                }
                                event.getDrops().add(drop);
                            }
                        }
                    }
                }
            }
            plugin.getMobManager().removerMob(entity.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.isPurgaActiva()) return;

        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        List<String> mundosPermitidos = plugin.getConfig().getStringList("mundos-activos");
        if (!mundosPermitidos.contains(victim.getWorld().getName())) return;

        // Efecto de trueno visual al morir
        if (plugin.getConfig().getBoolean("opciones-adicionales.rayo-al-morir", true)) {
            victim.getWorld().strikeLightningEffect(victim.getLocation());
        }

        // Limpiar cualquier estado pendiente de bypass de tótem
        plugin.getMobManager().removerBypassTotemVictim(victim.getUniqueId());

        if (killer != null && !killer.equals(victim)) {
            plugin.registrarMuerte(victim, killer);
        } else {
            // Murió a manos de un mob o entorno durante la purga
            if (plugin.getConfig().getBoolean("zombie-jugador-muerto.activado", true)) {
                double radioDeteccion = plugin.getConfig().getDouble("zombie-jugador-muerto.radio-deteccion", 25.0);
                double radioSq = radioDeteccion * radioDeteccion;

                List<Player> nearbyCompanions = new ArrayList<>();
                Player closestTarget = null;
                double closestDistSq = Double.MAX_VALUE;

                for (Player p : victim.getWorld().getPlayers()) {
                    if (!p.equals(victim) && p.isValid() && !p.isDead()) {
                        double dSq = p.getLocation().distanceSquared(victim.getLocation());
                        if (dSq <= radioSq) {
                            nearbyCompanions.add(p);
                            if (dSq < closestDistSq) {
                                closestDistSq = dSq;
                                closestTarget = p;
                            }
                        }
                    }
                }

                if (!nearbyCompanions.isEmpty()) {
                    List<ItemStack> drops = new ArrayList<>(event.getDrops());
                    if (plugin.getConfig().getBoolean("zombie-jugador-muerto.guardar-inventario-en-zombie", true)) {
                        event.getDrops().clear();
                    }

                    plugin.getMobManager().spawnZombieJugadorMuerto(victim, victim.getLocation(), drops, closestTarget);
                    PurgaMobManager.playSoundSafely(victim.getWorld(), victim.getLocation(), 1.5f, 0.8f, "ENTITY_ZOMBIE_VILLAGER_CONVERTED", "ZOMBIE_VILLAGER_CONVERTED", "ENTITY_ZOMBIE_INFECT");

                    String msgAparece = plugin.getConfig().getString("zombie-jugador-muerto.mensaje-zombie-aparece", "&4&l¡El cadáver zombificado de &c%jugador% &4&lha despertado! ¡Mátalo para recuperar sus cosas!")
                            .replace("%jugador%", victim.getName());
                    for (Player p : nearbyCompanions) {
                        p.sendMessage(plugin.colorize(msgAparece));
                    }
                }
            }
        }

        // Efectos negativos a compañeros cercanos al morir el usuario
        if (plugin.getConfig().getBoolean("zombie-jugador-muerto.efectos-a-companeros.activado", true)) {
            double radioEfectos = plugin.getConfig().getDouble("zombie-jugador-muerto.efectos-a-companeros.radio-efectos", 20.0);
            double radioEfectosSq = radioEfectos * radioEfectos;
            String msgEfectos = plugin.getConfig().getString("zombie-jugador-muerto.efectos-a-companeros.mensaje-efectos", "&4&l¡Tu compañero &c%jugador% &4&lha caído! &7Una ola de pánico y ceguera nubla tus sentidos...")
                    .replace("%jugador%", victim.getName());
            String sonidoEfectosStr = plugin.getConfig().getString("zombie-jugador-muerto.efectos-a-companeros.sonido-efectos", "ENTITY_WARDEN_HEARTBEAT");

            ConfigurationSection secEfectos = plugin.getConfig().getConfigurationSection("zombie-jugador-muerto.efectos-a-companeros.lista-efectos");

            for (Player p : victim.getWorld().getPlayers()) {
                if (!p.equals(victim) && p.isValid() && !p.isDead()) {
                    if (p.getLocation().distanceSquared(victim.getLocation()) <= radioEfectosSq) {
                        p.sendMessage(plugin.colorize(msgEfectos));
                        PurgaMobManager.playSoundSafely(p.getWorld(), p.getLocation(), 1.2f, 0.8f, sonidoEfectosStr, "ENTITY_WARDEN_HEARTBEAT", "BLOCK_NOTE_BLOCK_BASEDRUM");

                        if (secEfectos != null) {
                            for (String key : secEfectos.getKeys(false)) {
                                ConfigurationSection effCfg = secEfectos.getConfigurationSection(key);
                                if (effCfg == null) continue;

                                String tipoStr = effCfg.getString("tipo");
                                int durSec = effCfg.getInt("duracion-segundos", 10);
                                int nivel = effCfg.getInt("nivel", 1) - 1;
                                if (nivel < 0) nivel = 0;

                                if (tipoStr != null) {
                                    try {
                                        PotionEffectType type = PotionEffectType.getByName(tipoStr.toUpperCase());
                                        if (type != null) {
                                            p.addPotionEffect(new PotionEffect(type, durSec * 20, nivel, false, true, true));
                                        }
                                    } catch (Exception ignored) {}
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Bloquear comandos de escape durante la purga (ej: /spawn, /tpa, /home)
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (!plugin.isPurgaActiva()) return;
        Player player = event.getPlayer();

        if (player.hasPermission("purga.bypass.commands")) return;

        List<String> mundosPermitidos = plugin.getConfig().getStringList("mundos-activos");
        if (!mundosPermitidos.contains(player.getWorld().getName())) return;

        String msg = event.getMessage().toLowerCase().substring(1).trim();
        String commandRoot = msg.split(" ")[0];

        List<String> blockedCommands = plugin.getConfig().getStringList("opciones-adicionales.bloquear-comandos-durante-purga");
        if (blockedCommands.contains(commandRoot)) {
            event.setCancelled(true);
            String warn = plugin.getConfig().getString("opciones-adicionales.mensaje-comando-bloqueado", "&c¡No puedes huir con comandos durante la Purga!");
            player.sendMessage(plugin.colorize(warn));
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (plugin.isPurgaActiva() && plugin.getBossBar() != null) {
            plugin.getBossBar().addPlayer(event.getPlayer());
        }
    }

    /**
     * Maneja la domesticación especial del lobo "JUST" requiriendo 2 Netherite y 2 Diamantes.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!plugin.isPurgaActiva()) return;
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Wolf)) return;

        Wolf wolf = (Wolf) event.getRightClicked();
        if (!plugin.getMobManager().isJustWolf(wolf)) return;
        if (wolf.isTamed()) return;

        Player player = event.getPlayer();
        event.setCancelled(true);

        // Comprobar si el jugador ya tiene un lobo JUST domesticado
        if (plugin.getMobManager().hasLiveTamedJust(player.getUniqueId())) {
            player.sendMessage(plugin.colorize("&c¡Ya tienes a tu compañero JUST activo! Solo puedes tener 1 en total."));
            return;
        }

        // Obtener materiales requeridos de config (por defecto 2 Netherite y 2 Diamantes)
        String mat1Str = plugin.getConfig().getString("mobs-especiales.just.domesticacion.item-1.material", "NETHERITE_INGOT");
        int cant1 = plugin.getConfig().getInt("mobs-especiales.just.domesticacion.item-1.cantidad", 2);
        String mat2Str = plugin.getConfig().getString("mobs-especiales.just.domesticacion.item-2.material", "DIAMOND");
        int cant2 = plugin.getConfig().getInt("mobs-especiales.just.domesticacion.item-2.cantidad", 2);

        Material mat1 = Material.matchMaterial(mat1Str.toUpperCase());
        Material mat2 = Material.matchMaterial(mat2Str.toUpperCase());
        if (mat1 == null) mat1 = Material.NETHERITE_INGOT;
        if (mat2 == null) mat2 = Material.DIAMOND;

        int playerCant1 = contarItems(player, mat1);
        int playerCant2 = contarItems(player, mat2);

        if (playerCant1 >= cant1 && playerCant2 >= cant2) {
            removerItems(player, mat1, cant1);
            removerItems(player, mat2, cant2);

            wolf.setTamed(true);
            wolf.setOwner(player);
            wolf.setCollarColor(DyeColor.CYAN);

            String nombreDom = plugin.getConfig().getString("mobs-especiales.just.nombre-domesticado", "&b&l🐺 JUST 🐺 &7(&e%dueno%&7)")
                    .replace("%dueno%", player.getName());
            wolf.setCustomName(plugin.colorize(nombreDom));
            wolf.setCustomNameVisible(true);

            plugin.getMobManager().registrarDuenoJust(player.getUniqueId(), wolf.getUniqueId());

            PurgaMobManager.spawnParticleSafely(wolf.getWorld(), wolf.getLocation().add(0, 0.5, 0), 12, 0.3, 0.3, 0.3, 0.05, "HEART");
            PurgaMobManager.playSoundSafely(wolf.getWorld(), wolf.getLocation(), 1.2f, 1.0f, "ENTITY_WOLF_HOWL", "ENTITY_WOLF_GROWL", "ENTITY_WOLF_AMBIENT");

            String msgDom = plugin.getConfig().getString("mobs-especiales.just.domesticacion.mensaje-domesticado",
                    "&a&l¡Has domesticado a JUST! &eAhora te protegerá en combate, ahuyentará al gato DANIS y curará tus heridas.");
            player.sendMessage(plugin.colorize(msgDom));
        } else {
            String msgFalta = plugin.getConfig().getString("mobs-especiales.just.domesticacion.mensaje-falta-items",
                    "&c¡Para domesticar a JUST necesitas 2 Lingotes de Netherite y 2 Diamantes en tu inventario!");
            player.sendMessage(plugin.colorize(msgFalta));
        }
    }

    private int contarItems(Player player, Material material) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                total += item.getAmount();
            }
        }
        return total;
    }

    private void removerItems(Player player, Material material, int cantidad) {
        int restante = cantidad;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == material) {
                if (item.getAmount() <= restante) {
                    restante -= item.getAmount();
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - restante);
                    player.getInventory().setItem(i, item);
                    restante = 0;
                    break;
                }
                if (restante <= 0) break;
            }
        }
        player.updateInventory();
    }
}
