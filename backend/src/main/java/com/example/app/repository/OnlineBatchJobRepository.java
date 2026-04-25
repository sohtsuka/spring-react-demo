package com.example.app.repository;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.app.model.entity.OnlineBatchJob;

@Mapper
public interface OnlineBatchJobRepository {

    Optional<OnlineBatchJob> findById(@Param("id") Long id);

    List<OnlineBatchJob> findAll();

    void insert(OnlineBatchJob job);

    void update(OnlineBatchJob job);
}
