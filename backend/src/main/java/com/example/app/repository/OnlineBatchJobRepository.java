package com.example.app.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.app.model.entity.OnlineBatchJob;

@Mapper
public interface OnlineBatchJobRepository {

    void failIncomplete(@Param("id") Long id, @Param("events") String events);

    Optional<OnlineBatchJob> findById(@Param("id") Long id);

    List<OnlineBatchJob> findAll(@Param("offset") long offset, @Param("limit") int limit);

    long count();

    void pruneHistory(@Param("cutoff") LocalDateTime cutoff, @Param("maxRows") int maxRows);

    void insert(OnlineBatchJob job);

    void update(OnlineBatchJob job);
}
