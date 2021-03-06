package com.mtihc.regionselfservice.v2.plots;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockVector;

import com.mtihc.regionselfservice.v2.plots.exceptions.EconomyException;
import com.mtihc.regionselfservice.v2.plots.exceptions.PlotBoundsException;
import com.mtihc.regionselfservice.v2.plots.exceptions.PlotControlException;
import com.mtihc.regionselfservice.v2.plots.exceptions.SignException;
import com.mtihc.regionselfservice.v2.plots.signs.ForRentSign;
import com.mtihc.regionselfservice.v2.plots.signs.PlotSignText.ForRentSignText;
import com.mtihc.regionselfservice.v2.plots.signs.PlotSignType;
import com.mtihc.regionselfservice.v2.plots.util.TimeStringConverter;
import com.mtihc.regionselfservice.v2.plugin.SelfServiceMessage;
import com.mtihc.regionselfservice.v2.plugin.SelfServiceMessage.MessageKey;
import com.mtihc.regionselfservice.v2.util.PlayerUUIDConverter;
import com.sk89q.worldedit.bukkit.selections.Selection;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion.CircularInheritanceException;
import com.sk89q.worldguard.protection.regions.RegionType;


/*
 * INFO:
 * Worldguard uses playernames and player uuids parallel! Both sets are redundant and not synced!
 */
public class PlotControl {
    
    private PlotManager mgr;
    
    public PlotControl(PlotManager manager) {
	this.mgr = manager;
    }
    
    public PlotManager getPlotManager() {
	return this.mgr;
    }
    
    public int getRegionCountOfPlayer(World world, UUID playerUUID) {
	// get WorldGuard's region manager
	RegionManager regionManager = this.mgr.getPlotWorld(world).getRegionManager();
	
	// get online player
	OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
	if (player != null && player.isOnline()) {
	    // when player is online, use WorldGuard's method of counting regions
	    return regionManager.getRegionCountOfPlayer(this.mgr.getWorldGuard().wrapOfflinePlayer(player));
	}
	
	// get all regions
	Collection<ProtectedRegion> regions = regionManager.getRegions().values();
	if (regions == null || regions.isEmpty()) {
	    return 0;
	}
	
	// count owned regions
	int count = 0;
	for (ProtectedRegion region : regions) {
	    Set<UUID> ownerUUIDs = region.getOwners().getUniqueIds();
	    
	    if (ownerUUIDs.contains(playerUUID)) {
		count++;
	    }
	}
	
	return count;
    }
    
    private static final Set<Material> invisibleBlockMaterials = new HashSet<Material>();
    
    private static Set<Material> getInvisibleMaterials() {
	if (invisibleBlockMaterials.isEmpty()) {
	    invisibleBlockMaterials.add(Material.AIR);
	    invisibleBlockMaterials.add(Material.WATER);
	    invisibleBlockMaterials.add(Material.STATIONARY_WATER);
	    invisibleBlockMaterials.add(Material.LAVA);
	    invisibleBlockMaterials.add(Material.STATIONARY_LAVA);
	    invisibleBlockMaterials.add(Material.SNOW);
	    invisibleBlockMaterials.add(Material.LONG_GRASS);
	}
	return invisibleBlockMaterials;
    }
    
    public static Sign getTargetSign(Player player) {
	// get targeted block
	Block block = player.getTargetBlock(getInvisibleMaterials(), 8);
	
	// check if block is a wooden sign, return null otherwise
	if (block.getState() instanceof Sign) {
	    return (Sign) block.getState();
	} else {
	    return null;
	}
	
    }
    
    private void checkRegionCount(Player player, PlotWorld world) throws PlotControlException {
	
	//
	// Check if player has too many regions or special permission
	//
	int regionCount = getRegionCountOfPlayer(world.getWorld(), player.getUniqueId());
	int regionMax = world.getConfig().getMaxRegionCount();
	boolean bypassMax = player.hasPermission(Permission.BYPASSMAX_REGIONS);
	
	if (!bypassMax && regionCount >= regionMax) {
	    throw new PlotControlException(SelfServiceMessage.getFormatedMessage(MessageKey.error_to_much_regions, regionCount, regionMax));
	}
	
    }
    
