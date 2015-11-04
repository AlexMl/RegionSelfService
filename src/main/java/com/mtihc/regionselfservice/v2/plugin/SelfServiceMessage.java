package com.mtihc.regionselfservice.v2.plugin;

import java.io.File;
import java.io.IOException;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;


public class SelfServiceMessage {
    
    public enum MessageKey {
	sharing_cost_with(ChatColor.GREEN + "Sharing " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " with " + ChatColor.WHITE + "%s" + ChatColor.GREEN + "."),
	sharing_refund_with(ChatColor.GREEN + "Sharing the refund of " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " with " + ChatColor.WHITE + "%s" + ChatColor.GREEN + "."),
	received(ChatColor.GREEN + "You received " + ChatColor.WHITE + "%s" + ChatColor.GREEN + "."),
	received_refund(ChatColor.GREEN + "You received the refund of " + ChatColor.WHITE + "%s" + ChatColor.GREEN + "."),
	received_equal_share_of(ChatColor.GREEN + "You all received an equal share of " + ChatColor.WHITE + "%s" + ChatColor.GREEN + "."),
	received_tax_of(ChatColor.WHITE + "%s" + ChatColor.GREEN + " received the tax of " + ChatColor.WHITE + "%s" + ChatColor.GREEN + "."),
	
	owner_of_region(ChatColor.GREEN + "You are owner of that region."),
	owner_of_region_past(ChatColor.GREEN + "You were owner of that region."),
	member_of_region(ChatColor.GREEN + "You are member of that region."),
	member_of_region_past(ChatColor.GREEN + "You were member of that region."),
	
	up_for_sale(ChatColor.GREEN + "You put region " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " up for sale, for " + ChatColor.WHITE + "%s" + ChatColor.GREEN + "."),
	up_for_sale_player(ChatColor.GREEN + "Player " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " put region " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " up for sale, for " + ChatColor.WHITE + "%s" + ChatColor.GREEN + "."),
	
	bought_region(ChatColor.GREEN + "You bought region " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " for " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " from " + ChatColor.WHITE + "%s" + ChatColor.GREEN + "."),
	bought_region_player(ChatColor.GREEN + "Region " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " was sold to " + ChatColor.WHITE + "%s" + ChatColor.GREEN + "."),
	bought_profits_single(ChatColor.GREEN + "If the region is sold, the profits are for " + ChatColor.WHITE + "%s" + ChatColor.GREEN + "."),
	bought_profits_shared(ChatColor.GREEN + "If the region is sold, the profits are shared amongst " + ChatColor.WHITE + "%s" + ChatColor.GREEN + "."),
	
	up_for_rent(ChatColor.GREEN + "You put region " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " up for rent, for " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " per " + ChatColor.WHITE + "%s" + ChatColor.GREEN + "."),
	up_for_rent_player(ChatColor.GREEN + "Player " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " put region " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " up for rent, for " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " per " + ChatColor.WHITE + "%s" + ChatColor.GREEN + "."),
	
	rent_region(ChatColor.GREEN + "You rented region " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " for " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " from " + ChatColor.WHITE + "%s" + ChatColor.GREEN + ", for " + ChatColor.WHITE + "%s" + ChatColor.GREEN + "."),
	rent_region_player(ChatColor.GREEN + "Region " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " was rented out to " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " for " + ChatColor.WHITE + "%s" + ChatColor.GREEN + "."),
	rent_profits_single(ChatColor.GREEN + "If the region is rented out, the profits are for " + ChatColor.WHITE + "%s" + ChatColor.GREEN + "."),
	rent_profits_shared(ChatColor.GREEN + "If the region is rented out, the profits are shared amongst " + ChatColor.WHITE + "%s" + ChatColor.GREEN + "."),
	
