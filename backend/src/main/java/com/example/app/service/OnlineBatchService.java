package com.example.app.service;

import java.util.List;

import com.example.app.model.dto.OnlineBatchJobResponse;
import com.example.app.model.dto.StartOnlineBatchRequest;

public interface OnlineBatchService {

    OnlineBatchJobResponse start(StartOnlineBatchRequest request);

    List<OnlineBatchJobResponse> findAll();

    OnlineBatchJobResponse findById(Long id);
}