    public void buy(final Player player) throws PlotControlException {
	// get targeted sign
	Sign sign = getTargetSign(player);
	if (sign == null) {
	    throw new PlotControlException(SelfServiceMessage.getMessage(MessageKey.error_no_sign));
	}
	
	BlockVector coords = sign.getLocation().toVector().toBlockVector();
	final PlotWorld plotWorld = this.mgr.getPlotWorld(player.getWorld());
	final Plot plot;
	IPlotSign plotSign = null;
	
	try {
	    // try to get the plot-object via wooden sign,
	    // the sign should probably have the region name on the last 2 lines
	    plot = plotWorld.getPlot(sign);
	} catch (SignException e) {
	    throw new PlotControlException(SelfServiceMessage.getFormatedMessage(MessageKey.error_no_valid_sign_looking, e.getMessage()), e);
	}
	
	if (plot != null) {
	    // couldn't find plot-object using the targeted sign.
	    // The plot-data was probably deleted.
	    plotSign = (IPlotSign) plot.getSign(coords);
	} else {
	    throw new PlotControlException(SelfServiceMessage.getMessage(MessageKey.error_no_plotinfo));
	}
	
	if (plotSign == null) {
	    throw new PlotControlException(SelfServiceMessage.getMessage(MessageKey.error_no_plotinfo));
	}
	
	if (plotSign.getType() != PlotSignType.FOR_SALE) {
	    // plot-sign is not a for-sale sign
	    throw new PlotControlException(SelfServiceMessage.getMessage(MessageKey.error_looking_wrong_sign));
	}
	
	// get ProtectedRegion
	final ProtectedRegion region = plot.getRegion();
	if (region == null) {
	    throw new PlotControlException(SelfServiceMessage.getFormatedMessage(MessageKey.error_region_not_exists, plot.getRegionId()));
	}
	
	// not for sale?
	if (!plot.isForSale()) {
	    throw new PlotControlException(SelfServiceMessage.getFormatedMessage(MessageKey.error_region_not_for_sale, plot.getRegionId()));
	}
	
	// already owner?
	if (region.isOwner(this.mgr.getWorldGuard().wrapPlayer(player))) {
	    throw new PlotControlException(SelfServiceMessage.getMessage(MessageKey.error_already_own_region));
	}
	
	checkRegionCount(player, plotWorld);
	int regionCount = getRegionCountOfPlayer(plotWorld.getWorld(), player.getUniqueId());
	
	// get region cost
	final double cost = plot.getSellCost();
	
	//
	// check if it's a free region,
	// and if it's reserved,
	// and if player already has a region
	//
	boolean reserve = plotWorld.getConfig().isReserveFreeRegionsEnabled();
	if (reserve && (cost <= 0) && (regionCount > 0)) {
	    throw new PlotControlException(SelfServiceMessage.getMessage(MessageKey.error_region_reserved));
	}
	
	//
	// Check if players would become homless after sale.
	// This is part of preventing cheating with free regions.
	//
	
	final Set<UUID> ownerUUIDs = region.getOwners().getUniqueIds();
	
	if (reserve) {
	    Set<UUID> homeless = plotWorld.getPotentialHomeless(ownerUUIDs);
	    
	    if (!homeless.isEmpty()) {
		String homelessString = "";
		for (UUID playerUUID : homeless) {
		    homelessString += ", " + PlayerUUIDConverter.toPlayerName(playerUUID); // convert from uuid to player names
		}
		homelessString = homelessString.substring(2);// remove comma and space
		throw new PlotControlException(SelfServiceMessage.getMessage(MessageKey.error_can_not_buy) + " " + SelfServiceMessage.getFormatedMessage(MessageKey.error_people_get_homeless, homelessString));
	    }
	}
	
	// check bypasscost || pay for region
	
	final boolean bypassCost = player.hasPermission(Permission.BUY_BYPASSCOST);
	double balance = this.mgr.getEconomy().getBalance(player);
	if (!bypassCost && cost > balance) {
	    throw new PlotControlException(SelfServiceMessage.getFormatedMessage(MessageKey.error_not_enough_money, this.mgr.getEconomy().format(balance), this.mgr.getEconomy().format(cost - balance)));
	}
	
	if (plot.hasRenters()) {
	    throw new PlotControlException(SelfServiceMessage.getMessage(MessageKey.error_can_not_buy) + " " + SelfServiceMessage.getMessage(MessageKey.error_still_renting));
	}
	
	// create YesNoPrompt
	YesNoPrompt prompt = new YesNoPrompt() {
	    
	    @Override
	    protected Prompt onYes() {
		if (!bypassCost) {
		    try {
			PlotControl.this.mgr.getEconomy().withdraw(player, cost);
		    } catch (EconomyException e) {
			player.sendMessage(ChatColor.RED + e.getMessage());
			return Prompt.END_OF_CONVERSATION;
		    }
		}
		
		double share = cost;
		
		// --------------------
		// TAX BEGIN
		// --------------------
		
		UUID taxAccount = plotWorld.getConfig().getTaxAccountHolder();
		double percentageTax = plotWorld.getConfig().getTaxPercent();
		double percentage = 0;
		
		if (cost >= plotWorld.getConfig().getTaxFromPrice()) {
		    percentage = percentageTax * cost / 100;
		    share -= percentage;
		    PlotControl.this.mgr.getEconomy().deposit(taxAccount, percentage);
		}
		
		// --------------------
		// TAX END
		// --------------------
		
		// calc share and pay owners their share
		share = share / Math.max(1, ownerUUIDs.size());
		for (UUID ownerUUID : ownerUUIDs) {
		    PlotControl.this.mgr.getEconomy().deposit(ownerUUID, share);
		}
		
		// remove owners, add buyer as owner
		DefaultDomain newOwnerDomain = new DefaultDomain();
		newOwnerDomain.addPlayer(player.getUniqueId());
		region.setOwners(newOwnerDomain);
		// save region owner changes
		try {
		    plotWorld.getRegionManager().save();
		} catch (StorageException e) {
		    PlotControl.this.mgr.getPlugin().getLogger().log(Level.WARNING, ChatColor.RED + "Failed to save region changes to world \"" + plotWorld.getName() + "\", using WorldGuard.", e);
		}
		
		// break all for sale signs
		Collection<IPlotSignData> forSaleSigns = plot.getSigns(PlotSignType.FOR_SALE);
		for (IPlotSignData data : forSaleSigns) {
		    BlockVector vec = data.getBlockVector();
		    plot.removeSign(vec, player.getGameMode() != GameMode.CREATIVE);
		}
		
		// delete plot-info if possible, otherwise just save changes
		// (a plot can't be deleted when there's still active renters)
		if (!plot.delete()) {
		    plot.save();
		}
		PlotControl.this.mgr.messages.bought(plot.getRegionId(), player, cost, ownerUUIDs, region.getMembers().getUniqueIds(), share, taxAccount, percentage);
		return Prompt.END_OF_CONVERSATION;
	    }
	    
	    @Override
	    protected Prompt onNo() {
		SelfServiceMessage.sendFormatedMessage(player, MessageKey.player_not_bought, plot.getRegionId());
		return Prompt.END_OF_CONVERSATION;
	    }
	};
	
	// ask the question
	if (bypassCost) {
	    SelfServiceMessage.sendMessage(player, MessageKey.player_has_perm_skip_pay);
	}
	SelfServiceMessage.sendFormatedMessage(player, MessageKey.player_will_buy, region.getId(), this.mgr.getEconomy().format(cost));
	// prompt for yes or no
	new ConversationFactory(this.mgr.getPlugin()).withFirstPrompt(prompt).withLocalEcho(false).withModality(false).buildConversation(player).begin();
	
    }
    
