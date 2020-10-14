package uk.gov.ons.census.fwmt.fulfilment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import uk.gov.ons.census.fwmt.fulfilment.data.GatewayCache;

import java.util.List;

@Repository
public interface GatewayCacheRepository extends JpaRepository<GatewayCache, Long> {

  @Query("SELECT DISTINCT household.caseId FROM GatewayCache household WHERE household.caseId = :caseId AND "
      + "household.type = :type AND household.existsInFwmt = :exists")
  String findByCaseIdAndTypeAndExistsInFwmt(@Param("caseId") String caseId, @Param("type") int type, @Param("exists") boolean exists);

  @Query("SELECT DISTINCT household.individualCaseId FROM GatewayCache household WHERE household.individualCaseId = :caseId AND "
      + "household.type = :type AND household.existsInFwmt = :exists")
  String findByIndividualCaseIdAndTypeAndExistsInFwmt(@Param("caseId") String caseId, @Param("type") int type, @Param("exists") boolean exists);

  @NonNull
  List<GatewayCache> findAll();

}
