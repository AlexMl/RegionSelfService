package com.mtihc.regionselfservice.v2.plots;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Sign;
import com.mtihc.regionselfservice.v2.plots.exceptions.SignException;
import com.mtihc.regionselfservice.v2.plots.signs.PlotSignText;
import com.sk89q.worldguard.protection.managers.RegionManager;

public class PlotWorld {
  protected final PlotManager manager;
  protected final UUID worldUID;
  protected final IPlotWorldConfig config;
  protected final IPlotDataRepository plots;
  protected final RegionManager regionManager;

  public PlotWorld(PlotManager manager, World world, IPlotWorldConfig config, IPlotDataRepository plots) {
    this.manager = manager;
    this.worldUID = world.getUID();
    this.config = config;
    this.plots = plots;

    this.regionManager = manager.getWorldGuard().getRegionManager(world);
  }

  public String getName() {
    return getWorld().getName();
  }

  public UUID getWorldUID() {
    return this.worldUID;
  }

  public World getWorld() {
    return Bukkit.getWorld(getWorldUID());
  }

  public IPlotWorldConfig getConfig() {
    return this.config;
  }

  public PlotManager getPlotManager() {
    return this.manager;
  }

  public IPlotDataRepository getPlotData() {
    return this.plots;
  }

  public Plot getPlot(String regionId) {
    PlotData data = this.plots.get(regionId);
    if (data == null) {
      data = new PlotData(regionId, 0, 0, 0);
    }
    return createPlot(data);
  }

  public Plot getPlot(Sign sign) throws SignException {
    String regionId = PlotSignText.getRegionId(sign.getLines());
    return getPlot(regionId);
  }

  protected Plot createPlot(PlotData data) {
    return new Plot(this, data);
  }

  public RegionManager getRegionManager() {
    return this.regionManager;
  }

  public Set<UUID> getPotentialHomeless(Set<UUID> playerUUIDs) {
    Set<UUID> result = new HashSet<UUID>();

    // iterate over regions owners
    if (!playerUUIDs.isEmpty()) {
      for (UUID playerUUID : playerUUIDs) {
        // count regions of owner
        int count = this.manager.control.getRegionCountOfPlayer(getWorld(), playerUUID);
        if (count < 2) {
          // player only has 1 region -> would get homeless if he
          // sells it
          result.add(playerUUID);
        }
      }
    }
    return result;
  }
}
