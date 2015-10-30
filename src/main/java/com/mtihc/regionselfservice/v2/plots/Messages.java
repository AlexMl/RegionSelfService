package com.mtihc.regionselfservice.v2.plots;

import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mtihc.regionselfservice.v2.plugin.SelfServiceMessage;
import com.mtihc.regionselfservice.v2.plugin.SelfServiceMessage.MessageKey;
import com.mtihc.regionselfservice.v2.util.PlayerUUIDConverter;


public class Messages {
    
    private IEconomy economy;
    
    // @formatter:off
  /**
   * Messages
   * 
   * SOLD members Region "id" was sold to "buyer". You were member of that
   * region. owners Region "id" was sold to "buyer". You were owner of that
   * region. You are sharing "cost" with "owners". You all received an equal
   * share of "share". buyer You bought region "id" for "cost" from "owners".
   * 
   * RENTED members Region "id" was rented out to "renter" for "time". You are
   * member of this region. owners Region "id" was rented out to "renter" for
   * "time". You are owner of this region. You are sharing "cost" with "owners".
   * You all received an equal share of "share". renter You rented region "id"
   * for "time", for the price of "cost", from "owners".
   * 
   * UPFORSALE members Player "seller" put region "id" up for sale, for "cost".
   * You are member of this region. owners Player "seller" put region "id" up
   * for sale, for "cost". You are owner of this region. If the region is sold,
   * the profits are shared amongst "owners". seller You put region "id" up for
   * sale, for "cost". If the region is sold, the profits are shared amongst
   * "owners".
   * 
   * UPFORRENT members Player "letter" put region "id" up for rent, for "cost"
   * per "time". You are member of this region. owners Player "letter" put
   * region "id" up for rent, for "cost" per "time". You are owner of this
   * region. If the region is rented out, the profits are shared amongst
   * "owners". letter You put region "id" up for rent, for "cost" per "time". If
   * the region is rented out, the profits are shared amongst "owners".
   * 
   * REMOVED members Player "remover" removed region "id". You were member of
   * that region. owners Player "remover" removed region "id". You were owner of
   * that region. You are sharing the refund of "refund" with "owners". You all
   * received an equal share of "share". remover Region "id" removed.
   * 
   * RESIZED (bigger) members Player "resizer" resized region "id" from
   * "old-size" to "new-size". You are member of that region. owners Player
   * "resizer" payed "cost" to resize region "id" from "old-size" to "new-size".
   * You are owner of that region. resizer You payed "cost" to resize region
   * "id" from "old-size" to "new-size".
   * 
   * RESIZED (smaller) members Player "resizer" resized region "id" from
   * "old-size" to "new-size". You are member of that region. owners Player
   * "resizer" resized region "id" from "old-size" to "new-size". You are owner
   * of that region. The region became smaller, so you are sharing the refund of
   * "refund" with "owners". You all received an equal share of "share". resizer
   * You payed "cost" to resize region "id" from "old-size" to "new-size".
   * 
   * CREATE creator Region "id" protected. ...show region info....
   */
  // @formatter:on
    public Messages(IEconomy economy) {
	this.economy = economy;
    }
    
