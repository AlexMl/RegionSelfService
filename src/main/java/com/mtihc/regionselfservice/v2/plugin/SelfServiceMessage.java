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
	sharing_refund_with(ChatColor.GREEN + "You're sharing the refund of " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " with " + ChatColor.WHITE + "%s" + ChatColor.GREEN + "."),
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
	
	rent_time_passed(ChatColor.RED + "The rent time of %s has passed. You are no longer a member of region \"%s\"."),
	rent_time_passed_player(ChatColor.RED + "Player %s's rent time of %s has passed. %s is no longer a member of region \"%s\"."),
	
	region_removed(ChatColor.YELLOW + "Region " + ChatColor.WHITE + "%s" + ChatColor.YELLOW + " removed."),
	region_removed_player(ChatColor.GREEN + "Player " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " removed region " + ChatColor.WHITE + "%s" + ChatColor.GREEN + "."),
	
	resize_region(ChatColor.GREEN + "You resized region " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " from " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " to " + ChatColor.WHITE + "%s" + ChatColor.GREEN + "."),
	resize_region_pay(ChatColor.GREEN + "You payed " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " and resized region " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " from " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " to " + ChatColor.WHITE + "%s" + ChatColor.GREEN + "."),
	resize_region_player(ChatColor.GREEN + "Player " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " resized region " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " from " + ChatColor.WHITE + "%s" + ChatColor.GREEN + " to " + ChatColor.WHITE + "%s" + ChatColor.GREEN + "."), ;
	
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
