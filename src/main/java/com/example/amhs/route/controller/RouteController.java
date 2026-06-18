package com.example.amhs.route.controller;

import com.example.amhs.route.dto.RouteResult;
import com.example.amhs.route.dto.RouteSearchStrategy;
import com.example.amhs.route.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/routes")
public class RouteController {

    private final RouteService routeService;

    @GetMapping("/bfs")
    public ResponseEntity<RouteResult> getBfsRoute(
            @RequestParam("source") String source,
            @RequestParam("destination") String destination
    ) {
        return ResponseEntity.ok(routeService.findRouteByBfs(source, destination));
    }

    @GetMapping("/dijkstra")
    public ResponseEntity<RouteResult> getDijkstraRoute(
            @RequestParam("source") String source,
            @RequestParam("destination") String destination,
            @RequestParam(name = "strategy", defaultValue = "TIME") RouteSearchStrategy strategy
    ) {
        return ResponseEntity.ok(routeService.findRouteByDijkstra(source, destination, strategy));
    }
}