    public void bought(String regionId, CommandSender buyer, double cost, Set<UUID> owners, Set<UUID> members, double share, UUID taxAccountUUID, double tax) {
	
	String ownerNames = toUserfriendlyString(owners);
	String permOwner = Permission.INFORM_OWNER_SOLD;
	String permMember = Permission.INFORM_MEMBER_SOLD;
	
	// You bought region <id> for <cost> from <owners>
	SelfServiceMessage.sendFormatedMessage(buyer, MessageKey.bought_region, regionId, format(cost), ownerNames);
	explainTax(buyer, taxAccountUUID, tax);
	
	if (owners != null) {
	    if (owners.size() > 1) {
		for (UUID ownerUUID : owners) {
		    Player owner = Bukkit.getPlayer(ownerUUID);
		    if (owner == null || !owner.isOnline() || !owner.hasPermission(permOwner) || owner.getName().equalsIgnoreCase(buyer.getName())) {
			continue;
		    }
		    // Region <id> was sold to <buyer>.
		    SelfServiceMessage.sendFormatedMessage(owner, MessageKey.bought_region_player, regionId, buyer.getName());
		    // Sharing <cost> with <owners>
		    SelfServiceMessage.sendFormatedMessage(owner, MessageKey.sharing_cost_with, format(cost), ownerNames);
		    // You all received an equal share of <share>
		    SelfServiceMessage.sendFormatedMessage(owner, MessageKey.received_equal_share_of, formatShare(cost, owners));
		    explainTax(owner, taxAccountUUID, tax);
		}
	    } else {
		Player owner;
		try {
		    owner = Bukkit.getPlayer(owners.iterator().next());
		} catch (NoSuchElementException e) {
		    owner = null;
		}
		if (owner != null && owner.isOnline() && owner.hasPermission(permOwner) && !owner.getName().equalsIgnoreCase(buyer.getName())) {
		    // Region <id> was sold to <buyer>.
		    SelfServiceMessage.sendFormatedMessage(owner, MessageKey.bought_region_player, regionId, buyer.getName());
		    SelfServiceMessage.sendFormatedMessage(owner, MessageKey.received, format(cost));
		    explainTax(owner, taxAccountUUID, tax);
		}
	    }
	    
	}
	if (members != null) {
	    for (UUID memberUUID : members) {
		Player member = Bukkit.getPlayer(memberUUID);
		if (member == null || !member.isOnline() || !member.hasPermission(permMember) || member.getName().equalsIgnoreCase(buyer.getName())) {
		    continue;
		}
		// Region <id> was sold to <buyer>.
		SelfServiceMessage.sendFormatedMessage(member, MessageKey.bought_region_player, regionId, buyer.getName());
		// You are member of that region
		SelfServiceMessage.sendMessage(member, MessageKey.member_of_region);
	    }
	}
    }
    
    private void explainTax(CommandSender sender, UUID taxAccountUUID, double tax) {
	if (tax != 0) {
	    SelfServiceMessage.sendFormatedMessage(sender, MessageKey.received_tax_of, PlayerUUIDConverter.toPlayerName(taxAccountUUID), format(tax));
	}
    }
    
    public void rent_ended(UUID renterUUID, Set<UUID> owners, Set<UUID> members, String regionId, String timeString) {
	
	OfflinePlayer renter = Bukkit.getOfflinePlayer(renterUUID);
	
	if (renter.isOnline()) {
	    // Rent <time> of <region> has passed
	    SelfServiceMessage.sendFormatedMessage(renter.getPlayer(), MessageKey.rent_time_passed, timeString, regionId);
	}
	
	String permOwner = Permission.INFORM_OWNER_RENTED;
	String permMember = Permission.INFORM_MEMBER_RENTED;
	
	if (owners != null) {
	    for (UUID ownerUUID : owners) {
		Player owner = Bukkit.getPlayer(ownerUUID);
		if (owner == null || !owner.isOnline() || !owner.hasPermission(permOwner) || ownerUUID.equals(renter.getUniqueId())) {
		    continue;
		}
		// Rent <time> for <player> of <region> has passed
		SelfServiceMessage.sendFormatedMessage(owner, MessageKey.rent_time_passed_player, renter.getName(), timeString, renter.getName(), regionId);
	    }
	}
	
	if (members != null) {
	    for (UUID memberUUID : members) {
		Player member = Bukkit.getPlayer(memberUUID);
		if (member == null || !member.isOnline() || !member.hasPermission(permMember) || memberUUID.equals(renter.getUniqueId())) {
		    continue;
		}
		// Rent <time> for <player> of <region> has passed
		SelfServiceMessage.sendFormatedMessage(member, MessageKey.rent_time_passed_player, renter.getName(), timeString, renter.getName(), regionId);
	    }
	}
    }
    
