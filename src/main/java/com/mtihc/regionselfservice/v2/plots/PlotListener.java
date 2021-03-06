package com.mtihc.regionselfservice.v2.plots;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.BlockVector;

import com.mtihc.regionselfservice.v2.plots.exceptions.SignException;
import com.mtihc.regionselfservice.v2.plots.signs.ForRentSign;
import com.mtihc.regionselfservice.v2.plots.signs.ForSaleSign;
import com.mtihc.regionselfservice.v2.plots.signs.PlotSignText;
import com.mtihc.regionselfservice.v2.plots.signs.PlotSignText.ForRentSignText;
import com.mtihc.regionselfservice.v2.plots.signs.PlotSignText.ForSaleSignText;
import com.mtihc.regionselfservice.v2.plots.signs.PlotSignType;
import com.mtihc.regionselfservice.v2.plots.util.TimeStringConverter;
import com.mtihc.regionselfservice.v2.plugin.SelfServiceMessage;
import com.mtihc.regionselfservice.v2.plugin.SelfServiceMessage.MessageKey;
import com.mtihc.regionselfservice.v2.util.PlayerUUIDConverter;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;


class PlotListener implements Listener {
    
    private PlotManager mgr;
    
    PlotListener(PlotManager mgr) {
	this.mgr = mgr;
    }
    