    public void rent(final Player player) throws PlotControlException {
	// get targeted sign
	final Sign sign = getTargetSign(player);
	if (sign == null) {
	    throw new PlotControlException(SelfServiceMessage.getMessage(MessageKey.error_no_sign));
	}
	
	BlockVector coords = sign.getLocation().toVector().toBlockVector();
	final PlotWorld plotWorld = this.mgr.getPlotWorld(player.getWorld());
	final Plot plot;
	IPlotSign plotSign = null;
	
	try {
	    // try to get the plot-object via wooden sign,
	    // the sign should probably have the region name on the last 2 lines
	    plot = plotWorld.getPlot(sign);
	} catch (SignException e) {
	    throw new PlotControlException(SelfServiceMessage.getFormatedMessage(MessageKey.error_no_valid_sign_looking, e.getMessage()), e);
	}
	
	if (plot != null) {
	    // couldn't find plot-object using the targeted sign.
	    // The plot-data was probably deleted.
	    plotSign = (IPlotSign) plot.getSign(coords);
	} else {
	    throw new PlotControlException(SelfServiceMessage.getMessage(MessageKey.error_no_plotinfo));
	}
	
	if (plotSign == null) {
	    throw new PlotControlException(SelfServiceMessage.getMessage(MessageKey.error_no_plotinfo));
	}
	
	if (plotSign.getType() != PlotSignType.FOR_RENT) {
	    // plot-sign is not a for-rent sign
	    throw new PlotControlException(SelfServiceMessage.getMessage(MessageKey.error_looking_wrong_sign));
	}
	
	// get ProtectedRegion
	final ProtectedRegion region = plot.getRegion();
	if (region == null) {
	    throw new PlotControlException(SelfServiceMessage.getFormatedMessage(MessageKey.error_region_not_exists, plot.getRegionId()));
	}
	
	// not for rent
	// this check is probably not even needed
	if (!plot.isForRent()) {
	    throw new PlotControlException(SelfServiceMessage.getFormatedMessage(MessageKey.error_region_not_for_rent, plot.getRegionId()));
	}
	
	// check if(rentSign.getRentPlayer() == player.getName())
	// then it's OK if he is already member. We will just add extra time to his rent-time.
	// TODO add a configuration like... when the remaining time is below
	// 10%, THEN you are allowed to extend the time.
	
	ForRentSign rentSign = (ForRentSign) plotSign;
	
	final long remainingTime = rentSign.getRentPlayerTime();
	
	// already member?
	if (region.isMember(this.mgr.getWorldGuard().wrapPlayer(player))) {
	    if (rentSign.isRentedOut()) {
		if (!player.getUniqueId().equals(rentSign.getRentPlayerUUID())) {
		    // can't extend time via this sign
		    throw new PlotControlException(SelfServiceMessage.getMessage(MessageKey.error_already_member_region) + " " + SelfServiceMessage.getMessage(MessageKey.error_can_not_extend_sign));
		} else {
		    // extending time
		    
		    // when is it allowed?
		    long allowExtendAtRemaining = plot.getRentTimeExtendAllowedAt();
		    
		    // check if it's too soon
		    if (remainingTime > allowExtendAtRemaining) {
			// too soon to extend rent time
			throw new PlotControlException(SelfServiceMessage.getFormatedMessage(MessageKey.error_can_not_extend_yet, new TimeStringConverter().convert(remainingTime - allowExtendAtRemaining)));
		    }
		}
	    } else {
		throw new PlotControlException(SelfServiceMessage.getMessage(MessageKey.error_already_member_region));
	    }
	}
	
	// get owners for later
	final Set<UUID> ownerUUIDs = region.getOwners().getUniqueIds();
	// get members for later
	final Set<UUID> memberUUIDs = region.getMembers().getUniqueIds();
	
	// get rent cost and time
	final double cost = plot.getRentCost();
	final String timeString = new TimeStringConverter().convert(plot.getRentTime());
	
	// check bypasscost || pay for region
	
	final boolean bypassCost = player.hasPermission(Permission.RENT_BYPASSCOST);
	double balance = this.mgr.getEconomy().getBalance(player);
	if (!bypassCost && cost > balance) {
	    throw new PlotControlException(SelfServiceMessage.getFormatedMessage(MessageKey.error_not_enough_money, this.mgr.getEconomy().format(balance), this.mgr.getEconomy().format(cost - balance)));
	}
	
	// create YesNoPrompt
	YesNoPrompt prompt = new YesNoPrompt() {
	    
	    @Override
	    protected Prompt onYes() {
		if (!bypassCost) {
		    try {
			PlotControl.this.mgr.getEconomy().withdraw(player, cost);
		    } catch (EconomyException e) {
			player.sendMessage(ChatColor.RED + e.getMessage());
			return Prompt.END_OF_CONVERSATION;
		    }
		}
		
		double share = cost;
		
		// no tax for renting out
		
		// calc share and pay owners their share
		share = share / Math.max(1, ownerUUIDs.size());
		for (UUID ownerUUID : ownerUUIDs) {
		    PlotControl.this.mgr.getEconomy().deposit(ownerUUID, share);
		}
		
		// add renter as member
		region.getMembers().addPlayer(player.getUniqueId());
		
		// save region owner changes
		try {
		    plotWorld.getRegionManager().save();
		} catch (StorageException e) {
		    PlotControl.this.mgr.getPlugin().getLogger().log(Level.WARNING, ChatColor.RED + "Failed to save region changes to world \"" + plotWorld.getName() + "\", using WorldGuard.", e);
		}
		
		// put player's name on the sign...
		// put rent time on the sign
		ForRentSign newPlotSign = new ForRentSign(plot, sign.getLocation().toVector().toBlockVector());
		newPlotSign.setRentPlayer(player.getUniqueId());
		newPlotSign.setRentPlayerTime(remainingTime + plot.getRentTime());
		plot.setSign(newPlotSign);
		plot.save();
		
		ForRentSignText rentText = new ForRentSignText(plotWorld, region.getId(), newPlotSign.getRentPlayerUUID(), newPlotSign.getRentPlayerTime());
		rentText.applyToSign(sign);
		
		// the time on the sign will update using a timer
		// that timer starts whenever the server restarts
		
		if (remainingTime > 0) {
		    String newTimeString = new TimeStringConverter().convert(remainingTime + plot.getRentTime());
		    PlotControl.this.mgr.messages.rented(player, ownerUUIDs, memberUUIDs, plot.getRegionId(), cost, newTimeString);
		} else {
		    PlotControl.this.mgr.messages.rented(player, ownerUUIDs, memberUUIDs, plot.getRegionId(), cost, timeString);
		}
		return Prompt.END_OF_CONVERSATION;
	    }
	    
	    @Override
	    protected Prompt onNo() {
		if (remainingTime > 0) {
		    SelfServiceMessage.sendFormatedMessage(player, MessageKey.player_not_extend_renttime, plot.getRegionId());
		} else {
		    SelfServiceMessage.sendFormatedMessage(player, MessageKey.player_not_rented, plot.getRegionId());
		}
		return Prompt.END_OF_CONVERSATION;
	    }
	};
	
	// ask the question
	if (bypassCost) {
	    SelfServiceMessage.sendMessage(player, MessageKey.player_has_perm_skip_pay);
	}
	
	if (remainingTime > 0) {
	    SelfServiceMessage.sendFormatedMessage(player, MessageKey.player_will_rent_extend, this.mgr.getEconomy().format(cost), region.getId(), timeString);
	} else {
	    SelfServiceMessage.sendFormatedMessage(player, MessageKey.player_will_rent, this.mgr.getEconomy().format(cost), region.getId(), timeString);
	}
	
	// prompt for yes or no
	new ConversationFactory(this.mgr.getPlugin()).withFirstPrompt(prompt).withLocalEcho(false).withModality(false).buildConversation(player).begin();
    }
    
