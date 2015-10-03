package com.mtihc.regionselfservice.v2.util;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;


public class PlayerUUIDConverter {
    
    public static Player toPlayer(UUID uuid) {
	return Bukkit.getPlayer(uuid);
    }
    
    public static OfflinePlayer toOfflinePlayer(UUID uuid) {
	return Bukkit.getOfflinePlayer(uuid);
    }
    
    public static String toPlayerName(UUID uuid) {
	return toOfflinePlayer(uuid).getName();
    }
}
