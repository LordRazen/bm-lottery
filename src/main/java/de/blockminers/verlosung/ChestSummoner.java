package de.blockminers.verlosung;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.block.*;
import org.bukkit.util.Vector;

import java.util.*;

public class ChestSummoner extends JavaPlugin {

    private List<Inventory> markedContainers = new ArrayList<>();
    private Location spawnLocation;
    private boolean pluginEnabled = false;

    @Override
    public void onEnable() {
        // Register commands
        getCommand("lottery-markcontainer").setExecutor(this::markContainer);
        getCommand("lottery-spawn-location").setExecutor(this::setSpawnLocation);
        getCommand("lottery-draw").setExecutor(this::drawLottery);
        getCommand("lottery-enable").setExecutor(this::enablePlugin);
        getCommand("lottery-disable").setExecutor(this::disablePlugin);
    }

    // Command to mark a container (chest, barrel, etc.)
    private boolean markContainer(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be executed by a player.");
            return false;
        }
        Player player = (Player) sender;
        Block block = player.getTargetBlock(null, 5); // Get the block the player is looking at

        if (block.getState() instanceof InventoryHolder) {
            InventoryHolder container = (InventoryHolder) block.getState();
            markedContainers.add(container.getInventory());
            player.sendMessage("Container marked successfully.");
        } else {
            player.sendMessage("This is not a valid container.");
        }
        return true;
    }

    // Command to set the spawn location for the item to be summoned
    private boolean setSpawnLocation(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be executed by a player.");
            return false;
        }
        Player player = (Player) sender;
        spawnLocation = player.getLocation();
        player.sendMessage("Spawn location set.");
        return true;
    }

    // Command to enable the plugin functionality
    private boolean enablePlugin(CommandSender sender, Command command, String label, String[] args) {
        pluginEnabled = true;
        sender.sendMessage("Plugin enabled.");
        return true;
    }

    // Command to disable the plugin functionality
    private boolean disablePlugin(CommandSender sender, Command command, String label, String[] args) {
        pluginEnabled = false;
        markedContainers.clear();
        spawnLocation = null;
        sender.sendMessage("Plugin disabled and all marked containers removed.");
        return true;
    }

    // Command to draw a lottery and spawn an item
    private boolean drawLottery(CommandSender sender, Command command, String label, String[] args) {
        if (!pluginEnabled) {
            sender.sendMessage("Plugin is not enabled.");
            return false;
        }

        if (markedContainers.isEmpty() || spawnLocation == null) {
            sender.sendMessage("No containers marked or spawn location not set.");
            return false;
        }

        // Choose a random container
        Inventory chosenContainer = markedContainers.get(new Random().nextInt(markedContainers.size()));

        // Attempt to find a non-empty item slot
        List<Integer> nonEmptySlots = new ArrayList<>();
        for (int i = 0; i < chosenContainer.getSize(); i++) {
            ItemStack item = chosenContainer.getItem(i);
            if (item != null && item.getAmount() > 0) {
                nonEmptySlots.add(i); // Add the index of non-empty slot
            }
        }

        // Check if there are any non-empty slots
        if (nonEmptySlots.isEmpty()) {
            sender.sendMessage("The chosen container is empty.");
            return false;
        }

        // Pick a random non-empty slot
        int randomSlotIndex = nonEmptySlots.get(new Random().nextInt(nonEmptySlots.size()));
        ItemStack chosenItem = chosenContainer.getItem(randomSlotIndex);

        // If no item is found in the chosen slot (just in case), try again
        if (chosenItem == null || chosenItem.getAmount() <= 0) {
            sender.sendMessage("The chosen slot does not contain a valid item.");
            return false;
        }

        // Remove the item from the container
        chosenContainer.setItem(randomSlotIndex, null);

        // Spawn the item at the spawn location
        spawnItem(chosenItem);

        return true;
    }

    private void spawnItem(ItemStack item) {
        if (spawnLocation == null) {
            getServer().getConsoleSender().sendMessage("Spawn location not set.");
            return;
        }

        // Spawn the item 3 blocks above the set location
        Location itemLocation = spawnLocation.clone().add(0, 3, 0);
        Item droppedItem = itemLocation.getWorld().dropItem(itemLocation, item);
        droppedItem.setVelocity(new Vector(0, -0.1, 0)); // Make the item fall slowly

        // Add happy villager particles once at the item's location
        itemLocation.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, droppedItem.getLocation(), 10, 0.5, 0.5, 0.5, 0.1);
    }
}
