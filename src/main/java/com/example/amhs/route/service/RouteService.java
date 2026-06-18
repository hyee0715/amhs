package com.example.amhs.route.service;

import com.example.amhs.common.exception.BusinessException;
import com.example.amhs.common.exception.ErrorCode;
import com.example.amhs.edge.domain.AmhsEdge;
import com.example.amhs.edge.domain.EdgeStatus;
import com.example.amhs.edge.repository.EdgeRepository;
import com.example.amhs.node.domain.AmhsNode;
import com.example.amhs.node.domain.NodeStatus;
import com.example.amhs.node.repository.NodeRepository;
import com.example.amhs.route.dto.RouteResult;
import com.example.amhs.route.dto.RouteSearchStrategy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RouteService {

    private final NodeRepository nodeRepository;
    private final EdgeRepository edgeRepository;

    public RouteResult findRouteByBfs(String sourceCode, String destinationCode) {
        AmhsNode sourceNode = findNodeByCode(sourceCode);
        AmhsNode destinationNode = findNodeByCode(destinationCode);

        Map<String, List<RouteEdge>> graph = buildAvailableGraph();
        validateRouteEndpoints(sourceNode, destinationNode, graph);

        Queue<String> queue = new ArrayDeque<>();
        Map<String, String> previous = new HashMap<>();
        Set<String> visited = new HashSet<>();

        queue.add(sourceCode);
        visited.add(sourceCode);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (current.equals(destinationCode)) {
                return createRouteResult(
                        sourceCode,
                        destinationCode,
                        "BFS",
                        previous,
                        graph,
                        RouteSearchStrategy.TIME
                );
            }

            for (RouteEdge next : graph.getOrDefault(current, List.of())) {
                if (visited.add(next.toCode())) {
                    previous.put(next.toCode(), current);
                    queue.add(next.toCode());
                }
            }
        }

        throw routeNotFound(sourceCode, destinationCode);
    }

    public RouteResult findRouteByDijkstra(String sourceCode, String destinationCode) {
        return findRouteByDijkstra(sourceCode, destinationCode, RouteSearchStrategy.TIME);
    }

    public RouteResult findRouteByDijkstra(
            String sourceCode,
            String destinationCode,
            RouteSearchStrategy strategy
    ) {
        AmhsNode sourceNode = findNodeByCode(sourceCode);
        AmhsNode destinationNode = findNodeByCode(destinationCode);

        Map<String, List<RouteEdge>> graph = buildAvailableGraph();
        validateRouteEndpoints(sourceNode, destinationNode, graph);

        Map<String, Integer> timeDistance = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        PriorityQueue<RouteNode> priorityQueue = new PriorityQueue<>(Comparator.comparingInt(RouteNode::cost));

        initializeDistances(graph.keySet(), timeDistance);
        timeDistance.put(sourceCode, 0);
        priorityQueue.add(new RouteNode(sourceCode, 0));

        while (!priorityQueue.isEmpty()) {
            RouteNode current = priorityQueue.poll();

            if (current.cost() > timeDistance.getOrDefault(current.code(), Integer.MAX_VALUE)) {
                continue;
            }

            if (current.code().equals(destinationCode)) {
                return createRouteResult(sourceCode, destinationCode, "DIJKSTRA", previous, graph, strategy);
            }

            for (RouteEdge next : graph.getOrDefault(current.code(), List.of())) {
                int nextCost = current.cost() + calculateWeight(next, strategy);
                if (nextCost < timeDistance.getOrDefault(next.toCode(), Integer.MAX_VALUE)) {
                    timeDistance.put(next.toCode(), nextCost);
                    previous.put(next.toCode(), current.code());
                    priorityQueue.add(new RouteNode(next.toCode(), nextCost));
                }
            }
        }

        throw routeNotFound(sourceCode, destinationCode);
    }

    private RouteResult createRouteResult(
            String sourceCode,
            String destinationCode,
            String algorithm,
            Map<String, String> previous,
            Map<String, List<RouteEdge>> graph,
            RouteSearchStrategy strategy
    ) {
        List<String> path = reconstructPath(sourceCode, destinationCode, previous);
        int totalDistance = 0;
        int totalEstimatedTimeSeconds = 0;

        for (int i = 0; i < path.size() - 1; i++) {
            RouteEdge edge = findRouteEdge(graph.getOrDefault(path.get(i), List.of()), path.get(i + 1));
            totalDistance += edge.distance();
            totalEstimatedTimeSeconds += edge.estimatedTimeSeconds();
        }

        return RouteResult.of(
                sourceCode,
                destinationCode,
                algorithm + (algorithm.equals("DIJKSTRA") ? "_" + strategy.name() : ""),
                path,
                totalEstimatedTimeSeconds,
                totalDistance
        );
    }

    private RouteEdge findRouteEdge(List<RouteEdge> edges, String toCode) {
        return edges.stream()
                .filter(edge -> edge.toCode().equals(toCode))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Route edge not found"));
    }

    private List<String> reconstructPath(String sourceCode, String destinationCode, Map<String, String> previous) {
        List<String> path = new ArrayList<>();
        String current = destinationCode;
        path.add(current);

        while (!current.equals(sourceCode)) {
            current = previous.get(current);
            if (current == null) {
                throw routeNotFound(sourceCode, destinationCode);
            }
            path.add(0, current);
        }

        return path;
    }

    private Map<String, List<RouteEdge>> buildAvailableGraph() {
        Map<String, List<RouteEdge>> graph = new HashMap<>();
        Set<String> availableNodeCodes = loadAvailableNodeCodes();

        for (String nodeCode : availableNodeCodes) {
            graph.put(nodeCode, new ArrayList<>());
        }

        for (AmhsEdge edge : edgeRepository.findAll()) {
            String fromCode = edge.getFromNode().getCode();
            String toCode = edge.getToNode().getCode();

            if (edge.getStatus() != EdgeStatus.AVAILABLE) {
                continue;
            }
            if (!availableNodeCodes.contains(fromCode) || !availableNodeCodes.contains(toCode)) {
                continue;
            }

            graph.get(fromCode).add(
                    new RouteEdge(toCode, edge.getDistance(), edge.getEstimatedTimeSeconds(), edge.getCongestionLevel())
            );
        }

        return graph;
    }

    private Set<String> loadAvailableNodeCodes() {
        return nodeRepository.findAll()
                .stream()
                .filter(node -> node.getStatus() == NodeStatus.AVAILABLE)
                .map(AmhsNode::getCode)
                .collect(Collectors.toSet());
    }

    private void validateRouteEndpoints(
            AmhsNode sourceNode,
            AmhsNode destinationNode,
            Map<String, List<RouteEdge>> graph
    ) {
        if (sourceNode.getStatus() != NodeStatus.AVAILABLE || destinationNode.getStatus() != NodeStatus.AVAILABLE) {
            throw routeNotFound(sourceNode.getCode(), destinationNode.getCode());
        }

        if (!graph.containsKey(sourceNode.getCode()) || !graph.containsKey(destinationNode.getCode())) {
            throw routeNotFound(sourceNode.getCode(), destinationNode.getCode());
        }
    }

    private void initializeDistances(Collection<String> nodeCodes, Map<String, Integer> timeDistance) {
        for (String nodeCode : nodeCodes) {
            timeDistance.put(nodeCode, Integer.MAX_VALUE);
        }
    }

    private int calculateWeight(RouteEdge edge, RouteSearchStrategy strategy) {
        return switch (strategy) {
            case TIME -> edge.estimatedTimeSeconds();
            case CONGESTION_AWARE -> edge.estimatedTimeSeconds() + edge.congestionLevel();
        };
    }

    private AmhsNode findNodeByCode(String code) {
        return nodeRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NODE_NOT_FOUND,
                        "Node not found: " + code
                ));
    }

    private BusinessException routeNotFound(String sourceCode, String destinationCode) {
        return new BusinessException(
                ErrorCode.ROUTE_NOT_FOUND,
                "Route not found from " + sourceCode + " to " + destinationCode
        );
    }

    private record RouteEdge(
            String toCode,
            int distance,
            int estimatedTimeSeconds,
            int congestionLevel
    ) {
    }

    private record RouteNode(
            String code,
            int cost
    ) {
    }
}
