package com.biggestnerd.caveores;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.github.devotedmc.hiddenore.BlockConfig;
import com.github.devotedmc.hiddenore.Config;
import com.github.devotedmc.hiddenore.LootConfig;
import com.github.devotedmc.hiddenore.TransformConfig;

public class CaveOres extends JavaPlugin implements Listener {

	private List<ChunkCoords> populated = Collections.synchronizedList(new LinkedList<ChunkCoords>());
	
	File file;
	YamlConfiguration save;

	@SuppressWarnings("unchecked")
	public void onEnable() {
		ConfigurationSerialization.registerClass(ChunkCoords.class);
		getServer().getPluginManager().registerEvents(this, this);
		file = new File(getDataFolder(), "populated.yml");
		if(!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		if(!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		save = YamlConfiguration.loadConfiguration(file);
		List<?> load = Collections.synchronizedList(save.getList("populated"));
		if(load.size() > 0 && load.get(0) instanceof ChunkCoords) {
			populated.addAll((List<ChunkCoords>) load);
		}
		new BukkitRunnable() {
			public void run() {
				save.set("populated", populated);
				try {
					save.save(file);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.runTaskTimerAsynchronously(this, 0, 1200);
	}
	
	public void onDisable() {
		try {
			save.save(file);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	static BlockFace[] faces = new BlockFace[] {BlockFace.UP,BlockFace.DOWN,BlockFace.NORTH,BlockFace.SOUTH,BlockFace.EAST,BlockFace.WEST};
	
	@SuppressWarnings("deprecation")
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		Chunk chunk = event.getPlayer().getLocation().getChunk();
		World world = event.getPlayer().getWorld();
		ChunkCoords pair = new ChunkCoords(chunk);
		if(!populated.contains(pair)) {
			System.out.println("Populating cave ores for chunk: " + chunk.toString());
			populated.add(pair);
			for(int x = 0; x < 16; x++) {
				for(int z = 0; z < 16; z++) {
					for(int y = 0; y < world.getMaxHeight(); y++) {
						Block at = chunk.getBlock(x, y, z);
						if(at.getType() == Material.AIR) continue;
						for(BlockFace face : faces) {
							Block next = at.getRelative(face);
							if(next.getType() == Material.AIR) {
								BlockConfig bc = Config.isDropBlock(at.getType().name(), at.getData());
								if(bc != null) {
									LootConfig loot = bc.getLootConfig(Math.random(), at.getBiome().name(), new ItemStack(Material.DIAMOND_PICKAXE), event.getPlayer().getLocation());
									if(loot != null && loot instanceof TransformConfig) {
										List<ItemStack> items = loot.renderLoot(at.getBiome().name(), loot.dropsWithToolConfig(at.getBiome().name(), new ItemStack(Material.DIAMOND_PICKAXE)));
										if(items.size() > 0) {
											at.setType(items.get(0).getType());
											at.setData((byte) items.get(0).getDurability());
										}
									}
								}
								break;
							}
						}
					}
				}
			}
		}
	}
	
	public class ChunkCoords implements ConfigurationSerializable {
		private int x;
		private int z;
		private String world;
		
		public boolean equals(Object other) {
			if(!(other instanceof ChunkCoords)) return false;
			ChunkCoords c = (ChunkCoords) other;
			return c.x == x && c.z == z && c.world.equals(world);
		}
		
		public ChunkCoords(Chunk chunk) {
			x = chunk.getX();
			z = chunk.getZ();
			world = chunk.getWorld().getName();
		}
		
		public ChunkCoords(Map<String, Object> map) {
			x = (int) map.get("x");
			z = (int) map.get("z");
			world = (String) map.get("world");
		}

		@Override
		public Map<String, Object> serialize() {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("x", x);
			map.put("z", z);
			map.put("world", world);
			return map;
		}
	}
}