    private Selection getSelection(Player player) throws PlotControlException {
	Selection sel = this.mgr.getWorldEdit().getSelection(player);
	if (sel == null || sel.getMaximumPoint() == null || sel.getMinimumPoint() == null) {
	    throw new PlotControlException(SelfServiceMessage.getMessage(MessageKey.error_selection_first));
	}
	return sel;
    }
    
    private ProtectedRegion defineRegion(PlotWorld plotWorld, Player player, String regionId, Selection sel, int bottomY, int topY) throws PlotControlException {
	return defineRegion(plotWorld, player, regionId, sel, bottomY, topY, null);
    }
    
    private ProtectedRegion defineRegion(PlotWorld plotWorld, Player player, String regionId, Selection sel, int bottomY, int topY, ProtectedRegion existing) throws PlotControlException {
	int by;
	int ty;
	// If value is -1, use exact selection,
	// otherwise use specified value.
	// Specified value will be default value from config, or arguments from command
	if (bottomY <= -1) {
	    by = sel.getMinimumPoint().getBlockY();
	} else {
	    by = bottomY;
	}
	if (topY <= -1) {
	    ty = sel.getMaximumPoint().getBlockY();
	} else {
	    ty = topY;
	}
	// switch values if necessary
	if (ty < by) {
	    int y = ty;
	    ty = by;
	    by = y;
	}
	
	if (!player.hasPermission(Permission.CREATE_ANYSIZE)) {
	    
	    int width = sel.getWidth();
	    int length = sel.getLength();
	    int height = sel.getHeight();
	    
	    int minY = plotWorld.getConfig().getMinimumY();
	    int maxY = plotWorld.getConfig().getMaximumY();
	    int minHeight = plotWorld.getConfig().getMinimumHeight();
	    int maxHeight = plotWorld.getConfig().getMaximumHeight();
	    int minWidthLength = plotWorld.getConfig().getMinimumWidthLength();
	    int maxWidthLength = plotWorld.getConfig().getMaximumWidthLength();
	    
	    // check min width/length/height
	    if (width < minWidthLength || length < minWidthLength || height < minHeight) {
		throw new PlotBoundsException(PlotBoundsException.Type.SELECTION_TOO_SMALL, width, length, height, minWidthLength, maxWidthLength, minHeight, maxHeight);
	    }
	    // check max width/length/height
	    else if (width > maxWidthLength || length > maxWidthLength || height > maxHeight) {
		throw new PlotBoundsException(PlotBoundsException.Type.SELECTION_TOO_BIG, width, length, height, maxWidthLength, maxWidthLength, minHeight, maxHeight);
	    }
	    // check maxY
	    if (ty > maxY) {
		throw new PlotBoundsException(PlotBoundsException.Type.SELECTION_TOO_HIGH, ty, by, minY, maxY);
	    }
	    // check minY
	    if (by < minY) {
		throw new PlotBoundsException(PlotBoundsException.Type.SELECTION_TOO_LOW, ty, by, minY, maxY);
	    }
	}
	
	Location min = sel.getMinimumPoint();
	Location max = sel.getMaximumPoint();
	// create protected region
	ProtectedCuboidRegion region = new ProtectedCuboidRegion(regionId, new com.sk89q.worldedit.BlockVector(min.getBlockX(), by, min.getBlockZ()), new com.sk89q.worldedit.BlockVector(max.getBlockX(), ty, max.getBlockZ()));
	
	if (existing != null) {
	    // redefining region, so keep existing values
	    region.setFlags(existing.getFlags());
	    region.setMembers(existing.getMembers());
	    region.setOwners(existing.getOwners());
	    region.setPriority(existing.getPriority());
	    try {
		region.setParent(existing.getParent());
	    } catch (CircularInheritanceException e) {
		// ignore error
	    }
	}
	
	boolean allowOverlap = plotWorld.getConfig().isOverlapUnownedRegionAllowed();
	if (!allowOverlap && overlapsUnownedRegion(region, plotWorld.getWorld(), player)) {
	    // overlapping is not allowed
	    throw new PlotControlException(SelfServiceMessage.getMessage(MessageKey.error_selection_overlaps));
	} else {
	    // not overlapping or it's allowed to overlap
	    
	    boolean doAutomaticParent = plotWorld.getConfig().isAutomaticParentEnabled();
	    boolean allowAnywhere = player.hasPermission(Permission.CREATE_ANYWHERE);
	    
	    ProtectedRegion parentRegion;
	    if (!allowAnywhere || doAutomaticParent) {
		// we need a parent
		parentRegion = getAutomaticParentRegion(region, plotWorld.getWorld(), player);
		
		if (parentRegion == null) {
		    if (!allowAnywhere) {
			// automatic parent was not found, but it's required...
			// because player can only create regions inside owned existing regions.
			throw new PlotControlException(SelfServiceMessage.getMessage(MessageKey.error_region_create_not_allowed));
		    }
		} else if (doAutomaticParent) {
		    // found parent region, and according to the configuration,
		    // we should do automatic parenting
		    try {
			region.setParent(parentRegion);
		    } catch (CircularInheritanceException e) {
		    }
		}
	    }
	}
	return region;
    }
    
