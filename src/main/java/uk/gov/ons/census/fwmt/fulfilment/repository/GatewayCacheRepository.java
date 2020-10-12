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
  GatewayCache findByCaseIdAndTypeAndExistsInFwmt(String caseId, int type, boolean exists);

  GatewayCache findByIndividualCaseIdAndTypeAndExistsInFwmt(String indCaseId, int type, boolean exists);

  boolean existsByEstabUprn(String uprn);

  boolean existsByEstabUprnAndType(String estabUprn, int type);

  @Query("SELECT estab.caseId FROM GatewayCache estab WHERE estab.estabUprn = :estabUprn")
  String findByEstabUprn(@Param("estabUprn") String estabUprn);

  @Query("SELECT estab.caseId FROM GatewayCache estab WHERE estab.uprn = :uprn")
  String findByUprn(@Param("uprn") String uprn);

  @NonNull
  List<GatewayCache> findAll();

}
