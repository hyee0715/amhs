package com.example.amhs.transferjob.repository;

import com.example.amhs.transferjob.domain.TransferJobHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferJobHistoryRepository extends JpaRepository<TransferJobHistory, Long> {

    List<TransferJobHistory> findByTransferJobIdOrderByCreatedAtAsc(Long transferJobId);
}
