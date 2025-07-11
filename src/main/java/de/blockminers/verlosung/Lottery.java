package de.blockminers.verlosung;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.block.*;
import org.bukkit.util.Vector;

import java.util.*;

public class Lottery extends JavaPlugin {

	private List<Inventory> markedContainers = new ArrayList<>();
	private Location spawnLocation;
	private boolean pluginEnabled = false;
	public static FileConfiguration config;

	@Override
	public void onEnable() {
		saveDefaultConfig();
        config = getConfig();
		// Register commands
		Objects.requireNonNull(getCommand("lottery-markcontainer")).setExecutor(this::markContainer);
		Objects.requireNonNull(getCommand("lottery-spawn-location")).setExecutor(this::setSpawnLocation);
		Objects.requireNonNull(getCommand("lottery-draw")).setExecutor(this::drawLottery);
		Objects.requireNonNull(getCommand("lottery-enable")).setExecutor(this::enablePlugin);
		Objects.requireNonNull(getCommand("lottery-disable")).setExecutor(this::disablePlugin);
	}

	// Command to mark a container (chest, barrel, etc.)
	private boolean markContainer(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender.hasPermission("lottery.main"))) {
			sender.sendMessage("You don't have Permission to perform this Command.");
		}
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
		if (!(sender.hasPermission("lottery.main"))) {
			sender.sendMessage("You don't have Permission to perform this Command.");
			return false;
		}
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
		if (!(sender.hasPermission("lottery.main"))) {
			sender.sendMessage("You don't have Permission to perform this Command.");
			return false;
		}
		pluginEnabled = true;
		sender.sendMessage("Plugin enabled.");
		return true;
	}

	// Command to disable the plugin functionality
	private boolean disablePlugin(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender.hasPermission("lottery.main"))) {
			sender.sendMessage("You don't have Permission to perform this Command.");
			return false;
		}
		pluginEnabled = false;
		markedContainers.clear();
		spawnLocation = null;
		sender.sendMessage("Plugin disabled and all marked containers removed.");
		return true;
	}

	// Command to draw a lottery and spawn an item
	private boolean drawLottery(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender.hasPermission("lottery.main"))) {
			sender.sendMessage("You don't have Permission to perform this Command.");
			return false;
		}
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
		

		// Spawn the item offset variable from the config from the set location
		Location itemLocation = spawnLocation.clone().add(
				getConfig().getDouble("spawnlocation.vectorx"),
				getConfig().getDouble("spawnlocation.vectory"),
				getConfig().getDouble("spawnlocation.vectorz")
		);
		Item droppedItem = Objects.requireNonNull(itemLocation.getWorld()).dropItem(itemLocation, item);
		droppedItem.setVelocity(new Vector(
				getConfig().getDouble("itemfallspeed.velocityx"),
				getConfig().getDouble("itemfallspeed.velocityy"),
				getConfig().getDouble("itemfallspeed.velocityz")
		));


		// Add  particles  at the item's location
		String particleName = getConfig().getString("particle-effect", "HAPPY_VILLAGER");
		Particle particle;
        try {
            particle = Particle.valueOf(particleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            getLogger().warning("Illegal Particle Configuration, Standard HAPPY_VILLAGER will be used!.");
            particle = Particle.HAPPY_VILLAGER; // Fallback
        }

		
		int count = config.getInt("repeats");
		for (int i = 0; i < count; i++)
		itemLocation.getWorld().spawnParticle(particle, droppedItem.getLocation(), config.getInt("particle-count"), config.getDouble("particle-offsetX"), config.getDouble("particle-offsetY"), config.getDouble("particle-offsetZ"),
				config.getDouble("particle-speed"));
		System.out.println(itemLocation);
	}
}