    public void define(Player player, String regionId) throws PlotControlException {
	// get player's selection
	Selection sel = getSelection(player);
	// get plot-world information
	PlotWorld plotWorld = this.mgr.getPlotWorld(sel.getWorld());
	
	// define, using default bottom y and top y
	define(player, regionId, plotWorld.getConfig().getDefaultBottomY(), plotWorld.getConfig().getDefaultTopY());
    }
    
    public void define(final Player player, final String regionId, int bottomY, int topY) throws PlotControlException {
	/*
	 * exists already? 
	 * invalid name? 
	 * can't afford? 
	 * call method defineRegion 
	 * set region owner(s) to player or default player pays money save region 
	 * send info
	 */
	
	// get player's selection
	Selection sel = getSelection(player);
	// get plot-world information
	final PlotWorld plotWorld = this.mgr.getPlotWorld(sel.getWorld());
	
	// get world's RegionManager of WorldGuard
	final RegionManager regionManager = plotWorld.getRegionManager();
	
	// check region existance
	if (regionManager.hasRegion(regionId)) {
	    throw new PlotControlException(SelfServiceMessage.getFormatedMessage(MessageKey.error_region_already_exists, regionId));
	}
	// check if valid region name, just like WorldGuard
	if (!isValidRegionName(regionId)) {
	    throw new PlotControlException(SelfServiceMessage.getFormatedMessage(MessageKey.error_region_invalid_name, regionId));
	}
	
	// create region
	final ProtectedRegion region = defineRegion(plotWorld, player, regionId, sel, bottomY, topY);
	// cost must be configured and bypass not permitted
	final boolean enableCost = plotWorld.getConfig().isCreateCostEnabled() && !player.hasPermission(Permission.CREATE_BYPASSCOST);
	// calculate cost
	final double cost = getWorth(region, plotWorld.getConfig().getBlockWorth());
	// check balance
	double balance = this.mgr.getEconomy().getBalance(player);
	if (enableCost && balance < cost) {
	    throw new PlotControlException(SelfServiceMessage.getFormatedMessage(MessageKey.error_not_enough_money, this.mgr.getEconomy().format(balance), this.mgr.getEconomy().format(cost)));
	}
	
	// get default owners from config
	List<UUID> ownerUUIDList = plotWorld.getConfig().getDefaultOwnerUUIDs();
	// who will be the region owner?
	final DefaultDomain ownersDomain = new DefaultDomain();
	// let's create the prompt first
	
	//
	// create the YesNoPrompt
	// we override onYes and onNo
	//
	YesNoPrompt prompt = new YesNoPrompt() {
	    
	    @Override
	    protected Prompt onYes() {
		// set region's owners
		region.setOwners(ownersDomain);
		
		// pay money
		if (enableCost) {
		    try {
			PlotControl.this.mgr.getEconomy().withdraw(player, cost);
		    } catch (EconomyException e) {
			SelfServiceMessage.sendFormatedMessage(player, MessageKey.error_region_pay, e.getMessage());
			return Prompt.END_OF_CONVERSATION;
		    }
		}
		
		// save
		try {
		    regionManager.addRegion(region);
		    regionManager.save();
		} catch (StorageException e) {
		    SelfServiceMessage.sendFormatedMessage(player, MessageKey.error_region_saved, regionId, e.getMessage());
		    return Prompt.END_OF_CONVERSATION;
		}
		// send region info to indicate it was successful
		plotWorld.getPlot(regionId).sendInfo(player);
		return Prompt.END_OF_CONVERSATION;
	    }
	    
	    @Override
	    protected Prompt onNo() {
		SelfServiceMessage.sendMessage(player, MessageKey.player_not_create);
		return Prompt.END_OF_CONVERSATION;
	    }
	};
	
	//
	// add owners, and
	// run YesNoPrompt
	//
	if (enableCost) {
	    // cost is enabled, player will be owner
	    checkRegionCount(player, plotWorld);
	    ownersDomain.addPlayer(player.getUniqueId());
	    // ask question
	    SelfServiceMessage.sendFormatedMessage(player, MessageKey.player_will_create, this.mgr.getEconomy().format(cost), region.getId());
	    
	    // run YesNoPrompt
	    new ConversationFactory(this.mgr.getPlugin()).withLocalEcho(false).withModality(false).withFirstPrompt(prompt).buildConversation(player).begin();
	    
	} else {
	    // cost is not enabled
	    // who will be owner depends on config
	    if (ownerUUIDList == null || ownerUUIDList.isEmpty()) {
		// no owners in config, owner is player
		checkRegionCount(player, plotWorld);
		ownersDomain.addPlayer(player.getUniqueId());
	    } else {
		// owners are in config
		// owners from cronfig will be owners
		
		for (UUID ownerUUID : ownerUUIDList) {
		    ownersDomain.addPlayer(ownerUUID);
		}
	    }
	    // save
	    prompt.onYes();
	}
    }
    
    public void redefine(Player player, String regionId) throws PlotControlException {
	PlotWorld plotWorld = this.mgr.getPlotWorld(player.getWorld());
	redefine(player, regionId, plotWorld.getConfig().getDefaultBottomY(), plotWorld.getConfig().getDefaultTopY());
    }
    
