package im.wma.Creepair;

import java.util.ArrayList;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Creepair extends JavaPlugin implements Listener {
    private final ArrayList<String> worlds = new ArrayList<String>();
    private int y;
    private RepairHelper helper;

    @Override
    public void onEnable() {
        if (!this.getConfig().contains("config"))
            this.getConfig().options().copyDefaults(true);

        worlds.addAll(this.getConfig().getStringList("worlds"));
        y = this.getConfig().getInt("above_y", 50);

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
                if (block.getType() == Material.GRASS || block.getType() == Material.DIRT)
                    helper.add(new CreepairBlock(block, block.getType()));
            }
        }
    }

    public class CreepairBlock {
        public Block block;
        public Material original;

        public CreepairBlock(Block block, Material original) {
            this.block = block;
            this.original = original;
        }
    }

    public class RepairHelper implements Runnable {
        private final ArrayList<CreepairBlock> blocks = new ArrayList<CreepairBlock>();

        public void add(CreepairBlock block) {
            blocks.add(block);
        }

        @Override
        public void run() {
            for (int i = 0; i < 5; i++) {
                if (blocks.size() == 0) {
                    return;
                }

                if (blocks.get(0) != null) {
                    CreepairBlock block = blocks.get(0);
                    block.block.getLocation().getWorld().playEffect(block.block.getLocation(), Effect.STEP_SOUND, block.original.getId());
                    block.block.setType(block.original);
                    blocks.remove(0);
                }
            }
        }
    }

}
