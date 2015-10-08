package com.mtihc.regionselfservice.v2.plots.signs;

import java.util.Map;
import java.util.UUID;

import org.bukkit.util.BlockVector;

import com.mtihc.regionselfservice.v2.util.PlayerUUIDConverter;


public class ForRentSignData extends PlotSignData {
    
    private UUID rentPlayer;
    private long rentPlayerTime;
    
    public ForRentSignData(ForRentSignData other) {
	super(other);
	this.rentPlayer = other.rentPlayer;
	this.rentPlayerTime = other.rentPlayerTime;
    }
    
    public ForRentSignData(BlockVector coords) {
	super(PlotSignType.FOR_RENT, coords);
	// when sign is created, nobody is renting yet
	this.rentPlayer = null;
	this.rentPlayerTime = 0;
    }
    
    public ForRentSignData(Map<String, Object> values) {
	super(values);
	this.rentPlayer = PlayerUUIDConverter.fromString((String) values.get("rent-player"));
	this.rentPlayerTime = (Integer) values.get("rent-player-time");
    }
    
    @Override
    public Map<String, Object> serialize() {
	Map<String, Object> values = super.serialize();
	String uuidString = (this.rentPlayer == null) ? null : this.rentPlayer.toString();
	values.put("rent-player", uuidString);
	values.put("rent-player-time", this.rentPlayerTime);
	return values;
    }
    
    public UUID getRentPlayerUUID() {
	return this.rentPlayer;
    }
    
    public void setRentPlayer(UUID playerUUID) {
	this.rentPlayer = playerUUID;
    }
    
    public long getRentPlayerTime() {
	return this.rentPlayerTime;
    }
    
    public void setRentPlayerTime(long millisec) {
	this.rentPlayerTime = millisec;
    }
    
    public boolean isRentedOut() {
	return this.rentPlayer != null;
    }
}
