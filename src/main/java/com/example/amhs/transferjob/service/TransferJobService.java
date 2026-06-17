package com.example.amhs.transferjob.service;

import com.example.amhs.common.exception.BusinessException;
import com.example.amhs.common.exception.ErrorCode;
import com.example.amhs.equipment.domain.Equipment;
import com.example.amhs.equipment.repository.EquipmentRepository;
import com.example.amhs.node.domain.AmhsNode;
import com.example.amhs.node.repository.NodeRepository;
import com.example.amhs.route.dto.RouteResult;
import com.example.amhs.route.service.RouteService;
import com.example.amhs.transferjob.domain.TransferJob;
import com.example.amhs.transferjob.domain.TransferJobHistory;
import com.example.amhs.transferjob.domain.TransferJobStatus;
import com.example.amhs.transferjob.dto.TransferJobCreateRequest;
import com.example.amhs.transferjob.dto.TransferJobHistoryResponse;
import com.example.amhs.transferjob.dto.TransferJobResponse;
import com.example.amhs.transferjob.dto.TransferJobStatusUpdateRequest;
import com.example.amhs.transferjob.repository.TransferJobHistoryRepository;
import com.example.amhs.transferjob.repository.TransferJobRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransferJobService {

    private final TransferJobRepository transferJobRepository;
    private final TransferJobHistoryRepository transferJobHistoryRepository;
    private final NodeRepository nodeRepository;
    private final EquipmentRepository equipmentRepository;
    private final RouteService routeService;
    private final ObjectMapper objectMapper;

    @Transactional
    public TransferJobResponse createTransferJob(TransferJobCreateRequest request) {
        AmhsNode sourceNode = findNodeByCode(request.sourceNodeCode());
        AmhsNode destinationNode = findNodeByCode(request.destinationNodeCode());
        RouteResult routeResult = routeService.findRouteByDijkstra(
                request.sourceNodeCode(),
                request.destinationNodeCode()
        );

        TransferJob transferJob = transferJobRepository.save(
                TransferJob.create(
                        request.carrierId(),
                        sourceNode,
                        destinationNode,
                        request.priority(),
                        toJson(routeResult.path()),
                        routeResult.totalEstimatedTimeSeconds()
                )
        );

        saveHistory(transferJob, TransferJobStatus.CREATED, null, null);
        return toResponse(transferJob);
    }

    public List<TransferJobResponse> getTransferJobs() {
        return transferJobRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TransferJobResponse getTransferJob(Long id) {
        return toResponse(findTransferJobById(id));
    }

    @Transactional
    public TransferJobResponse updateTransferJobStatus(Long id, TransferJobStatusUpdateRequest request) {
        validateStatusRequest(request);

        TransferJob transferJob = findTransferJobById(id);
        Equipment equipment = findEquipmentIfPresent(request.assignedEquipmentCode());

        transferJob.updateStatus(request.status(), equipment, request.reason());
        saveHistory(transferJob, request.status(), request.reason(), request.assignedEquipmentCode());

        return toResponse(transferJob);
    }

    public List<TransferJobHistoryResponse> getTransferJobHistories(Long id) {
        findTransferJobById(id);
        return transferJobHistoryRepository.findByTransferJobIdOrderByCreatedAtAsc(id)
                .stream()
                .map(history -> TransferJobHistoryResponse.from(history, fromJson(history.getPathSnapshot())))
                .toList();
    }

    private void validateStatusRequest(TransferJobStatusUpdateRequest request) {
        if (request.status() == TransferJobStatus.FAILED
                && (request.reason() == null || request.reason().isBlank())) {
            throw new BusinessException(
                    ErrorCode.INVALID_JOB_STATUS,
                    "Failure reason is required when status is FAILED"
            );
        }
    }

    private void saveHistory(
            TransferJob transferJob,
            TransferJobStatus status,
            String reason,
            String assignedEquipmentCode
    ) {
        transferJobHistoryRepository.save(
                TransferJobHistory.create(
                        transferJob,
                        status,
                        reason,
                        assignedEquipmentCode,
                        transferJob.getPath()
                )
        );
    }

    private TransferJobResponse toResponse(TransferJob transferJob) {
        return TransferJobResponse.from(transferJob, fromJson(transferJob.getPath()));
    }

    private String toJson(List<String> path) {
        try {
            return objectMapper.writeValueAsString(path);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize path", exception);
        }
    }

    private List<String> fromJson(String path) {
        if (path == null || path.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(path, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize path", exception);
        }
    }

    private AmhsNode findNodeByCode(String code) {
        return nodeRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NODE_NOT_FOUND,
                        "Node not found: " + code
                ));
    }

    private Equipment findEquipmentIfPresent(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }

        return equipmentRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.EQUIPMENT_NOT_FOUND,
                        "Equipment not found: " + code
                ));
    }

    private TransferJob findTransferJobById(Long id) {
        return transferJobRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.TRANSFER_JOB_NOT_FOUND,
                        "Transfer job not found: " + id
                ));
    }
}
