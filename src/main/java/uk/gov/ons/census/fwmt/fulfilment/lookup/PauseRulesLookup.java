package uk.gov.ons.census.fwmt.fulfilment.lookup;

import java.util.HashMap;
import java.util.Map;

public class PauseRulesLookup {

  private final Map<String, String> pauseRulesMap = new HashMap<>();

  public String getLookup(String productCode) {
    return pauseRulesMap.get(productCode);
  }

  public void add (String productCode, String pauseRule) {
    pauseRulesMap.put(productCode, pauseRule);
  }
}
