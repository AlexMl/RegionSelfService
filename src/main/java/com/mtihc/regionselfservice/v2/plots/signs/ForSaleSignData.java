package com.mtihc.regionselfservice.v2.plots.signs;

import java.util.Map;
import org.bukkit.util.BlockVector;

public class ForSaleSignData extends PlotSignData {
  public ForSaleSignData(ForSaleSignData other) {
    super(other);
  }

  public ForSaleSignData(BlockVector coords) {
    super(PlotSignType.FOR_SALE, coords);
  }

  public ForSaleSignData(Map<String, Object> values) {
    super(values);
  }

  public Map<String, Object> serialize() {
    return super.serialize();
  }
}