	rent_time_passed(ChatColor.RED + "The rent time of %s has passed. You are no longer a member of region '%s'."),
	rent_time_passed_player(ChatColor.RED + "Player %s's rent time of %s has passed. %s is no longer a member of region '%s'."),
	rent_time_warning(ChatColor.GREEN + "If you want to stay member of region " + ChatColor.WHITE + "%s" + ChatColor.GREEN + ", you should extend the rent time now. You have " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " remaining."),
	
	region_removed(ChatColor.YELLOW + "Region " + ChatColor.WHITE + "%s" + ChatColor.YELLOW + " removed."),
	region_removed_player(ChatColor.GREEN + "Player " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " removed region " + ChatColor.WHITE + "%s" + ChatColor.GREEN + "."),
	region_worth(ChatColor.GREEN + "A Region with a size of " + ChatColor.WHITE + "%sx%s" + ChatColor.GREEN + " blocks, in world '%s' is worth about " + ChatColor.WHITE + "%s" + ChatColor.GREEN + "."),
	
	resize_region(ChatColor.GREEN + "You resized region " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " from " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " to " + ChatColor.WHITE + "%s" + ChatColor.GREEN + "."),
	resize_region_pay(ChatColor.GREEN + "You payed " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " and resized region " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " from " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " to " + ChatColor.WHITE + "%s" + ChatColor.GREEN + "."),
	resize_region_player(ChatColor.GREEN + "Player " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " resized region " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " from " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " to " + ChatColor.WHITE + "%s" + ChatColor.GREEN + "."),
	
	player_owns(ChatColor.GREEN + "Player " + ChatColor.WHITE + "'%s'" + ChatColor.GREEN + " owns %s" + ChatColor.GREEN + " regions in world " + ChatColor.WHITE + "'%s'" + ChatColor.GREEN + "."),
	player_not_bought(ChatColor.RED + "Did not buy region %s."),
	player_not_rented(ChatColor.RED + "Did not rent region %s."),
	player_not_create(ChatColor.RED + "Did not create a region."),
	player_not_resize(ChatColor.RED + "Did not resize region."),
	player_not_delete(ChatColor.RED + "Did not delete region."),
	player_not_extend_renttime(ChatColor.RED + "Did not extend rent time of region %s."),
	player_can_not_delete("Sorry, you can't delete this region."),
	player_can_not_delete_renting("You can't delete this region when players are still renting it."),
	player_has_perm_skip_pay(ChatColor.GREEN + "You have permission to skip payment."),
	player_will_buy(ChatColor.GREEN + "Are you sure you want to buy region " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " for " + ChatColor.WHITE + "%s" + ChatColor.GREEN + "?"),
	player_will_rent(ChatColor.GREEN + "Are you sure you want to pay " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " to rent region " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " for " + ChatColor.WHITE + "%s" + ChatColor.GREEN + "?"),
	player_will_rent_extend(ChatColor.GREEN + "Are you sure you want to pay " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " to extend the rent time of region " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " with " + ChatColor.WHITE + "%s" + ChatColor.GREEN + "?"),
	player_will_create(ChatColor.GREEN + "Are you sure you want to pay " + ChatColor.WHITE + "%s" + ChatColor.GREEN + "and create region '" + ChatColor.WHITE + "%s" + ChatColor.GREEN + "?"),
	player_will_resize_pay(ChatColor.GREEN + "Are you sure you want to pay " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " and resize region '" + ChatColor.WHITE + "%s" + ChatColor.GREEN + "' from " + ChatColor.WHITE + "%sx%sx%s" + ChatColor.GREEN + " to " + ChatColor.WHITE + "%sx%sx%s" + ChatColor.GREEN + "?"),
	player_will_resize(ChatColor.GREEN + "Are you sure you want to resize region '" + ChatColor.WHITE + "%s" + ChatColor.GREEN + "' from " + ChatColor.WHITE + "%sx%sx%s" + ChatColor.GREEN + " to " + ChatColor.WHITE + "%sx%sx%s" + ChatColor.GREEN + "?"),
	player_will_delete(ChatColor.GREEN + "Are you sure you want to delete region '" + ChatColor.WHITE + "%s" + ChatColor.GREEN + "'?"),
	player_will_delete_share(ChatColor.GREEN + "Are you sure you want to delete region '" + ChatColor.WHITE + "%s" + ChatColor.GREEN + "' and share the refund of " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " amongst '" + ChatColor.WHITE + "%s" + ChatColor.GREEN + "'?"),
	
