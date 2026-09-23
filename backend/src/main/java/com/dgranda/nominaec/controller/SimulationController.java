package com.dgranda.nominaec.controller;

import com.dgranda.nominaec.calculation.HiringCostSimulator;
import com.dgranda.nominaec.dto.SimulationDtos.HiringCostRequest;
import com.dgranda.nominaec.service.SimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/simulations")
@Tag(name = "Simulador")
public class SimulationController {

    private final SimulationService service;

    public SimulationController(SimulationService service) {
        this.service = service;
    }

    @PostMapping("/hiring-cost")
    @Operation(summary = "¿Cuánto cuesta contratar a alguien? Costo mensual y anual, primer año y desde el segundo")
    public HiringCostSimulator.Result hiringCost(@Valid @RequestBody HiringCostRequest request) {
        return service.hiringCost(request);
    }
}
