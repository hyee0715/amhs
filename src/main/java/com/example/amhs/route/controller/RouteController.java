package com.example.amhs.route.controller;

import com.example.amhs.route.dto.RouteResult;
import com.example.amhs.route.dto.RouteSearchStrategy;
import com.example.amhs.route.service.RouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/routes")
@Tag(name = "Route", description = "경로 탐색 API")
public class RouteController {

    private final RouteService routeService;

    @GetMapping("/bfs")
    @Operation(summary = "BFS 경로 탐색")
    public ResponseEntity<RouteResult> getBfsRoute(
            @RequestParam("source") String source,
            @RequestParam("destination") String destination
    ) {
        return ResponseEntity.ok(routeService.findRouteByBfs(source, destination));
    }

    @GetMapping("/dijkstra")
    @Operation(summary = "Dijkstra 경로 탐색", description = "strategy 파라미터로 TIME 또는 CONGESTION_AWARE 선택")
    public ResponseEntity<RouteResult> getDijkstraRoute(
            @RequestParam("source") String source,
            @RequestParam("destination") String destination,
            @RequestParam(name = "strategy", defaultValue = "TIME") RouteSearchStrategy strategy
    ) {
        return ResponseEntity.ok(routeService.findRouteByDijkstra(source, destination, strategy));
    }
}