	prompt_type("Type"),
	prompt_yes("YES"),
	prompt_or("or"),
	prompt_no("NO"),
	
	sign_rent_price_low(ChatColor.RED + "The price is to low. The rent-price must be between %s and %s per %s. In other words, between %s and %s per hour."),
	sign_rent_price_high(ChatColor.RED + "The price is to high. The rent-price must be between %s and %s per %s. In other words, between %s and %s per hour."),
	sign_sell_price_low(ChatColor.RED + "The price is to low. The sell-price must be between %s and %s."),
	sign_sell_price_high(ChatColor.RED + "The price is to high. The sell-price must be between %s and %s."),
	
	sign_broke(ChatColor.GREEN + "You broke the last %s sign of region '%s'."),
	sign_broke_last(ChatColor.GREEN + "You broke a %s sign of region '%s'. There are %s %s signs left."),
	
	config_reload(ChatColor.GREEN + "Configuration reloaded."),
	
	error_to_much_regions("You already own %s regions (max: %s)."),
	error_no_sign("You're not looking at a wooden sign."),
	error_no_valid_sign_looking("You're not looking at a valid sign: %s"),
	error_looking_wrong_sign("You're looking at the wrong sign! Check your command!"),
	error_selection_first("Select a region first. Use WorldEdit's command: " + ChatColor.LIGHT_PURPLE + "//wand"),
	error_selection_overlaps("Your selection overlaps with someone else's region."),
	error_selection_to_small("The selected region is to small! Your selection's size is %sx%sx%s. But the minimum is %sx%sx%s."),
	error_selection_to_big("The selected region is to big! Your selection's size is %sx%sx%s. But the maximum width/length/height is %sx%sx%s."),
	error_selection_to_low("The bottom-y coordinate is to low! The bottom-y coordinate is %s. But shouldn't be lower than %s."),
	error_selection_to_high("The top-y coordinate is to high! The top-y coordinate is %s. But shouldn't be greater than %s."),
	error_selection("You selection's coordinates are not correct!"),
	
	error_region_not_exists(ChatColor.RED + "Region '%s' does not exist."),
	error_region_already_exists("Region '%s' already exists."),
	error_region_not_own(ChatColor.RED + "You don't own this region."),
	error_region_invalid_name("Invalid region name '%s'. Try a different name."),
	error_region_not_for_sale("Region '%s' is not for sale!"),
	error_region_not_for_rent("Region '%s' is not for rent!"),
	error_region_reserved("Free regions are reserved for new players."),
	error_region_redefine("You can only redefine your own regions."),
	error_region_create_not_allowed("You are not allowed to create this region!"),
	error_region_send(ChatColor.RED + "Failed to send region info. Region '%s' doesn't exist."),
	error_region_saved(ChatColor.RED + "Failed to save new region with id '%s': %s"),
	error_region_delete(ChatColor.RED + "Failed to delete region '%s'. %s"),
	error_region_pay(ChatColor.RED + "Failed to pay for the region: %s"),
	error_region_can_not_sell(ChatColor.RED + "Sorry, you can't sell this region."),
	error_already_own_region("You already own this region!"),
	error_already_member_region("You're already member of this region."),
	
