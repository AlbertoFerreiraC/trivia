package com.fersal.trivia.common;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationalErrorRepository extends JpaRepository<OperationalError, Long> {
    Page<OperationalError> findAllByOrderByCreatedAtDesc(Pageable pageable);
}