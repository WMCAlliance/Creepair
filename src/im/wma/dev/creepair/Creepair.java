package im.wma.dev.creepair;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class Creepair extends JavaPlugin implements Listener {
    private final ArrayList<String> worlds = new ArrayList<String>();
    private final List<Material> naturalBlocks = new ArrayList<Material>();
    private int y;
    private RepairHelper helper;


    @Override
    public void onEnable() {
	this.saveDefaultConfig();
        if (!this.getConfig().contains("config")) {
            this.getConfig().options().copyDefaults(true);
        }

        worlds.addAll(this.getConfig().getStringList("worlds"));
        y = this.getConfig().getInt("above_y", 50);
        naturalBlocks.addAll(getMaterialList(this.getConfig().getStringList("natural_blocks")));


        this.helper = new RepairHelper();
        this.getServer().getScheduler().scheduleSyncRepeatingTask(this, this.helper, 10, 10);

        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!worlds.contains(event.getLocation().getWorld().getName()) || event.getEntityType() != EntityType.CREEPER) {
            return;
        }

        if (event.getLocation().getBlockY() >= y) {
            event.setYield(0F);

            for (Block block : event.blockList()) {
                if (naturalBlocks.contains(block.getType())) {
                	helper.add(new CreepairBlock(block, block.getType()));
                } else {
                	for (ItemStack drop : block.getDrops()) {
                		block.getWorld().dropItemNaturally(block.getLocation(), drop);
                	}
                }
            }
        }
    }

    private List<Material> getMaterialList(List<String> names) {
    	List<Material> materials = new ArrayList<Material>(names.size());
    	for (String materialName : names) {
    		Material material = Material.matchMaterial(materialName);
    		if (material == null) {
    			getLogger().warning("Skipping material " + materialName + " didn't match a material.");
    		} else {
    			materials.add(material);
    			getLogger().info("Added material " + materialName + ".");
    		}
    	}

    	return materials;
    }

    public class CreepairBlock implements Comparable<CreepairBlock>{
        public Block block;
        public Material original;
        public byte originalData;

        public CreepairBlock(Block block, Material original) {
         this.block = block;
         this.original = original;
         this.originalData = block.getData();
        }

        public int compareTo(CreepairBlock otherBlock) {
            if (otherBlock.equals(this)) {
        	return 0;
            } else if (block.getY() == otherBlock.block.getY()) {
        	return block.getX() - otherBlock.block.getX();
            } else {
        	return block.getY() - otherBlock.block.getY();
            }
        }

        @Override
	public boolean equals(Object object) {
            if (object instanceof CreepairBlock) {
        	CreepairBlock otherBlock = (CreepairBlock) object;
        	if (otherBlock.block.getLocation().equals(block.getLocation()) &&
        		otherBlock.original == original && otherBlock.originalData == originalData) {
        	    return true;
        	}
            }
            return false;
        }
    }

    public class RepairHelper implements Runnable {
        private final SortedList<CreepairBlock> blocks = new SortedList<CreepairBlock>();

        public void add(CreepairBlock block) {
            blocks.add(block);
        }

        public void run() {
            for (int i = 0; i < 5; i++) {
                if (blocks.size() == 0) {
                    return;
                }

                if (blocks.get(0) != null) {
                    CreepairBlock block = blocks.get(0);

                    // Don't destroy player repairs.
                    if (block.block.getLocation().getBlock().getType() != Material.AIR) {
                    	blocks.remove(0);
                    	continue;
                    }

                    block.block.getLocation().getWorld().playEffect(block.block.getLocation(), Effect.STEP_SOUND, block.original.getId());
                    block.block.setType(block.original);
                    // Make damage/data values (different leaves and such) work.
                    block.block.setData(block.originalData);
                    blocks.remove(0);
                }
            }
        }
    }

}
