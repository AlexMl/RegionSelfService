package com.mtihc.regionselfservice.v2.plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import com.mtihc.regionselfservice.v2.plots.IPlotWorldConfig;
import com.mtihc.regionselfservice.v2.plugin.util.YamlFile;
import com.mtihc.regionselfservice.v2.util.PlayerUUIDConverter;


public class PlotWorldConfig extends YamlFile implements IPlotWorldConfig {
    
    public PlotWorldConfig(String filePath) {
	this(new File(filePath), null);
    }
    
    public PlotWorldConfig(File file) {
	this(file, null);
    }
    
    public PlotWorldConfig(String filePath, Logger logger) {
	this(new File(filePath), logger);
    }
    
    public PlotWorldConfig(File file, Logger logger) {
	super(file, logger);
    }
    
    public double getBlockWorth() {
	return getConfig().getDouble("block_worth");
    }
    
    public double getOnSellMinBlockCost() {
	return getConfig().getDouble("sell_min_block_cost");
    }
    
    public double getOnSellMaxBlockCost() {
	return getConfig().getDouble("sell_max_block_cost");
    }
    
    public double getOnRentMinBlockCost() {
	return getConfig().getDouble("rent_min_block_cost");
    }
    
    public double getOnRentMaxBlockCost() {
	return getConfig().getDouble("rent_max_block_cost");
    }
    
    public int getMaxRegionCount() {
	return getConfig().getInt("max_regions_per_player");
    }
    
    public boolean isReserveFreeRegionsEnabled() {
	return getConfig().getBoolean("reserve_free_regions");
    }
    
    public int getMinimumY() {
	return getConfig().getInt("region_size.minimum_y");
    }
    
    public int getMaximumY() {
	return getConfig().getInt("region_size.maximum_y");
    }
    
    public int getMinimumHeight() {
	return getConfig().getInt("region_size.minimum_height");
    }
    
    public int getMaximumHeight() {
	return getConfig().getInt("region_size.maximum_height");
    }
    
    public int getMinimumWidthLength() {
	return getConfig().getInt("region_size.minimum_width_length");
    }
    
    public int getMaximumWidthLength() {
	return getConfig().getInt("region_size.maximum_width_length");
    }
    
    public int getDefaultBottomY() {
	return getConfig().getInt("region_defaults.bottom_y");
    }
    
    public int getDefaultTopY() {
	return getConfig().getInt("region_defaults.top_y");
    }
    
    public List<UUID> getDefaultOwnerUUIDs() {
	List<UUID> ownerUUIDs = new ArrayList<UUID>();
	
	for (String ownerName : getConfig().getStringList("region_defaults.owners")) {
	    ownerUUIDs.add(PlayerUUIDConverter.toUUID(ownerName));
	}
	return ownerUUIDs;
    }
    
    public boolean isOverlapUnownedRegionAllowed() {
	return getConfig().getBoolean("allow_overlap_unowned_regions");
    }
    
    public boolean isAutomaticParentEnabled() {
	return getConfig().getBoolean("region_defaults.parent_automatic");
    }
    
    public boolean isCreateCostEnabled() {
	return getConfig().getBoolean("enable_create_cost");
    }
    
    public UUID getTaxAccountHolder() {
	return PlayerUUIDConverter.toUUID(getConfig().getString("tax_to_account"));
    }
    
    public double getTaxPercent() {
	return getConfig().getDouble("tax_percent");
    }
    
    public double getTaxFromPrice() {
	return getConfig().getDouble("tax_from_price");
    }
    
    public double getDeleteRefundPercent() {
	return getConfig().getDouble("percent_delete_refund");
    }
    
    public double getAllowRentExtendAfterPercentTime() {
	return getConfig().getDouble("allow_rent_extend_after_percent_time");
    }
}
