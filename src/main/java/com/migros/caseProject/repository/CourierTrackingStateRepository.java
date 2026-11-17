package com.migros.caseProject.repository;

import com.migros.caseProject.model.CourierTrackingState;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CourierTrackingStateRepository extends JpaRepository<CourierTrackingState, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select state from CourierTrackingState state where state.courierId = :courierId")
    Optional<CourierTrackingState> findByCourierIdForUpdate(@Param("courierId") String courierId);
}