    public void rented(CommandSender renter, Set<UUID> owners, Set<UUID> members, String regionId, double cost, String time) {
	
	String ownerNames = toUserfriendlyString(owners);
	String permOwner = Permission.INFORM_OWNER_RENTED;
	String permMember = Permission.INFORM_MEMBER_RENTED;
	
	// You rented region <id> for <cost> from <owners>
	SelfServiceMessage.sendFormatedMessage(renter, MessageKey.rent_region, regionId, format(cost), ownerNames, time);
	
	if (owners != null) {
	    if (owners.size() > 1) {
		for (UUID ownerUUID : owners) {
		    Player owner = Bukkit.getPlayer(ownerUUID);
		    if (owner == null || !owner.isOnline() || !owner.hasPermission(permOwner) || owner.getName().equalsIgnoreCase(renter.getName())) {
			continue;
		    }
		    // Region <id> was rented out to <buyer> for <time>.
		    SelfServiceMessage.sendFormatedMessage(owner, MessageKey.rent_region_player, regionId, renter.getName(), time);
		    // Sharing <cost> with <owners>
		    SelfServiceMessage.sendFormatedMessage(owner, MessageKey.sharing_cost_with, format(cost), ownerNames);
		    // You all received an equal share of <share>
		    SelfServiceMessage.sendFormatedMessage(owner, MessageKey.received_equal_share_of, formatShare(cost, owners));
		}
	    } else {
		Player owner;
		try {
		    owner = Bukkit.getPlayer(owners.iterator().next());
		} catch (NoSuchElementException e) {
		    owner = null;
		}
		if (owner != null && owner.isOnline() && owner.hasPermission(permOwner) && !owner.getName().equalsIgnoreCase(renter.getName())) {
		    // Region <id> was rented out to <buyer> for <time>.
		    SelfServiceMessage.sendFormatedMessage(owner, MessageKey.rent_region_player, regionId, renter.getName(), time);
		    SelfServiceMessage.sendFormatedMessage(owner, MessageKey.received, format(cost));
		}
	    }
	}
	if (members != null) {
	    for (UUID memberUUID : members) {
		Player member = Bukkit.getPlayer(memberUUID);
		if (member == null || !member.isOnline() || !member.hasPermission(permMember) || member.getName().equalsIgnoreCase(renter.getName())) {
		    continue;
		}
		SelfServiceMessage.sendFormatedMessage(member, MessageKey.rent_region_player, regionId, renter.getName(), time);
		SelfServiceMessage.sendFormatedMessage(member, MessageKey.member_of_region);
	    }
	}
    }
    
    /*
     * UPFORSALE members Player "seller" put region "id" up for sale, for "cost".
     * You are member of this region. owners Player "seller" put region "id" up
     * for sale, for "cost". You are owner of this region. If the region is sold,
     * the profits are shared amongst "owners". seller You put region "id" up for
     * sale, for "cost". If the region is sold, the profits are shared amongst
     * "owners".
     */
    public void upForSale(CommandSender seller, Set<UUID> owners, Set<UUID> members, String regionId, double cost) {
	
	String ownerNames = toUserfriendlyString(owners);
	String permOwner = Permission.INFORM_OWNER_UPFORSALE;
	String permMember = Permission.INFORM_MEMBER_UPFORSALE;
	boolean sharedProfits = (owners != null && owners.size() > 1);
	
	// You put region <id> up for sale, for <cost>.
	SelfServiceMessage.sendFormatedMessage(seller, MessageKey.up_for_sale, regionId, format(cost));
	
	// If the region is sold, the profits are shared amongst <owners>.
	SelfServiceMessage.sendFormatedMessage(seller, sharedProfits ? MessageKey.bought_profits_shared : MessageKey.bought_profits_single, ownerNames);
	
	if (owners != null) {
	    for (UUID ownerUUID : owners) {
		Player player = Bukkit.getPlayer(ownerUUID);
		if (player == null || !player.isOnline() || !player.hasPermission(permOwner) || player.getName().equalsIgnoreCase(seller.getName())) {
		    continue;
		}
		// You are owner of that region.
		// Player <seller> put region <id> up for sale, for <cost>.
		SelfServiceMessage.sendFormatedMessage(player, MessageKey.up_for_sale_player, seller.getName(), regionId, format(cost));
		SelfServiceMessage.sendMessage(player, MessageKey.owner_of_region);
		SelfServiceMessage.sendFormatedMessage(seller, sharedProfits ? MessageKey.bought_profits_shared : MessageKey.bought_profits_single, ownerNames);
	    }
	} else if (members != null) {
	    for (UUID memberUUID : members) {
		Player player = Bukkit.getPlayer(memberUUID);
		if (player == null || !player.isOnline() || !player.hasPermission(permMember) || player.getName().equalsIgnoreCase(seller.getName())) {
		    continue;
		}
		// You are member of that region.
		SelfServiceMessage.sendFormatedMessage(player, MessageKey.up_for_sale_player, seller.getName(), regionId, format(cost));
		SelfServiceMessage.sendMessage(player, MessageKey.member_of_region);
	    }
	}
    }
    
