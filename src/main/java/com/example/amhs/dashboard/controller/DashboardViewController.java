package com.example.amhs.dashboard.controller;

import com.example.amhs.analytics.service.AnalyticsService;
import com.example.amhs.alert.service.AlertService;
import com.example.amhs.dashboard.service.DashboardService;
import com.example.amhs.edge.domain.EdgeStatus;
import com.example.amhs.edge.dto.EdgeCongestionUpdateRequest;
import com.example.amhs.edge.dto.EdgeCreateRequest;
import com.example.amhs.edge.dto.EdgeResponse;
import com.example.amhs.edge.dto.EdgeStatusUpdateRequest;
import com.example.amhs.edge.service.EdgeService;
import com.example.amhs.equipment.domain.EquipmentStatus;
import com.example.amhs.equipment.domain.EquipmentType;
import com.example.amhs.equipment.dto.EquipmentCreateRequest;
import com.example.amhs.equipment.dto.EquipmentStatusUpdateRequest;
import com.example.amhs.equipment.service.EquipmentService;
import com.example.amhs.node.domain.NodeStatus;
import com.example.amhs.node.domain.NodeType;
import com.example.amhs.node.dto.NodeCreateRequest;
import com.example.amhs.node.dto.NodeStatusUpdateRequest;
import com.example.amhs.node.service.NodeService;
import com.example.amhs.route.dto.RouteSearchStrategy;
import com.example.amhs.route.service.RouteService;
import com.example.amhs.transferjob.domain.TransferJobPriority;
import com.example.amhs.transferjob.domain.TransferJobStatus;
import com.example.amhs.transferjob.dto.TransferJobCreateRequest;
import com.example.amhs.transferjob.dto.TransferJobHistoryResponse;
import com.example.amhs.transferjob.dto.TransferJobStatusUpdateRequest;
import com.example.amhs.transferjob.service.TransferJobService;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j

@Controller
@RequiredArgsConstructor
public class DashboardViewController {

    private final DashboardService dashboardService;
    private final NodeService nodeService;
    private final EdgeService edgeService;
    private final EquipmentService equipmentService;
    private final TransferJobService transferJobService;
    private final AlertService alertService;
    private final RouteService routeService;
    private final AnalyticsService analyticsService;

