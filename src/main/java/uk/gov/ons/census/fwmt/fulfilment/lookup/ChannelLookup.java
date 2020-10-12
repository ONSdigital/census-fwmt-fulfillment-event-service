package uk.gov.ons.census.fwmt.fulfilment.lookup;

import java.util.HashMap;
import java.util.Map;

public class ChannelLookup {

  private final Map<String, String> channelMap = new HashMap<>();

  public String getLookup(String channel) {
    return channelMap.get(channel);
  }

  public void add (String channelSelector, String channel) {
    channelMap.put(channelSelector, channel);
  }
}
