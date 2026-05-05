package com.geojit.contractnote.repository;

import com.geojit.contractnote.entity.LambdaException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LambdaExceptionRepository extends JpaRepository<LambdaException, Long> {

    List<LambdaException> findByJobIdOrderByOccurredAtDesc(String jobId);

    List<LambdaException> findByJobIdAndLambdaNameOrderByOccurredAtDesc(String jobId, String lambdaName);

    List<LambdaException> findByLambdaNameOrderByOccurredAtDesc(String lambdaName);

    List<LambdaException> findTop100ByOrderByOccurredAtDesc();
}
