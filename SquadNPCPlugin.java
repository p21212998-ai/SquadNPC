package me.squadnpc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SquadNPCPlugin extends JavaPlugin implements Listener {
    private final Map<UUID, List<UUID>> squads = new HashMap<>();
    private NamespacedKey squadKey;

    @Override
    public void onEnable() {
        squadKey = new NamespacedKey(this, "squad_member");
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("squad").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command is for players.");
                return true;
            }
            if (!player.hasPermission("squadnpc.use")) {
                player.sendMessage("§cYou do not have permission: squadnpc.use");
                return true;
            }
            toggleSquad(player);
            return true;
        });

        Bukkit.getScheduler().runTaskTimer(this, this::tickSquads, 10L, 5L);
        getLogger().info("SquadNPC enabled.");
    }

    @Override
    public void onDisable() {
        for (List<UUID> ids : squads.values()) {
            for (UUID id : ids) {
                Entity entity = Bukkit.getEntity(id);
                if (entity != null && entity.isValid()) entity.remove();
            }
        }
        squads.clear();
    }

    @EventHandler
    public void onHorn(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.GOAT_HORN) return;
        Player player = event.getPlayer();
        if (!player.hasPermission("squadnpc.use")) return;
        event.setCancelled(true);
        toggleSquad(player);
    }

    @EventHandler
    public void onOwnerHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player owner)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;
        List<UUID> ids = squads.get(owner.getUniqueId());
        if (ids == null) return;
        for (UUID id : ids) {
            Entity entity = Bukkit.getEntity(id);
            if (entity instanceof Zombie zombie && zombie.isValid()) zombie.setTarget(attacker);
        }
    }

    private void toggleSquad(Player owner) {
        List<UUID> current = squads.get(owner.getUniqueId());
        if (current != null && !current.isEmpty()) {
            removeSquad(owner.getUniqueId());
            owner.sendMessage("§7Squad hidden.");
            return;
        }
        spawnSquad(owner);
    }

    private void spawnSquad(Player owner) {
        World world = owner.getWorld();
        List<UUID> ids = new ArrayList<>();
        String[] names = {"§8Sword Guard", "§8Mace Guard", "§8Spear Guard", "§8Heavy Guard I", "§8Heavy Guard II"};
        Material[] weapons = {Material.NETHERITE_SWORD, Material.MACE, Material.TRIDENT,
                Material.NETHERITE_AXE, Material.NETHERITE_AXE};
        for (int i = 0; i < names.length; i++) {
            Location spawn = owner.getLocation().clone().add(owner.getLocation().getDirection().multiply(-2.0 - i * 1.15));
            spawn.setY(owner.getLocation().getY());
            Zombie mob = (Zombie) world.spawnEntity(spawn, EntityType.ZOMBIE);
            mob.setCustomName(names[i]);
            mob.setCustomNameVisible(true);
            mob.setAdult();
            mob.setCanPickupItems(false);
            mob.setRemoveWhenFarAway(false);
            mob.setPersistent(true);
            mob.getPersistentDataContainer().set(squadKey, PersistentDataType.STRING, owner.getUniqueId().toString());
            equip(mob, weapons[i], i >= 3);
            ids.add(mob.getUniqueId());
        }
        squads.put(owner.getUniqueId(), ids);
        owner.sendMessage("§aYour squad has arrived. Use a goat horn or /squad to hide it.");
    }

    private void equip(Zombie mob, Material weapon, boolean heavy) {
        var equipment = mob.getEquipment();
        if (equipment == null) return;
        equipment.setHelmet(new ItemStack(Material.NETHERITE_HELMET));
        equipment.setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
        equipment.setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
        equipment.setBoots(new ItemStack(Material.NETHERITE_BOOTS));
        equipment.setItemInMainHand(new ItemStack(weapon));
        if (heavy) equipment.setItemInOffHand(new ItemStack(Material.SHIELD));
        equipment.setHelmetDropChance(0f);
        equipment.setChestplateDropChance(0f);
        equipment.setLeggingsDropChance(0f);
        equipment.setBootsDropChance(0f);
        equipment.setItemInMainHandDropChance(0f);
        equipment.setItemInOffHandDropChance(0f);
    }

    private void tickSquads() {
        for (var entry : squads.entrySet()) {
            Player owner = Bukkit.getPlayer(entry.getKey());
            if (owner == null || !owner.isOnline()) continue;
            List<UUID> ids = entry.getValue();
            for (int i = 0; i < ids.size(); i++) {
                Entity entity = Bukkit.getEntity(ids.get(i));
                if (!(entity instanceof Zombie mob) || !mob.isValid() || mob.isDead()) continue;
                if (!mob.getWorld().equals(owner.getWorld())) continue;
                if (mob.getTarget() != null && mob.getTarget().isValid()) continue;
                Location target = owner.getLocation().clone();
                Vector behind = owner.getLocation().getDirection().clone().normalize().multiply(-1);
                target.add(behind.multiply(1.8 + i * 1.1));
                target.setY(mob.getLocation().getY());
                Vector delta = target.toVector().subtract(mob.getLocation().toVector());
                double distance = delta.length();
                if (distance > 1.5) {
                    Vector velocity = delta.normalize().multiply(Math.min(0.28, 0.08 + distance * 0.025));
                    velocity.setY(mob.isOnGround() ? mob.getVelocity().getY() : mob.getVelocity().getY());
                    mob.setVelocity(velocity);
                } else {
                    Vector v = mob.getVelocity();
                    mob.setVelocity(new Vector(0, v.getY(), 0));
                }
            }
        }
    }

    private void removeSquad(UUID ownerId) {
        List<UUID> ids = squads.remove(ownerId);
        if (ids == null) return;
        for (UUID id : ids) {
            Entity entity = Bukkit.getEntity(id);
            if (entity != null && entity.isValid()) entity.remove();
        }
    }
}