    @EventHandler
    public void onSignChange(SignChangeEvent event) {
	if (event.isCancelled()) {
	    return;
	}
	
	Sign sign = (Sign) event.getBlock().getState();
	PlotSignType type = PlotSignType.getPlotSignType(event.getLines());
	if (type == null) {
	    // not a plot-sign
	    return;
	}
	
	Player player = event.getPlayer();
	
	// plot world
	PlotWorld plotWorld = this.mgr.getPlotWorld(event.getBlock().getWorld());
	
	Plot plot;
	PlotSignText<?> signText;
	IPlotSign plotSign;
	
	try {
	    // try to read the sign
	    signText = PlotSignText.createText(plotWorld, event.getLines());
	    
	    // get plot data
	    plot = plotWorld.getPlot(signText.getRegionId());
	    
	    // create sign data... later
	    // add sign to plot... later
	    
	} catch (SignException e) {
	    // invalid sign. Cancel the event
	    denieSignChange(player, SelfServiceMessage.getFormatedMessage(MessageKey.error_sign_creation, type.name(), e.getMessage()), sign.getBlock(), true, event, true);
	    return;
	}
	
	ProtectedRegion region = plot.getRegion();
	if (region == null) {
	    denieSignChange(player, SelfServiceMessage.getFormatedMessage(MessageKey.error_sign_creation_region, type.name(), plot.getRegionId()), sign.getBlock(), true, event, true);
	    return;
	}
	
	boolean isOwner = region.isOwner(this.mgr.getWorldGuard().wrapPlayer(player));
	boolean isInside = region.contains(sign.getX(), sign.getY(), sign.getZ());
	
	IPlotWorldConfig config = plot.getPlotWorld().getConfig();
	
	if (type == PlotSignType.FOR_RENT) {
	    
	    // check permission to rent out
	    if (!player.hasPermission(Permission.RENTOUT)) {
		denieSignChange(player, SelfServiceMessage.getMessage(MessageKey.error_rent_no_perm), sign.getBlock(), true, event, true);
		return;
	    }
	    
	    // check permission to rent out, unowned regions
	    if (!isOwner && !player.hasPermission(Permission.RENTOUT_ANYREGION)) {
		denieSignChange(player, SelfServiceMessage.getMessage(MessageKey.error_rent_not_own), sign.getBlock(), true, event, true);
		return;
	    }
	    
	    // check permission to rent out, outside the region
	    if (!isInside && !player.hasPermission(Permission.RENTOUT_ANYWHERE)) {
		denieSignChange(player, SelfServiceMessage.getMessage(MessageKey.error_sign_outside), sign.getBlock(), true, event, true);
		return;
	    }
	    
	    ForRentSignText rentText = (ForRentSignText) signText;
	    // check if is rented out, then player typed a name on the sign instead of cost
	    if (rentText.isRentedOut()) {
		denieSignChange(player, SelfServiceMessage.getMessage(MessageKey.error_sign_not_valid_rent), sign.getBlock(), true, event, true);
		return;
	    }
	    
	    double rentCostOld = plot.getRentCost();
	    double rentCost = rentText.getRentCost();
	    long rentTimeOld = plot.getRentTime();
	    long rentTime = rentText.getRentTime();
	    String rentTimeString = new TimeStringConverter().convert(rentTime);
	    
	    rentText.applyToSign(event);
	    
	    // check min/max cost
	    double minRentCost = plot.getWorth(config.getOnRentMinBlockCost());
	    double maxRentCost = plot.getWorth(config.getOnRentMaxBlockCost());
	    // interpret the min/max rent cost as "rent cost per hour"
	    // convert them to min/max rent cost as "rent cost per rentTime"
	    double maxRentCostConverted = maxRentCost * (rentTime / (1000.0 * 60.0 * 60.0)); // milliseconds->hours
	    double minRentCostConverted = minRentCost * (rentTime / (1000.0 * 60.0 * 60.0));
	    
	    if (rentCost < minRentCostConverted) {
		denieSignChange(player, SelfServiceMessage.getFormatedMessage(MessageKey.sign_rent_price_low, this.mgr.getEconomy().format(minRentCostConverted), this.mgr.getEconomy().format(maxRentCostConverted), rentTimeString, this.mgr.getEconomy().format(minRentCost), this.mgr.getEconomy().format(maxRentCost)), sign.getBlock(), true, event, true);
		return;
	    } else if (rentCost > maxRentCostConverted) {
		denieSignChange(player, SelfServiceMessage.getFormatedMessage(MessageKey.sign_rent_price_high, this.mgr.getEconomy().format(minRentCostConverted), this.mgr.getEconomy().format(maxRentCostConverted), rentTimeString, this.mgr.getEconomy().format(minRentCost), this.mgr.getEconomy().format(maxRentCost)), sign.getBlock(), true, event, true);
		return;
	    }
	    
	    // check permission to rent out, for free
	    if (rentCost == 0) {
		if (!player.hasPermission(Permission.RENTOUT_FREE)) {
		    denieSignChange(player, SelfServiceMessage.getMessage(MessageKey.error_rent_no_perm), sign.getBlock(), true, event, true);
		    return;
		}
	    }
	    
	    if (rentCostOld != rentCost || rentTimeOld != rentTime) {
		plot.setRentCost(rentCost, rentTime);
	    }
	    plotSign = new ForRentSign(plot, sign.getLocation().toVector().toBlockVector());
	    // no need to set extra data at this point
	    
	    this.mgr.messages.upForRent(player, region.getOwners().getUniqueIds(), region.getMembers().getUniqueIds(), region.getId(), rentCost, rentTimeString);
	} else if (type == PlotSignType.FOR_SALE) {
	    
	    // check permission to sell
	    if (!player.hasPermission(Permission.SELL)) {
		denieSignChange(player, SelfServiceMessage.getMessage(MessageKey.error_sell_no_perm), sign.getBlock(), true, event, true);
		return;
	    }
	    
	    // check permission to sell, unowned regions
	    if (!isOwner && !player.hasPermission(Permission.SELL_ANYREGION)) {
		denieSignChange(player, SelfServiceMessage.getMessage(MessageKey.error_sell_not_own), sign.getBlock(), true, event, true);
		return;
	    }
	    
	    // check permission to sell, outside the region
	    if (!isInside && !player.hasPermission(Permission.SELL_ANYWHERE)) {
		denieSignChange(player, SelfServiceMessage.getMessage(MessageKey.error_sign_outside), sign.getBlock(), true, event, true);
		return;
	    }
	    
	    // You can't sell a player's last region,
	    // because players would be able to work together, to mess up your server
	    if (plotWorld.getConfig().isReserveFreeRegionsEnabled()) {
		Set<UUID> ownerUUIDs = region.getOwners().getUniqueIds();
		Set<UUID> homelessUUIDs = plotWorld.getPotentialHomeless(ownerUUIDs);
		if (!homelessUUIDs.isEmpty()) {
		    String homelessString = "";
		    for (UUID homelessUUID : homelessUUIDs) {
			homelessString += ", " + PlayerUUIDConverter.toPlayerName(homelessUUID);
		    }
		    homelessString = homelessString.substring(2);
		    denieSignChange(player, SelfServiceMessage.getMessage(MessageKey.error_region_can_not_sell) + " " + SelfServiceMessage.getFormatedMessage(MessageKey.error_people_get_homeless, homelessString), sign.getBlock(), true, event, true);
		    return;
		}
	    }
	    
	    ForSaleSignText saleText = (ForSaleSignText) signText;
	    double sellCostOld = plot.getSellCost();
	    double sellCost = saleText.getSellCost();
	    signText.applyToSign(event.getLines());
	    
	    // check min/max cost
	    double minSellCost = plot.getWorth(config.getOnSellMinBlockCost());
	    double maxSellCost = plot.getWorth(config.getOnSellMaxBlockCost());
	    
	    if (sellCost < minSellCost) {
		denieSignChange(player, SelfServiceMessage.getFormatedMessage(MessageKey.sign_sell_price_low, this.mgr.getEconomy().format(minSellCost), this.mgr.getEconomy().format(maxSellCost)), sign.getBlock(), true, event, true);
		return;
	    } else if (sellCost > maxSellCost) {
		denieSignChange(player, SelfServiceMessage.getFormatedMessage(MessageKey.sign_sell_price_high, this.mgr.getEconomy().format(minSellCost), this.mgr.getEconomy().format(maxSellCost)), sign.getBlock(), true, event, true);
		return;
	    }
	    
	    // check permission to sell, for free
	    if (sellCost == 0) {
		if (!player.hasPermission(Permission.SELL_FREE)) {
		    denieSignChange(player, SelfServiceMessage.getMessage(MessageKey.error_sell_no_perm), sign.getBlock(), true, event, true);
		    return;
		}
	    }
	    
	    if (sellCostOld != sellCost) {
		plot.setSellCost(sellCost);
	    }
	    plotSign = new ForSaleSign(plot, sign.getLocation().toVector().toBlockVector());
	    // no need to set extra data at this point
	    
	    this.mgr.messages.upForSale(player, region.getOwners().getUniqueIds(), region.getMembers().getUniqueIds(), region.getId(), sellCost);
	} else {
	    SelfServiceMessage.sendMessage(player, MessageKey.error_sign_not_valid_type);
	    return;
	}
	
	// save new sign data
	plot.setSign(plotSign);
	plot.save();
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
	if (event.isCancelled()) {
	    return;// event was cancelled
	}
	
	if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK) {
	    if (event.getClickedBlock().getState() instanceof Sign) {
		Sign sign = (Sign) event.getClickedBlock().getState();
		PlotSignType type = PlotSignType.getPlotSignType(sign.getLines());
		
		if (type != null) {
		    PlotWorld plotWorld = this.mgr.getPlotWorld(sign.getWorld());
		    
		    try {
			Plot plot = plotWorld.getPlot(sign);
			
			if (plot != null) {
			    if (plot.getRegion() != null) {
				// send plot info
				plot.sendInfo(event.getPlayer());
				return;
			    } else {
				// protected region doesn't exist
				denieSignChange(event.getPlayer(), SelfServiceMessage.getFormatedMessage(MessageKey.error_region_not_exists, plot.getRegionId()), event.getClickedBlock(), true, event, false);
				plot.delete();
				return;
			    }
			} else {
			    // didn't find the plot information
			    denieSignChange(event.getPlayer(), SelfServiceMessage.getMessage(MessageKey.error_sign_not_valid) + " " + SelfServiceMessage.getMessage(MessageKey.error_no_plotinfo), event.getClickedBlock(), true, event, false);
			    return;
			}
		    } catch (SignException e) {
			// not a valid sign
			denieSignChange(event.getPlayer(), ChatColor.RED + e.getMessage(), event.getClickedBlock(), true, event, false);
			return;
		    }
		}
	    }
	}
	
    }
    
    private void denieSignChange(Player eventPlayer, String playerMessage, Block block, boolean shouldBreak, Cancellable event, boolean cancelEvent) {
	
	if (shouldBreak) {
	    if (eventPlayer.getGameMode() != GameMode.CREATIVE) {
		block.breakNaturally();
	    } else {
		block.setType(Material.AIR);
	    }
	}
	
	if (cancelEvent) {
	    event.setCancelled(true);
	}
	eventPlayer.sendMessage(playerMessage);
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
	onBlockProtect(event.getBlock(), event);
    }
    
    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
	onBlockProtect(event.getBlock(), event);
    }
    
    private void onBlockProtect(Block block, Cancellable event) {
	onBlockProtect(block, event, null);
    }
    
    private boolean areLocationsEqual(Block block1, Block block2) {
	if (block1 == null || block2 == null) {
	    return false;
	}
	return block1.getLocation().equals(block2.getLocation());
    }
    
    private void onBlockProtect(Block block, Cancellable event, Block originalBlock) {
	if (event.isCancelled()) {
	    return;// event cancelled
	}
	if (!(block.getState() instanceof Sign)) {
	    if (originalBlock == null) {
		// check if there's a sign attached to this block
		onBlockProtect(block.getRelative(BlockFace.UP), event, block);
		onBlockProtect(block.getRelative(BlockFace.EAST), event, block);
		onBlockProtect(block.getRelative(BlockFace.SOUTH), event, block);
		onBlockProtect(block.getRelative(BlockFace.WEST), event, block);
		onBlockProtect(block.getRelative(BlockFace.NORTH), event, block);
	    }
	    return;// not a sign
	}
	
	Sign sign = (Sign) block.getState();
	Block attached = block.getRelative(((org.bukkit.material.Sign) sign.getData()).getAttachedFace());
	if (originalBlock != null && !areLocationsEqual(attached, originalBlock)) {
	    // broke a block next to a sign.
	    // but the sign was not attached to it
	    return;
	}
	// broke a sign. or a block with a sign attached to it.
	
	PlotSignType type = PlotSignType.getPlotSignType(sign.getLines());
	if (type == null) {
	    return;// not a plot-sign
	}
	
	String regionId = PlotSignText.getRegionId(sign.getLines());
	PlotWorld plotWorld = this.mgr.getPlotWorld(sign.getWorld());
	Plot plot = plotWorld.getPlot(regionId);
	
	if (plot == null) {
	    // not a saved sign, let it break
	    return;
	}
	
	ProtectedRegion region = plot.getRegion();
	if (region == null) {
	    // region doesn't exist anymore
	    // let it break
	    return;
	}
	
	BlockVector coords = sign.getLocation().toVector().toBlockVector();
	IPlotSignData plotSign = plot.getSign(coords);
	if (plotSign == null) {
	    // sign data doesn't exist
	    // let it break
	    return;
	}
	type = plotSign.getType();
	
	if (event instanceof BlockBreakEvent) {
	    BlockBreakEvent e = (BlockBreakEvent) event;
	    Player player = e.getPlayer();
	    
	    // check region ownership || permission break-any
	    boolean isOwner = region.isOwner(this.mgr.getWorldGuard().wrapPlayer(player));
	    if (!isOwner && !player.hasPermission(Permission.BREAK_ANY_SIGN)) {
		// not an owner, and no special permission
		SelfServiceMessage.sendMessage(player, MessageKey.error_region_not_own);
		// protect the sign
		event.setCancelled(true);
		return;
	    } else {
		plot.removeSign(coords, player.getGameMode() != GameMode.CREATIVE);
		plot.save();
		Collection<IPlotSignData> signs = plot.getSigns(type);
		if (signs == null || signs.isEmpty()) {
		    SelfServiceMessage.sendFormatedMessage(player, MessageKey.sign_broke, type.name(), plot.getRegionId());
		} else {
		    SelfServiceMessage.sendFormatedMessage(player, MessageKey.sign_broke_last, type.name(), plot.getRegionId(), signs.size(), type.name());
		}
	    }
	} else {
	    // protect the sign
	    event.setCancelled(true);
	}
	
    }
    
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
	Iterator<Block> blocks = event.blockList().iterator();
	while (blocks.hasNext()) {
	    Block block = blocks.next();
	    onBlockProtect(block, event);
	    if (event.isCancelled()) {
		break;
	    }
	}
    }
}