    public void redefine(final Player player, final String regionId, int bottomY, int topY) throws PlotControlException {
	/*
	 * doesn't exist?
	 * different owner?
	 * store old size, etc
	 * call method
	 * defineRegion 
	 * calculate cost/refund costs player if larger, refunds owners
	 * if smaller
	 */
	
	// get player's selection
	Selection sel = getSelection(player);
	// get plot-world information
	PlotWorld plotWorld = this.mgr.getPlotWorld(sel.getWorld());
	
	final RegionManager regionManager = plotWorld.getRegionManager();
	ProtectedRegion region = regionManager.getRegion(regionId);
	
	if (region == null) {
	    throw new PlotControlException(SelfServiceMessage.getFormatedMessage(MessageKey.error_region_not_exists, regionId));
	} else if (!region.isOwner(this.mgr.getWorldGuard().wrapPlayer(player)) && !player.hasPermission(Permission.REDEFINE_ANYREGION)) {
	    // must be owner
	    throw new PlotControlException(SelfServiceMessage.getMessage(MessageKey.error_region_redefine));
	}
	
	// get old values
	double blockWorth = plotWorld.getConfig().getBlockWorth();
	final double oldWorth = getWorth(region, blockWorth);
	final int oldWidth = Math.abs(region.getMaximumPoint().getBlockX() - region.getMinimumPoint().getBlockX()) + 1;
	final int oldLength = Math.abs(region.getMaximumPoint().getBlockZ() - region.getMinimumPoint().getBlockZ()) + 1;
	final int oldHeight = Math.abs(region.getMaximumPoint().getBlockY() - region.getMinimumPoint().getBlockY()) + 1;
	
	// redefine region
	final ProtectedRegion regionNew = defineRegion(plotWorld, player, regionId, sel, bottomY, topY, region);
	
	// get new values
	final double newWorth = getWorth(regionNew, blockWorth);
	final int newWidth = Math.abs(regionNew.getMaximumPoint().getBlockX() - regionNew.getMinimumPoint().getBlockX()) + 1;
	final int newLength = Math.abs(regionNew.getMaximumPoint().getBlockZ() - regionNew.getMinimumPoint().getBlockZ()) + 1;
	final int newHeight = Math.abs(regionNew.getMaximumPoint().getBlockY() - regionNew.getMinimumPoint().getBlockY()) + 1;
	
	// calculate cost. refund if < 0
	final double cost = newWorth - oldWorth;
	// get owners
	final Set<UUID> ownerUUIDs = region.getOwners().getUniqueIds();
	
	// cost must be configured and bypass must not be permitted
	final boolean enableCost = plotWorld.getConfig().isCreateCostEnabled() && cost != 0 && !player.hasPermission(Permission.CREATE_BYPASSCOST);
	
	//
	// Ask the question
	//
	if (cost > 0) {
	    // larger region
	    
	    if (enableCost) {
		// check balance
		double balance = this.mgr.getEconomy().getBalance(player);
		if (balance < cost) {
		    throw new PlotControlException(SelfServiceMessage.getFormatedMessage(MessageKey.error_not_enough_money, this.mgr.getEconomy().format(balance), this.mgr.getEconomy().format(cost)));
		}
		// send cost info
		SelfServiceMessage.sendFormatedMessage(player, MessageKey.player_will_resize_pay, this.mgr.getEconomy().format(cost), region.getId(), oldWidth, oldLength, oldHeight, newWidth, newLength, newHeight);
	    } else {
		// no cost
		SelfServiceMessage.sendFormatedMessage(player, MessageKey.player_will_resize, region.getId(), oldWidth, oldLength, oldHeight, newWidth, newLength, newHeight);
	    }
	} else {
	    // smaller region
	    SelfServiceMessage.sendFormatedMessage(player, MessageKey.player_will_resize, region.getId(), oldWidth, oldLength, oldHeight, newWidth, newLength, newHeight);
	    
	    if (enableCost) {
		// get comma seperated string of owner names
		// like: bob, john, hank
		String ownerNames = "";
		for (UUID ownerUUID : ownerUUIDs) {
		    ownerNames += ", " + PlayerUUIDConverter.toPlayerName(ownerUUID);
		}
		if (ownerNames.isEmpty()) {
		    ownerNames = "nobody";
		} else {
		    ownerNames = ownerNames.substring(2);
		}
		
		// send info about refund
		SelfServiceMessage.sendFormatedMessage(player, MessageKey.sharing_refund_with, this.mgr.getEconomy().format(Math.abs(cost)), ownerNames);
	    }
	}
	
	//
	// create YesNoPrompt object
	// we override onYes and onNo
	//
	YesNoPrompt prompt = new YesNoPrompt() {
	    
	    @Override
	    protected Prompt onYes() {
		if (enableCost) {
		    try {
			if (cost > 0) {
			    // larger region, cost money
			    PlotControl.this.mgr.getEconomy().withdraw(player, Math.abs(cost));
			} else {
			    // smaller region, refunds money to the owners
			    
			    // calculate share
			    double share = Math.abs(cost) / Math.max(1, ownerUUIDs.size());
			    // refund equal share to owners
			    for (UUID ownerUUID : ownerUUIDs) {
				PlotControl.this.mgr.getEconomy().deposit(ownerUUID, share);
			    }
			    
			}
		    } catch (EconomyException e) {
			// don't save
			player.sendMessage(ChatColor.RED + e.getMessage());
			return Prompt.END_OF_CONVERSATION;
		    }
		    
		}
		try {
		    // save
		    regionManager.addRegion(regionNew);
		    regionManager.save();
		    
		    // send info to the player and owners and members
		    PlotControl.this.mgr.messages.resized(player.getUniqueId(), regionNew.getOwners().getUniqueIds(), regionNew.getMembers().getUniqueIds(), regionId, (enableCost ? oldWorth : 0), (enableCost ? newWorth : 0), oldWidth, oldLength, oldHeight, newWidth, newLength, newHeight);
		    
		} catch (StorageException e) {
		    // i think your server has bigger problems
		    String msg = SelfServiceMessage.getFormatedMessage(MessageKey.error_region_saved, regionNew.getId(), e.getMessage());
		    player.sendMessage(msg);
		    PlotControl.this.mgr.getPlugin().getLogger().log(Level.WARNING, msg, e);
		}
		
		return Prompt.END_OF_CONVERSATION;
	    }
	    
	    @Override
	    protected Prompt onNo() {
		SelfServiceMessage.sendMessage(player, MessageKey.player_not_resize);
		return Prompt.END_OF_CONVERSATION;
	    }
	};
	
	//
	// run YesNoPrompt
	//
	new ConversationFactory(this.mgr.getPlugin()).withLocalEcho(false).withModality(false).withFirstPrompt(prompt).buildConversation(player).begin();
    }
    
