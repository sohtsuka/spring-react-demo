package com.example.app.service;

import com.example.app.model.dto.OnlineBatchJobResponse;
import com.example.app.model.dto.PagedResponse;
import com.example.app.model.dto.StartOnlineBatchRequest;

public interface OnlineBatchService {

    OnlineBatchJobResponse start(long userId, StartOnlineBatchRequest request);

    PagedResponse<OnlineBatchJobResponse> findAll(int page, int size);

    OnlineBatchJobResponse findById(Long id);
}