    /*
     * UPFORRENT members Player "letter" put region "id" up for rent, for "cost"
     * per "time". You are member of this region. owners Player "letter" put
     * region "id" up for rent, for "cost" per "time". You are owner of this
     * region. If the region is rented out, the profits are shared amongst
     * "owners". letter You put region "id" up for rent, for "cost" per "time". If
     * the region is rented out, the profits are shared amongst "owners".
     */
    public void upForRent(CommandSender letter, Set<UUID> owners, Set<UUID> members, String regionId, double costPerTime, String time) {
	
	String ownerNames = toUserfriendlyString(owners);
	String permOwner = Permission.INFORM_OWNER_UPFORRENT;
	String permMember = Permission.INFORM_MEMBER_UPFORRENT;
	boolean sharedProfits = (owners != null && owners.size() > 1);
	
	SelfServiceMessage.sendFormatedMessage(letter, MessageKey.up_for_rent, regionId, format(costPerTime), time);
	
	// If the region is sold, the profits are shared amongst <owners>.
	SelfServiceMessage.sendFormatedMessage(letter, sharedProfits ? MessageKey.rent_profits_shared : MessageKey.rent_profits_single, ownerNames);
	
	if (owners != null) {
	    for (UUID ownerUUID : owners) {
		Player player = Bukkit.getPlayer(ownerUUID);
		if (player == null || !player.isOnline() || !player.hasPermission(permOwner) || player.getName().equalsIgnoreCase(letter.getName())) {
		    continue;
		}
		// Player <letter> put region <id> up for rent, for <cost> per <time>.
		SelfServiceMessage.sendFormatedMessage(player, MessageKey.up_for_rent_player, letter.getName(), regionId, format(costPerTime), time);
		// You are owner of this region.
		SelfServiceMessage.sendMessage(player, MessageKey.owner_of_region);
		SelfServiceMessage.sendFormatedMessage(player, sharedProfits ? MessageKey.rent_profits_shared : MessageKey.rent_profits_single, ownerNames);
	    }
	} else if (members != null) {
	    for (UUID memberUUID : members) {
		Player player = Bukkit.getPlayer(memberUUID);
		if (player == null || !player.isOnline() || !player.hasPermission(permMember) || player.getName().equalsIgnoreCase(letter.getName())) {
		    continue;
		}
		// Player <letter> put region <id> up for rent, for <cost> per <time>.
		SelfServiceMessage.sendFormatedMessage(player, MessageKey.up_for_rent_player, letter.getName(), regionId, format(costPerTime), time);
		// You are member of this region.
		SelfServiceMessage.sendMessage(player, MessageKey.member_of_region);
	    }
	}
    }
    
    /*
     * REMOVED members Player "remover" removed region "id". You were member of
     * that region. owners Player "remover" removed region "id". You were owner of
     * that region. You are sharing the refund of "refund" with "owners". You all
     * received an equal share of "share". remover Region "id" removed.
     */
    public void removed(CommandSender remover, Set<UUID> owners, Set<UUID> members, String regionId, double refund) {
	
	String ownerNames = toUserfriendlyString(owners);
	String permOwner = Permission.INFORM_OWNER_REMOVED;
	String permMember = Permission.INFORM_MEMBER_REMOVED;
	
	// Region <id> removed.
	SelfServiceMessage.sendFormatedMessage(remover, MessageKey.region_removed, regionId);
	
	if (owners != null && !owners.isEmpty()) {
	    if (owners.size() > 1) {
		for (UUID ownerUUID : owners) {
		    Player player = Bukkit.getPlayer(ownerUUID);
		    if (player == null || !player.isOnline() || !player.hasPermission(permOwner)) {
			continue;
		    }
		    if (player.getName().equalsIgnoreCase(remover.getName())) {
			// Player <remover> removed region <id>
			SelfServiceMessage.sendFormatedMessage(player, MessageKey.region_removed_player, remover.getName(), regionId);
		    }
		    SelfServiceMessage.sendMessage(player, MessageKey.owner_of_region_past);
		    if (refund > 0) {
			SelfServiceMessage.sendFormatedMessage(player, MessageKey.sharing_refund_with, format(refund), ownerNames);
			SelfServiceMessage.sendFormatedMessage(player, MessageKey.received_equal_share_of, formatShare(refund, owners));
		    }
		    
		}
	    } else {
		Player player = Bukkit.getPlayer(owners.iterator().next());
		if (player != null && player.isOnline() && player.hasPermission(permOwner)) {
		    if (player.getName().equalsIgnoreCase(remover.getName())) {
			// Player <remover> removed region <id>
			SelfServiceMessage.sendFormatedMessage(player, MessageKey.region_removed_player, remover.getName(), regionId);
		    }
		    SelfServiceMessage.sendMessage(player, MessageKey.owner_of_region_past);
		    if (refund > 0) {
			SelfServiceMessage.sendFormatedMessage(player, MessageKey.received_refund, format(refund));
		    }
		}
		
	    }
	    
	} else if (members != null) {
	    for (UUID memberUUID : members) {
		Player player = Bukkit.getPlayer(memberUUID);
		if (player == null || !player.isOnline() || !player.hasPermission(permMember)) {
		    continue;
		}
		// Player <remover> removed region <id>
		SelfServiceMessage.sendFormatedMessage(player, MessageKey.region_removed_player, remover.getName(), regionId);
		SelfServiceMessage.sendMessage(player, MessageKey.member_of_region_past);
	    }
	}
	
    }
    