    public void delete(final CommandSender player, World world, String regionId) throws PlotControlException {
	final PlotWorld plotWorld = this.mgr.getPlotWorld(world);
	// doesn't exist?
	final Plot plot = plotWorld.getPlot(regionId);
	if (plot == null) {
	    throw new PlotControlException(SelfServiceMessage.getFormatedMessage(MessageKey.error_region_not_exists, regionId));
	}
	
	final ProtectedRegion region = plot.getRegion();
	
	if (plotWorld.getConfig().isReserveFreeRegionsEnabled()) {
	    
	    // Can't allow players to become homeless when
	    // there are free regions reserved for the homeless!
	    // Because they would be able to get a free region, delete it,
	    // get another free region, delete it.. etc
	    
	    if (region != null) {
		Set<UUID> ownerUUIDs = region.getOwners().getUniqueIds();
		Set<UUID> homelessUUIDs = plotWorld.getPotentialHomeless(ownerUUIDs);
		if (!homelessUUIDs.isEmpty()) {
		    String homelessString = "";
		    for (UUID homelessUUID : homelessUUIDs) {
			homelessString += ", " + PlayerUUIDConverter.toPlayerName(homelessUUID);
		    }
		    homelessString = homelessString.substring(2);// remove comma and space
		    throw new PlotControlException(SelfServiceMessage.getMessage(MessageKey.player_can_not_delete) + " " + SelfServiceMessage.getFormatedMessage(MessageKey.error_people_get_homeless, homelessString));
		}
	    }
	}
	
	final boolean costEnabled = plotWorld.getConfig().isCreateCostEnabled() && !player.hasPermission(Permission.CREATE_BYPASSCOST);
	
	// check if there are still renters
	if (plot.hasRenters()) {
	    throw new PlotControlException(SelfServiceMessage.getMessage(MessageKey.player_can_not_delete_renting));
	}
	
	Set<UUID> ownerUUIDs;
	if (region != null) {
	    ownerUUIDs = region.getOwners().getUniqueIds();
	} else {
	    // avoid null pointer errors
	    ownerUUIDs = new HashSet<UUID>();
	}
	
	final double refund;
	final double share;
	if (costEnabled) {
	    // calculate percentage of total worth
	    refund = plotWorld.getConfig().getDeleteRefundPercent() * plot.getWorth() / 100;
	    // calculate how much each owner gets
	    share = refund / Math.max(1, ownerUUIDs.size());
	    
	    // console will not get this message
	    if (player instanceof Player) {
		String nameString = "";
		for (UUID ownerUUID : ownerUUIDs) {
		    nameString += ", " + PlayerUUIDConverter.toPlayerName(ownerUUID);
		}
		if (!nameString.isEmpty()) {
		    nameString = nameString.substring(2);
		} else {
		    nameString = ChatColor.RED + "nobody";
		}
		
		// send refund question message
		SelfServiceMessage.sendFormatedMessage(player, MessageKey.player_will_delete_share, regionId, this.mgr.getEconomy().format(refund), nameString);
	    }
	    
	} else {
	    // setting share to zero, otherwise the final variable will give a warning
	    share = 0;
	    refund = 0;
	    
	    // console will not get this message
	    if (player instanceof Player) {
		// send normal message
		SelfServiceMessage.sendFormatedMessage(player, MessageKey.player_will_delete, regionId);
	    }
	}
	
	//
	// create YesNoPrompt object
	//
	YesNoPrompt prompt = new YesNoPrompt() {
	    
	    @Override
	    protected Prompt onYes() {
		if (!plot.delete()) {
		    SelfServiceMessage.sendFormatedMessage(player, MessageKey.error_region_delete, plot.getRegionId());
		} else {
		    try {
			RegionManager regionManager = plotWorld.getRegionManager();
			Set<UUID> ownerUUIDs;
			Set<UUID> memberUUIDs;
			if (region != null) {
			    regionManager.removeRegion(plot.getRegionId());
			    regionManager.save();
			    
			    ownerUUIDs = region.getOwners().getUniqueIds();
			    memberUUIDs = region.getMembers().getUniqueIds();
			} else {
			    // avoid null pointer errors
			    ownerUUIDs = new HashSet<UUID>();
			    memberUUIDs = new HashSet<UUID>();
			}
			
			// break all for sale signs
			Collection<IPlotSignData> forSaleSigns = plot.getSigns(PlotSignType.FOR_SALE);
			for (IPlotSignData data : forSaleSigns) {
			    BlockVector vec = data.getBlockVector();
			    plot.removeSign(vec, true);
			}
			
			// refund, now we know it's deleted
			if (costEnabled) {
			    for (UUID ownerUUID : ownerUUIDs) {
				PlotControl.this.mgr.getEconomy().deposit(ownerUUID, share);
			    }
			}
			// send messages to everyone involved
			PlotControl.this.mgr.messages.removed(player, ownerUUIDs, memberUUIDs, plot.getRegionId(), refund);
			
		    } catch (StorageException e) {
			SelfServiceMessage.sendFormatedMessage(player, MessageKey.error_region_delete, plot.getRegionId(), e.getMessage());
		    }
		}
		return Prompt.END_OF_CONVERSATION;
	    }
	    
	    @Override
	    protected Prompt onNo() {
		SelfServiceMessage.sendMessage(player, MessageKey.player_not_delete);
		return Prompt.END_OF_CONVERSATION;
	    }
	};
	
	if (!(player instanceof Player)) {
	    prompt.onYes();
	    return;
	}
	
	// run YesNoPrompt
	new ConversationFactory(this.mgr.getPlugin()).withFirstPrompt(prompt).withLocalEcho(false).withModality(false).buildConversation((Player) player).begin();
    }
    
