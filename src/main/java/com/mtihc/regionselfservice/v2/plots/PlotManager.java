package com.mtihc.regionselfservice.v2.plots;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.mtihc.regionselfservice.v2.plots.signs.ForRentSign;
import com.mtihc.regionselfservice.v2.plots.signs.ForRentSignData;
import com.mtihc.regionselfservice.v2.plots.signs.ForSaleSignData;
import com.mtihc.regionselfservice.v2.plots.signs.PlotSignText.ForRentSignText;
import com.mtihc.regionselfservice.v2.plots.signs.PlotSignType;
import com.mtihc.regionselfservice.v2.plots.util.TimeStringConverter;
import com.mtihc.regionselfservice.v2.plugin.SelfServiceMessage;
import com.mtihc.regionselfservice.v2.plugin.SelfServiceMessage.MessageKey;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;


public abstract class PlotManager {
    
    static {
	ConfigurationSerialization.registerClass(PlotData.class);
	ConfigurationSerialization.registerClass(ForRentSignData.class);
	ConfigurationSerialization.registerClass(ForSaleSignData.class);
    }
    
    protected final JavaPlugin plugin;
    protected final WorldGuardPlugin worldGuard;
    private WorldEditPlugin worldEdit;
    protected final IEconomy economy;
    protected final Messages messages;
    protected final IPlotManagerConfig config;
    protected final IPlotWorldConfig defaultConfig;
    protected final Map<UUID, PlotWorld> worlds;
    protected final PlotControl control;
    
    public PlotManager(JavaPlugin plugin, WorldGuardPlugin worldGuard, IEconomy economy, IPlotManagerConfig config, IPlotWorldConfig defaultConfig) {
	this.plugin = plugin;
	this.worldGuard = worldGuard;
	try {
	    this.worldEdit = worldGuard.getWorldEdit();
	} catch (CommandException e) {
	    throw new IllegalArgumentException("Couldn't find WorldEdit.", e);
	}
	this.economy = economy;
	this.messages = new Messages(economy);
	this.config = config;
	this.defaultConfig = defaultConfig;
	this.worlds = new HashMap<UUID, PlotWorld>();
	this.control = new PlotControl(this);
	
	Bukkit.getPluginManager().registerEvents(new PlotListener(this), plugin);
	
	Runnable rentTimer = new Runnable() {
	    
	    public void run() {
		
		// iterate over all PlotWorlds
		for (PlotWorld plotWorld : getPlotWorlds()) {
		    boolean requireSave = false;
		    Collection<PlotData> plots = plotWorld.getPlotData().getValues();
		    
		    // iterate over all plots
		    for (PlotData plot : plots) {
			if (!(plot instanceof Plot)) {
			    // convert to Plot object
			    plot = new Plot(plotWorld, plot);
			}
			
			ProtectedRegion region = ((Plot) plot).getRegion();
			String rentTimeString = new TimeStringConverter().convert(plot.getRentTime());
			
			Collection<IPlotSignData> rentSigns = plot.getSigns(PlotSignType.FOR_RENT);
			
			// iterate over all signs
			for (IPlotSignData plotSign : rentSigns) {
			    if (!(plotSign instanceof IPlotSign)) {
				// convert to IPlotSign
				plotSign = PlotSignType.createPlotSign((Plot) plot, plotSign);
			    }
			    
			    // cast to ForRentSign
			    ForRentSign rentSign = (ForRentSign) plotSign;
			    if (!rentSign.isRentedOut()) {
				// does not need to be updated
				continue;
			    }
			    
			    Sign sign = rentSign.getSign();
			    // subtract a minute
			    long newTime = rentSign.getRentPlayerTime() - 60000;
			    if (newTime <= 0) {
				// time is up
				newTime = 0;
				
				// remove region member
				region.getMembers().removePlayer(rentSign.getRentPlayerUUID());
				requireSave = true;
				
				// get player, if online
				PlotManager.this.messages.rent_ended(rentSign.getRentPlayerUUID(), region.getOwners().getUniqueIds(), region.getMembers().getUniqueIds(), plot.getRegionId(), rentTimeString);
				
				// remove player name from sign
				rentSign.setRentPlayer(null);
			    } else {
				if (newTime <= ((Plot) plot).getRentTimeExtendAllowedAt()) {
				    // TODO move code to messages.rent_extend_warning method
				    // TODO implement permission for this information
				    Player renter = Bukkit.getPlayer(rentSign.getRentPlayerUUID());
				    if (renter != null) {
					SelfServiceMessage.sendFormatedMessage(renter, MessageKey.rent_time_warning, plot.getRegionId(), new TimeStringConverter().convert(newTime));
				    }
				}
			    }
			    // update time on the sign data
			    rentSign.setRentPlayerTime(newTime);
			    
			    // update sign
			    // if rent-player is null, it will automatically write cost:time instead of player:time
			    ForRentSignText rentText = new ForRentSignText(plotWorld, plot.getRegionId(), rentSign.getRentPlayerUUID(), newTime);
			    rentText.applyToSign(sign);
			}
			// save changes
			((Plot) plot).save();
		    }
		    if (requireSave) {
			try {
			    plotWorld.getRegionManager().save();
			} catch (StorageException error) {
			    PlotManager.this.plugin.getLogger().log(Level.SEVERE, "Failed to remove member(s) that ran out of rent-time from region(s) in world \"" + plotWorld.getName() + "\".", error);
			}
		    }
		    
		}
	    }
	};
	
	Bukkit.getScheduler().runTaskTimer(plugin, rentTimer, 0L, 60 * 20L);
    }
    
    public IPlotWorldConfig getDefaultWorldConfig() {
	return this.defaultConfig;
    }
    
    public void reloadWorld(World world) {
	PlotWorld plotWorld = createPlotWorld(world);
	this.worlds.put(plotWorld.getWorldUID(), plotWorld);
    }
    
    public void reloadWorlds() {
	for (World world : Bukkit.getWorlds()) {
	    reloadWorld(world);
	}
	
    }
    
    protected abstract PlotWorld createPlotWorld(World world);
    
    public JavaPlugin getPlugin() {
	return this.plugin;
    }
    
    public WorldGuardPlugin getWorldGuard() {
	return this.worldGuard;
    }
    
    public WorldEditPlugin getWorldEdit() {
	return this.worldEdit;
    }
    
    public IEconomy getEconomy() {
	return this.economy;
    }
    
    public Messages getMessages() {
	return this.messages;
    }
    
    public IPlotManagerConfig getConfig() {
	return this.config;
    }
    
    public PlotWorld getPlotWorld(World world) {
	return this.worlds.get(world.getUID());
    }
    
    public Collection<PlotWorld> getPlotWorlds() {
	return this.worlds.values();
    }
    
    public PlotControl getControl() {
	return this.control;
    }
}