    public void resized(UUID resizerUUID, Set<UUID> owners, Set<UUID> members, String regionId, double oldWorth, double newWorth, int oldWidth, int oldLength, int oldHeight, int newWidth, int newLength, int newHeight) {
	if (oldWidth * oldLength > newWidth * newLength) {
	    resized_smaller(resizerUUID, owners, members, regionId, newWorth - oldWorth, oldWidth, oldLength, oldHeight, newWidth, newLength, newHeight);
	} else {
	    resized_bigger(resizerUUID, owners, members, regionId, newWorth - oldWorth, oldWidth, oldLength, oldHeight, newWidth, newLength, newHeight);
	}
	
    }
    
    /*
     * RESIZED (bigger) members Player "resizer" resized region "id" from
     * "old-size" to "new-size". You are member of that region. owners Player
     * "resizer" payed "cost" to resize region "id" from "old-size" to "new-size".
     * You are owner of that region. resizer You payed "cost" to resize region
     * "id" from "old-size" to "new-size".
     *
     * RESIZED (smaller) members Player "resizer" resized region "id" from
     * "old-size" to "new-size". You are member of that region. owners Player
     * "resizer" resized region "id" from "old-size" to "new-size". You are owner
     * of that region. The region became smaller, so you are sharing the refund of
     * "refund" with "owners". You all received an equal share of "share". resizer
     * You payed "cost" to resize region "id" from "old-size" to "new-size".
     *
     * CREATE creator Region "id" protected. ...show region info....
     */
    public void resized_bigger(UUID resizerUUID, Set<UUID> owners, Set<UUID> members, String regionId, double cost, int oldWidth, int oldLength, int oldHeight, int newWidth, int newLength, int newHeight) {
	
	String permOwner = Permission.INFORM_OWNER_RESIZE;
	String permMember = Permission.INFORM_MEMBER_RESIZE;
	
	String oldSize = formatSize(oldWidth, oldLength, oldHeight);
	String newSize = formatSize(newWidth, newLength, newHeight);
	
	Player resizer = Bukkit.getPlayer(resizerUUID);
	if (cost > 0) {
	    SelfServiceMessage.sendFormatedMessage(resizer, MessageKey.resize_region_pay, format(cost), regionId, oldSize, newSize);
	} else {
	    SelfServiceMessage.sendFormatedMessage(resizer, MessageKey.resize_region, regionId, oldSize, newSize);
	}
	
	if (owners != null) {
	    for (UUID ownerUUID : owners) {
		Player player = Bukkit.getPlayer(ownerUUID);
		if (player == null || !player.isOnline() || !player.hasPermission(permOwner) || player.getName().equalsIgnoreCase(resizer.getName())) {
		    continue;
		}
		if (!player.getName().equalsIgnoreCase(resizer.getName())) {
		    SelfServiceMessage.sendFormatedMessage(player, MessageKey.resize_region_player, resizer.getName(), regionId, oldSize, newSize);
		}
		SelfServiceMessage.sendMessage(player, MessageKey.owner_of_region);
	    }
	    
	} else if (members != null) {
	    for (UUID memberUUID : members) {
		Player player = Bukkit.getPlayer(memberUUID);
		if (player == null || !player.isOnline() || !player.hasPermission(permMember) || player.getName().equalsIgnoreCase(resizer.getName())) {
		    continue;
		}
		if (!player.getName().equalsIgnoreCase(resizer.getName())) {
		    SelfServiceMessage.sendFormatedMessage(player, MessageKey.resize_region_player, resizer.getName(), regionId, oldSize, newSize);
		}
		SelfServiceMessage.sendMessage(player, MessageKey.member_of_region);
	    }
	}
    }
    
