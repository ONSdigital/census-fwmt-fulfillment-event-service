package uk.gov.ons.census.fwmt.fulfilment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.ons.census.fwmt.fulfilment.data.GatewayCache;
import uk.gov.ons.census.fwmt.fulfilment.repository.GatewayCacheRepository;

/**
 * This class is bare-bones because it's a simple connector between the rest of the code and the caching implementation
 * Please don't subvert this class by touching the GatewayCacheRepository
 * If we ever change from a database to redis, this class will form the breaking point
 */

@Slf4j
@Service
public class GatewayCacheService {
  public final GatewayCacheRepository repository;

  public GatewayCacheService(GatewayCacheRepository repository) {
    this.repository = repository;
  }

  public GatewayCache getByIdAndTypeAndExists(String caseId, int type, boolean exists) {
    return repository.findByCaseIdAndTypeAndExistsInFwmt(caseId, type, exists); }

  public GatewayCache getByIndividualCaseIdAndTypeAndExists(String indCaseId, int type, boolean exists) {
    return repository.findByIndividualCaseIdAndTypeAndExistsInFwmt(indCaseId, type, exists); }
}
