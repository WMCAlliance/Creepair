package im.wma.dev.creepair;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class Creepair extends JavaPlugin implements Listener {
    private final ArrayList<String> worlds = new ArrayList<>();
    private final List<Material> naturalBlocks = new ArrayList<>();
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
        // Anonymous implementation of "/creepair" root command.
        CommandBase<Creepair> creepairCommand = new CommandBase<Creepair>(this) {
            @Override
            public boolean runCommand(CommandSender sender, Command rootCommand, String label, String[] args) {
                sender.sendMessage("Creepair v" + getPlugin().getDescription().getVersion());
                return true;
            }

            @Override
            public List<String> tabCommand(CommandSender sender, Command rootCommand, String label, String[] args) {
                return null;
            }
        };
        CommandAddBlock commandAddBlock = new CommandAddBlock(this);
        // Register "/creepair add" with the root "/creepair" command.
        creepairCommand.registerSubCommand("add", commandAddBlock);
        creepairCommand.registerSubCommandTab("add", commandAddBlock);

        CommandListBlocks commandListBlocks = new CommandListBlocks(this);
        // Register "/creepair list with the root "/creepair" command.
        creepairCommand.registerSubCommand("list", commandListBlocks);
		
        CommandReload commandReload = new CommandReload(this);
		creepairCommand.registerSubCommand("reload", commandReload);


        // Register "/check" command executor with Bukkit.
        getCommand("creepair").setExecutor(creepairCommand);
    }

	public void reloadPluginConfig(CommandSender sender) {
		reloadConfig();
		
		worlds.clear();
		naturalBlocks.clear();
		
		worlds.addAll(getConfig().getStringList("worlds"));
		y = this.getConfig().getInt("above_y", 50);
		naturalBlocks.addAll(getMaterialList(getConfig().getStringList("natural_blocks")));
		
		sender.sendMessage("Creepair reloaded successfully");
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
        List<Material> materials = new ArrayList<>(names.size());
        for (String materialName : names) {
            Material material = Material.matchMaterial(materialName);
            if (material == null) {
                getLogger().log(Level.WARNING, "Skipping material {0} didn''t match a material.", materialName);
            } else {
                materials.add(material);
                getLogger().log(Level.INFO, "Added material {0}.", materialName);
            }
        }

        return materials;
    }

    public class CommandAddBlock extends CommandBase<Creepair> implements TabExecutor {
        public CommandAddBlock(Creepair plugin) {
            super(plugin);
        }

        @Override
        public boolean runCommand(CommandSender sender, Command rootCommand, String label, String[] args) {
            if (sender.hasPermission("creepair.add")) {
                if (args.length == 1) {
                    Material material = Material.matchMaterial(args[0]);
                    if (material == null) {
                        sender.sendMessage("[Creepair] Please pass in a correct material name");
                    } else if (naturalBlocks.contains(material)) {
                        sender.sendMessage("[Creepair] Material already present");
                    } else {
                        naturalBlocks.add(material);
                        addBlockToConfig(material.name());
                        sender.sendMessage("[Creepair] Added material " + material.name());
                    }
                } else if (args.length == 0) {
                    sender.sendMessage("[Creepair] Please pass in a material name");
                } else {
                    sender.sendMessage("[Creepair] Please only pass in a single material name at a time");
                }
            } else {
                sender.sendMessage("[Creepair] No permission for command: " + rootCommand.getName() + " " + label);
            }
            return true;
        }
        @Override
        public List<String> tabCommand(CommandSender sender, Command rootCommand, String label, String[] args) {
            List<String> tabComplete = new ArrayList<>();
            if(args.length == 1) {
                for (Material materialType : Material.values()){
                    if (materialType.toString().startsWith(args[0])){
                        tabComplete.add((materialType.toString()));
                    }
                }
            }return tabComplete;
        }
    }

    public class CommandListBlocks extends CommandBase<Creepair> {
        public CommandListBlocks(Creepair plugin) {
            super(plugin);
        }

        @Override
        public boolean runCommand(CommandSender sender, Command rootCommand, String label, String[] args) {
            if (sender.hasPermission("creepair.list")) {
                sender.sendMessage("[Creepair] Configuration has " + naturalBlocks.size() + " blocks");
                for (Material block : naturalBlocks) {
                    sender.sendMessage(" - " + block.createBlockData().getMaterial().name());
                }
            } else {
                sender.sendMessage("[Creepair] No permission for command: " + rootCommand.getName() + " " + label);
            }
            return true;
        }

        @Override
        public List<String> tabCommand(CommandSender sender, Command rootCommand, String label, String[] args) {
            return null;
        }
    }
	
	public class CommandReload extends CommandBase<Creepair> {
        public CommandReload(Creepair plugin) {
            super(plugin);
        }

        @Override
        public boolean runCommand(CommandSender sender, Command rootCommand, String label, String[] args) {
            if (sender.hasPermission("creepair.reload")) {
				super.getPlugin().reloadPluginConfig(sender);
            } else {
                sender.sendMessage("[Creepair] No permission for command: " + rootCommand.getName() + " " + label);
            }
            return true;
        }

        @Override
        public List<String> tabCommand(CommandSender sender, Command rootCommand, String label, String[] args) {
            return null;
        }
	}

    public void addBlockToConfig(String name) {
        List<String> natural_blocks = this.getConfig().getStringList("natural_blocks");
        natural_blocks.add(name);
        this.getConfig().set("natural_blocks", natural_blocks);
        saveConfig();
    }

    public class CreepairBlock implements Comparable<CreepairBlock> {
        public Block block;
        public Material original;
        public BlockData originalData;

        public CreepairBlock(Block block, Material original) {
            this.block = block;
            this.original = original;
            this.originalData = block.getBlockData();
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
                return otherBlock.block.getLocation().equals(block.getLocation()) &&
                        otherBlock.original == original && otherBlock.originalData == originalData;
            }
            return false;
        }
    }

    public class RepairHelper implements Runnable {
        private final SortedList<CreepairBlock> blocks = new SortedList<>();

        public void add(CreepairBlock block) {
            blocks.add(block);
        }

        public void run() {
            for (int i = 0; i < 5; i++) {
                if (blocks.isEmpty()) {
                    return;
                }

                if (blocks.get(0) != null) {
                    CreepairBlock block = blocks.get(0);

                    // Don't destroy player repairs.
                    if (block.block.getLocation().getBlock().getType() != Material.AIR) {
                        blocks.remove(0);
                        continue;
                    }

                    block.block.getLocation().getWorld().playEffect(block.block.getLocation(), Effect.STEP_SOUND, 1);
                    block.block.setType(block.original);
                    // Make damage/data values (different leaves and such) work.
                    block.block.setBlockData(block.originalData);
                    blocks.remove(0);
                }
            }
        }
    }
}
