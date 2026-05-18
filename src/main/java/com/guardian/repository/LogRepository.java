package com.guardian.repository;

import com.guardian.entity.LogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import java.util.List;

@Repository
public interface LogRepository extends JpaRepository<LogEntry, Long> {
    List<LogEntry> findByTargetId(String targetId);
    
    @Query("SELECT DISTINCT l.targetId FROM LogEntry l WHERE l.targetId IS NOT NULL")
    List<String> findDistinctTargetIds();
}
