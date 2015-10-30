package com.mtihc.regionselfservice.v2.plugin;

import java.io.File;
import java.io.IOException;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;


public class SelfServiceMessage {
    
    public enum MessageKey {
	test("");
	
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