	error_can_not_extend_sign("You can't extend your rent time via this sign."),
	error_can_not_extend_yet("You can't extend the rent time yet. You have to wait %s."),
	error_can_not_buy("Sorry, you can't buy this region"),
	error_not_enough_money("You can not afford it! You only have %s. You still require %s."),
	error_people_get_homeless("The following players would become homeless: %s"),
	error_still_renting("The current owner is still renting it out to other players."),
	error_rent_no_perm(ChatColor.RED + "You don't have permission to rent out that region."),
	error_sell_no_perm(ChatColor.RED + "You don't have permission to sell that region."),
	error_rent_not_own(ChatColor.RED + "You can't rent out regions that you don't own."),
	error_sell_not_own(ChatColor.RED + "You can't sell regions that you don't own."),
	
	error_sign_not_valid(ChatColor.RED + "Not a valid sign."),
	error_sign_not_valid_type("Not a valid plot sign. Could not find the matching sign type."),
	error_sign_not_valid_rent(ChatColor.RED + "Invalid sign text. Expected rent-cost and rent-time."),
	error_sign_creation(ChatColor.RED + "Failed to create %s sign: %s"),
	error_sign_creation_region(ChatColor.RED + "Failed to create %s sign. Region %s doesn't exist."),
	error_sign_no_region("Region name is not specified on lines 3 and/or 4."),
	error_sign_no_cost("There is no cost specified on line 2."),
	error_sign_outside(ChatColor.RED + "You can't place this sign outside the region itself."),
	error_no_plotinfo("Couldn't find plot information."),
	
	error_unknown_command(ChatColor.RED + "Unknown command: " + ChatColor.WHITE + "/%s %s"),
	error_get_cmd_help("To get command help, type: " + ChatColor.WHITE + "/%s ?"),
	error_invalid_arguments("Incorrect number of arguments."),
	error_expected_player_name("Expected player name."),
	error_expected_region_id("Expected a region id."),
	error_expected_world_name("Expected world name."),
	error_expected_number("Expected a number instead of text"),
	error_player_not_exists("Player '%s' doesn't exist."),
	error_world_not_exists("World %s doesn't exist."),
	error_no_perm_command("You're not allowed to execute command /%s.");
	
	private String defaultMessage;
	private String fileMessage;
	
	private MessageKey(String message) {
	    this.defaultMessage = message;
	}
	
	private MessageKey(String defaultMessage, String fileMessage) {
	    this.defaultMessage = defaultMessage;
	    this.fileMessage = fileMessage;
	}
	
	public void setFileMessage(String message) {
	    this.fileMessage = message;
	}
	
	public String getDefaultMessage() {
	    return this.defaultMessage;
	}
	
	public String getFileMessage() {
	    return this.fileMessage;
	}
	
	public String getMessage() {
	    return getFileMessage() == null ? getDefaultMessage() : getFileMessage();
	}
    }
    
    public SelfServiceMessage(SelfServicePlugin plugin) throws IOException {
	
	File messageFile = new File(plugin.getDataFolder(), "messages.yml");
	FileConfiguration messageConfig = YamlConfiguration.loadConfiguration(messageFile);
	
	if (!messageFile.exists()) {
	    messageFile.createNewFile();
	}
	
	for (MessageKey key : MessageKey.values()) {
	    messageConfig.addDefault("messages." + key.name(), key.getDefaultMessage());
	}
	
	messageConfig.options().copyDefaults(true);
	messageConfig.save(messageFile);
	
	for (MessageKey key : MessageKey.values()) {
	    key.setFileMessage(messageConfig.getString("messages." + key.name()));
	}
    }
    
    public static String getMessage(MessageKey key) {
	return key.getMessage();
    }
    
    public static String getFormatedMessage(MessageKey key, Object... args) {
	return String.format(getMessage(key), args);
    }
    
    public static void sendMessage(CommandSender sender, MessageKey key) {
	sender.sendMessage(getMessage(key));
    }
    
    public static void sendFormatedMessage(CommandSender sender, MessageKey key, Object... args) {
	sender.sendMessage(getFormatedMessage(key, args));
    }
}
