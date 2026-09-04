package com.agentguard.repository;
import com.agentguard.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;
import com.agentguard.entity.TransactionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Transaction t where t.id = :id")
    Optional<Transaction> findByIdForUpdate(@Param("id") Long id);
    List<Transaction> findByUserIdOrderByCreatedAtDesc(Long userId);
    @Query("select coalesce(sum(coalesce(t.actualAmount, t.requestedAmount)), 0) from Transaction t where t.userId = :userId and t.createdAt >= :since and t.status in :statuses")
    BigDecimal sumSpendingSince(@Param("userId") Long userId, @Param("since") Instant since, @Param("statuses") List<TransactionStatus> statuses);
    boolean existsByUserIdAndMerchantIgnoreCaseAndCategoryIgnoreCaseAndRequestedAmountAndCreatedAtAfter(Long userId, String merchant, String category, BigDecimal requestedAmount, Instant since);
}