    @GetMapping({"/", "/dashboard"})
    public String dashboard(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false, defaultValue = "TIME") RouteSearchStrategy strategy,
            @RequestParam(required = false) Long historyJobId,
            Model model
    ) {
        model.addAttribute("summary", dashboardService.getSummary());
        model.addAttribute("nodes", nodeService.getNodes());
        model.addAttribute("edges", edgeService.getEdges());
        model.addAttribute("equipments", equipmentService.getEquipments());
        model.addAttribute("transferJobs", transferJobService.getTransferJobs());
        model.addAttribute("dispatchCandidates", transferJobService.getDispatchCandidates());
        model.addAttribute("alerts", alertService.getAlerts());
        model.addAttribute("transferTimeOutliers", analyticsService.getTransferTimeOutliers());
        model.addAttribute("routeStabilities", analyticsService.getRouteStabilities());
        model.addAttribute("failurePareto", analyticsService.getFailurePareto());
        model.addAttribute("hourlyDelayTrends", analyticsService.getHourlyDelayTrends());
        model.addAttribute("nodeTypes", NodeType.values());
        model.addAttribute("nodeStatuses", NodeStatus.values());
        model.addAttribute("equipmentTypes", EquipmentType.values());
        model.addAttribute("equipmentStatuses", EquipmentStatus.values());
        model.addAttribute("jobPriorities", TransferJobPriority.values());
        model.addAttribute("jobStatuses", TransferJobStatus.values());
        model.addAttribute("edgeStatuses", EdgeStatus.values());
        model.addAttribute("routeStrategies", RouteSearchStrategy.values());
        model.addAttribute("selectedSource", source);
        model.addAttribute("selectedDestination", destination);
        model.addAttribute("selectedStrategy", strategy);
        model.addAttribute("selectedHistoryJobId", historyJobId);

        if (hasText(source) && hasText(destination)) {
            try {
                model.addAttribute("bfsRouteResult", routeService.findRouteByBfs(source, destination));
            } catch (Exception exception) {
                model.addAttribute("bfsRouteError", exception.getMessage());
            }

            try {
                model.addAttribute("dijkstraRouteResult", routeService.findRouteByDijkstra(source, destination, strategy));
            } catch (Exception exception) {
                model.addAttribute("dijkstraRouteError", exception.getMessage());
            }
        }

        if (historyJobId != null) {
            try {
                model.addAttribute("jobHistories", transferJobService.getTransferJobHistories(historyJobId));
            } catch (Exception exception) {
                model.addAttribute("historyError", exception.getMessage());
            }
        }
        return "dashboard";
    }

    @PostMapping("/nodes")
    public String createNode(
            @RequestParam String code,
            @RequestParam String name,
            @RequestParam NodeType type,
            RedirectAttributes redirectAttributes
    ) {
        return runAction(redirectAttributes, "Node created", () -> nodeService.createNode(new NodeCreateRequest(code, name, type)));
    }

    @PostMapping("/nodes/{id}/status")
    public String updateNodeStatus(
            @PathVariable Long id,
            @RequestParam NodeStatus status,
            RedirectAttributes redirectAttributes
    ) {
        return runAction(redirectAttributes, "Node status updated", () -> nodeService.updateNodeStatus(id, new NodeStatusUpdateRequest(status)));
    }

    @PostMapping("/equipments")
    public String createEquipment(
            @RequestParam String code,
            @RequestParam String name,
            @RequestParam EquipmentType type,
            RedirectAttributes redirectAttributes
    ) {
        return runAction(
                redirectAttributes,
                "Equipment created",
                () -> equipmentService.createEquipment(new EquipmentCreateRequest(code, name, type))
        );
    }

    @PostMapping("/equipments/{id}/status")
    public String updateEquipmentStatus(
            @PathVariable Long id,
            @RequestParam EquipmentStatus status,
            RedirectAttributes redirectAttributes
    ) {
        return runAction(
                redirectAttributes,
                "Equipment status updated",
                () -> equipmentService.updateEquipmentStatus(id, new EquipmentStatusUpdateRequest(status))
        );
    }

    @PostMapping("/edges")
    public String createEdge(
            @RequestParam String fromNodeCode,
            @RequestParam String toNodeCode,
            @RequestParam int distance,
            @RequestParam int estimatedTimeSeconds,
            @RequestParam int congestionLevel,
            @RequestParam(defaultValue = "false") boolean bidirectional,
            RedirectAttributes redirectAttributes
    ) {
        return runAction(
                redirectAttributes,
                "Edge created",
                () -> edgeService.createEdge(new EdgeCreateRequest(
                        fromNodeCode,
                        toNodeCode,
                        distance,
                        estimatedTimeSeconds,
                        congestionLevel,
                        bidirectional
                ))
        );
    }

    @PostMapping("/edges/{id}/status")
    public String updateEdgeStatus(
            @PathVariable Long id,
            @RequestParam EdgeStatus status,
            RedirectAttributes redirectAttributes
    ) {
        return runAction(redirectAttributes, "Edge status updated", () -> edgeService.updateEdgeStatus(id, new EdgeStatusUpdateRequest(status)));
    }

    @PostMapping("/edges/{id}/congestion")
    public String updateEdgeCongestion(
            @PathVariable Long id,
            @RequestParam int congestionLevel,
            RedirectAttributes redirectAttributes
    ) {
        return runAction(
                redirectAttributes,
                "Edge congestion updated",
                () -> edgeService.updateEdgeCongestion(id, new EdgeCongestionUpdateRequest(congestionLevel))
        );
    }

    @PostMapping("/transfer-jobs")
    public String createTransferJob(
            @RequestParam String carrierId,
            @RequestParam String sourceNodeCode,
            @RequestParam String destinationNodeCode,
            @RequestParam TransferJobPriority priority,
            RedirectAttributes redirectAttributes
    ) {
        return runAction(
                redirectAttributes,
                "Transfer job created",
                () -> transferJobService.createTransferJob(
                        new TransferJobCreateRequest(carrierId, sourceNodeCode, destinationNodeCode, priority)
                )
        );
    }

    @PostMapping("/transfer-jobs/{id}/status")
    public String updateTransferJobStatus(
            @PathVariable Long id,
            @RequestParam TransferJobStatus status,
            @RequestParam(required = false) String assignedEquipmentCode,
            @RequestParam(required = false) String reason,
            RedirectAttributes redirectAttributes
    ) {
        return runAction(
                redirectAttributes,
                "Transfer job status updated",
                () -> transferJobService.updateTransferJobStatus(
                        id,
                        new TransferJobStatusUpdateRequest(status, emptyToNull(reason), emptyToNull(assignedEquipmentCode))
                )
        );
    }

    @PostMapping("/transfer-jobs/{id}/assign")
    public String assignTransferJob(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return runAction(redirectAttributes, "Transfer job assigned", () -> transferJobService.assignTransferJob(id));
    }

    @PostMapping("/transfer-jobs/{id}/retry")
    public String retryTransferJob(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return runAction(redirectAttributes, "Transfer job retried", () -> transferJobService.retryTransferJob(id));
    }

    @PostMapping("/transfer-jobs/assign-pending")
    public String assignPendingTransferJobs(RedirectAttributes redirectAttributes) {
        return runAction(redirectAttributes, "Pending jobs assigned", transferJobService::assignPendingTransferJobs);
    }

    @PostMapping("/alerts/{id}/resolve")
    public String resolveAlert(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return runAction(redirectAttributes, "Alert resolved", () -> alertService.resolveAlert(id));
    }

    private String runAction(RedirectAttributes redirectAttributes, String successMessage, Action action) {
        try {
            action.run();
            redirectAttributes.addFlashAttribute("successMessage", successMessage);
        } catch (Exception exception) {
            log.debug("Dashboard action failed", exception);
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/dashboard";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String emptyToNull(String value) {
        return hasText(value) ? value : null;
    }

    @FunctionalInterface
    private interface Action {
        void run();
    }
}