    public void resized_smaller(UUID resizerUUID, Set<UUID> owners, Set<UUID> members, String regionId, double refund, int oldWidth, int oldLength, int oldHeight, int newWidth, int newLength, int newHeight) {
	
	String ownerNames = toUserfriendlyString(owners);
	String permOwner = Permission.INFORM_OWNER_RESIZE;
	String permMember = Permission.INFORM_MEMBER_RESIZE;
	
	String oldSize = formatSize(oldWidth, oldLength, oldHeight);
	String newSize = formatSize(newWidth, newLength, newHeight);
	
	Player resizer = Bukkit.getPlayer(resizerUUID);
	SelfServiceMessage.sendFormatedMessage(resizer, MessageKey.resize_region, regionId, oldSize, newSize);
	
	if (owners != null) {
	    if (owners.size() > 1) {
		for (UUID ownerUUID : owners) {
		    Player player = Bukkit.getPlayer(ownerUUID);
		    if (player == null || !player.isOnline() || !player.hasPermission(permOwner)) {
			continue;
		    }
		    if (!player.getName().equalsIgnoreCase(resizer.getName())) {
			SelfServiceMessage.sendFormatedMessage(player, MessageKey.resize_region_player, resizer.getName(), regionId, oldSize, newSize);
		    }
		    SelfServiceMessage.sendMessage(player, MessageKey.owner_of_region);
		    if (refund != 0) {
			SelfServiceMessage.sendFormatedMessage(player, MessageKey.sharing_refund_with, format(Math.abs(refund)), ownerNames);
			SelfServiceMessage.sendFormatedMessage(player, MessageKey.received_equal_share_of, formatShare(Math.abs(refund), owners));
		    }
		}
	    } else {
		Player player = Bukkit.getPlayer(owners.iterator().next());
		if (player != null && player.isOnline() && player.hasPermission(permOwner)) {
		    if (!player.getName().equalsIgnoreCase(resizer.getName())) {
			SelfServiceMessage.sendFormatedMessage(player, MessageKey.resize_region_player, resizer.getName(), regionId, oldSize, newSize);
		    }
		    if (refund != 0) {
			SelfServiceMessage.sendFormatedMessage(player, MessageKey.received_refund, format(Math.abs(refund)));
		    }
		}
	    }
	    
	} else if (members != null) {
	    for (UUID memberUUID : members) {
		Player player = Bukkit.getPlayer(memberUUID);
		if (player == null || !player.isOnline() || !player.hasPermission(permMember)) {
		    continue;
		}
		if (!player.getName().equalsIgnoreCase(resizer.getName())) {
		    SelfServiceMessage.sendFormatedMessage(player, MessageKey.resize_region_player, resizer.getName(), regionId, oldSize, newSize);
		}
		SelfServiceMessage.sendMessage(player, MessageKey.member_of_region);
	    }
	}
    }
    
    private String formatSize(int width, int length, int height) {
	return width + "x" + length + "x" + height;
    }
    
    private String format(double amount) {
	return this.economy.format(amount);
    }
    
    private double formatShare(double cost, Set<UUID> ownerUUIDs) {
	if (ownerUUIDs == null || ownerUUIDs.isEmpty()) {
	    return 0;
	} else {
	    return cost / ownerUUIDs.size();
	}
    }
    
    private String toUserfriendlyString(Set<UUID> uuids) {
	if (uuids == null || uuids.isEmpty()) {
	    return "nobody";
	}
	String result = "";
	for (UUID uuid : uuids) {
	    result += ", " + Bukkit.getOfflinePlayer(uuid).getName();
	}
	return result.substring(2);
    }
}
