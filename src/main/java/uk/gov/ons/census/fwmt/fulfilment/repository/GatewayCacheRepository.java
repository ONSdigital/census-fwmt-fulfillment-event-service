package uk.gov.ons.census.fwmt.fulfilment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import uk.gov.ons.census.fwmt.fulfilment.data.GatewayCache;

import javax.persistence.LockModeType;

@Repository
public interface GatewayCacheRepository extends JpaRepository<GatewayCache, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  GatewayCache findByCaseIdAndTypeAndExistsInFwmt(String caseId, int type, boolean exists);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  GatewayCache findByIndividualCaseIdAndTypeAndExistsInFwmt(String caseId, int type, boolean exists);

}
