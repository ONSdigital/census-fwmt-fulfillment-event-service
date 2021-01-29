package uk.gov.ons.census.fwmt.fulfilment.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.fulfilment.lookup.ChannelLookup;
import uk.gov.ons.census.fwmt.fulfilment.lookup.PauseRulesLookup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static java.nio.charset.StandardCharsets.UTF_8;

@Configuration
public class FulfilmentSetup {
  @Autowired
  ResourceLoader resourceLoader;

  @Value(value = "${fulfilment.channel.path}")
  private String channel;

  @Value(value = "${fulfilment.pauseRules.path}")
  private String transitionRules;

  @Bean
  public ChannelLookup buildChannelLookup() throws GatewayException {
    String channelLine;
    Resource resource = resourceLoader.getResource(channel);

    ChannelLookup channelLookup = new ChannelLookup();

    try (BufferedReader in = new BufferedReader(new InputStreamReader(resource.getInputStream(), UTF_8))) {
      while ((channelLine = in.readLine()) != null) {
        channelLookup.add(channelLine, channelLine);
      }
    }catch (IOException e) {
      throw new GatewayException(GatewayException.Fault.SYSTEM_ERROR, e, "Cannot process transition rule lookup");
    }
    return channelLookup;
  }

  @Bean
  public PauseRulesLookup buildTransitionRuleLookup() throws GatewayException {
    String pauseLine;
    Resource resource = resourceLoader.getResource(transitionRules);

    PauseRulesLookup pauseRulesLookup = new PauseRulesLookup();

    try (BufferedReader in = new BufferedReader(new InputStreamReader(resource.getInputStream(), UTF_8))) {
      while ((pauseLine = in.readLine()) != null) {
        String[] lookup = pauseLine.split("\\|");
        String pauseRule = lookup[1];
        String productCode = lookup[0];
        pauseRulesLookup.add(productCode, pauseRule);
      }
    }catch (IOException e) {
      throw new GatewayException(GatewayException.Fault.SYSTEM_ERROR, e, "Cannot process transition rule lookup");
    }
    return pauseRulesLookup;
  }
}