    public void sendRegionCount(CommandSender sender, OfflinePlayer owner, World world) {
	int count = getRegionCountOfPlayer(world, owner.getUniqueId());
	
	String countString = String.valueOf(count);
	if (count < this.mgr.getPlotWorld(world).getConfig().getMaxRegionCount()) {
	    countString = ChatColor.WHITE + countString;
	} else {
	    countString = ChatColor.RED + countString;
	}
	
	SelfServiceMessage.sendFormatedMessage(sender, MessageKey.player_owns, owner.getName(), countString, world.getName());
    }
    
    public void sendWorth(CommandSender sender, String regionId, World world) {
	PlotWorld plotWorld = this.mgr.getPlotWorld(world);
	RegionManager regionManager = plotWorld.getRegionManager();
	ProtectedRegion region = regionManager.getRegion(regionId);
	if (region == null) {
	    SelfServiceMessage.sendFormatedMessage(sender, MessageKey.error_region_not_exists, regionId);
	    return;
	}
	
	int width = Math.abs(region.getMaximumPoint().getBlockX() - region.getMinimumPoint().getBlockX()) + 1;
	int length = Math.abs(region.getMaximumPoint().getBlockZ() - region.getMinimumPoint().getBlockZ()) + 1;
	double cost = getWorth(width, length, plotWorld.getConfig().getBlockWorth());
	SelfServiceMessage.sendFormatedMessage(sender, MessageKey.region_worth, width, length, world.getName(), this.mgr.getEconomy().format(cost));
    }
    
    public void sendWorth(CommandSender sender, int width, int length, World world) {
	PlotWorld plotWorld = this.mgr.getPlotWorld(world);
	double cost = getWorth(width, length, plotWorld.getConfig().getBlockWorth());
	SelfServiceMessage.sendFormatedMessage(sender, MessageKey.region_worth, width, length, world.getName(), this.mgr.getEconomy().format(cost));
    }
    
    public void sendWorth(CommandSender sender, double money, World world) {
	PlotWorld plotWorld = this.mgr.getPlotWorld(world);
	int size = (int) Math.sqrt(getSizeByWorth(money, plotWorld.getConfig().getBlockWorth()));
	SelfServiceMessage.sendFormatedMessage(sender, MessageKey.region_worth, size, size, world.getName(), this.mgr.getEconomy().format(money));
    }
    
    public static int getSizeByWorth(double money, double blockWorth) {
	return (int) Math.sqrt(money / blockWorth);
    }
    
    public static double getWorth(ProtectedRegion region, double blockWorth) {
	if (region == null) {
	    return 0;
	}
	
	int width = region.getMaximumPoint().getBlockX() - region.getMinimumPoint().getBlockX();
	width = Math.abs(width) + 1;
	
	int length = region.getMaximumPoint().getBlockZ() - region.getMinimumPoint().getBlockZ();
	length = Math.abs(length) + 1;
	
	return getWorth(width, length, blockWorth);
    }
    
    public static double getWorth(int width, int length, double blockWorth) {
	return width * length * blockWorth;
    }
    
    public boolean overlapsUnownedRegion(ProtectedRegion region, World world, Player player) {
	return this.mgr.getWorldGuard().getRegionManager(world).overlapsUnownedRegion(region, this.mgr.getWorldGuard().wrapPlayer(player));
    }
    
    public static boolean isValidRegionName(String regionName) {
	if (regionName == null || !ProtectedRegion.isValidId(regionName) || regionName.equalsIgnoreCase("__GLOBAL__") || regionName.matches("\\d")) {
	    return false;
	} else {
	    return true;
	}
	
    }
    
    public ProtectedRegion getAutomaticParentRegion(ProtectedRegion region, World world, Player player) {
	RegionManager regionManager = this.mgr.getWorldGuard().getRegionManager(world);
	LocalPlayer localPlayer = this.mgr.getWorldGuard().wrapPlayer(player);
	
	// get the regions in which the first corner exists
	ApplicableRegionSet regions = regionManager.getApplicableRegions(region.getMinimumPoint());
	
	List<ProtectedRegion> ownedApplicableRegions = new ArrayList<ProtectedRegion>();
	
	// find regions that are cuboid, and owned by the player
	for (ProtectedRegion element : regions) {
	    if (element.getType() != RegionType.CUBOID) {
		continue;
	    }
	    if (!element.isOwner(localPlayer)) {
		continue;
	    }
	    // add owned, cuboid, region
	    ownedApplicableRegions.add(element);
	}
	
	// the first corner is not in an owned, cuboid region
	if (ownedApplicableRegions.size() == 0) {
	    return null;
	}
	
	// like before, get the regions in which the second corner exists
	regions = regionManager.getApplicableRegions(region.getMaximumPoint());
	
	ProtectedRegion automaticParent = null;
	
	// see of the first corner is also in one of these regions
	// and determine which will be the parent
	for (ProtectedRegion element : regions) {
	    if (ownedApplicableRegions.contains(element)) {
		// found a region with both corners in it!
		if (automaticParent == null) {
		    // we didn't find one yet, so this is it for now
		    automaticParent = element;
		} else {
		    // we already found one, so we need to compare
		    if (element.getPriority() >= automaticParent.getPriority()) {
			// priority is higher
			automaticParent = element;
		    } else if (automaticParent.getPriority() == element.getPriority()) {
			// priorities are equal
			if (element.volume() <= automaticParent.volume()) {
			    // has less volume
			    automaticParent = element;
			}
		    }
		    
		}
	    }
	}
	
	return automaticParent;
    }
}
