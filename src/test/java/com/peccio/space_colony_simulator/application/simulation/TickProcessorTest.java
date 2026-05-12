package com.peccio.space_colony_simulator.application.simulation;

import com.peccio.space_colony_simulator.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class TickProcessorTest {

    private TickProcessor tickProcessor;
    private LocalDateTime simTime;

    @BeforeEach
    void setUp() {
        tickProcessor = new TickProcessor();
        simTime = LocalDateTime.of(2350, 1, 1, 0, 0);
    }

    @Test
    @DisplayName("should decrease resource when consumption exceeds production")
    void process_consumptionExceedsProduction_amountDecreases() {
        Colony colony = colonyWith(resource(ResourceType.FOOD, 500, 3, 8, 1000));

        tickProcessor.process(colony, simTime);

        BigDecimal food = resourceAmount(colony, ResourceType.FOOD);
        assertThat(food).isEqualByComparingTo("495.00");
    }

    @Test
    @DisplayName("should clamp resource at zero — never go negative")
    void process_massiveConsumption_amountNeverBelowZero() {
        Colony colony = colonyWith(resource(ResourceType.FOOD, 5, 0, 100, 1000));

        tickProcessor.process(colony, simTime);

        BigDecimal food = resourceAmount(colony, ResourceType.FOOD);
        assertThat(food).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("should clamp resource at storageCapacity — never overflow")
    void process_massiveProduction_amountNeverExceedsCapacity() {
        Colony colony = colonyWith(resource(ResourceType.ENERGY, 995, 50, 0, 1000));

        tickProcessor.process(colony, simTime);

        BigDecimal energy = resourceAmount(colony, ResourceType.ENERGY);
        assertThat(energy).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("should tick each resource independently")
    void process_multipleResources_eachTickedIndependently() {
        Colony colony = colonyWith(
                resource(ResourceType.OXYGEN,    500, 10, 8, 1000),  // net +2
                resource(ResourceType.FOOD,      300, 5,  3, 1000),  // net +2
                resource(ResourceType.ENERGY,    200, 0, 10, 1000),  // net -10
                resource(ResourceType.MATERIALS, 100, 4,  4, 1000)   // net  0
        );

        tickProcessor.process(colony, simTime);

        assertThat(resourceAmount(colony, ResourceType.OXYGEN))
                .isEqualByComparingTo("502.00");
        assertThat(resourceAmount(colony, ResourceType.FOOD))
                .isEqualByComparingTo("302.00");
        assertThat(resourceAmount(colony, ResourceType.ENERGY))
                .isEqualByComparingTo("190.00");
        assertThat(resourceAmount(colony, ResourceType.MATERIALS))
                .isEqualByComparingTo("100.00");
    }

    // -------------------------------------------------------
    // Event generation
    // -------------------------------------------------------

    @Test
    @DisplayName("should generate no events when all resources are healthy")
    void process_allResourcesHealthy_noEventsGenerated() {
        Colony colony = colonyWith(
                resource(ResourceType.OXYGEN,    500, 10, 8, 1000),
                resource(ResourceType.FOOD,      500, 10, 8, 1000),
                resource(ResourceType.ENERGY,    500, 10, 8, 1000),
                resource(ResourceType.MATERIALS, 500, 10, 8, 1000)
        );

        TickResult result = tickProcessor.process(colony, simTime);

        assertThat(result.generatedEvents()).isEmpty();
    }

    @Test
    @DisplayName("should generate HIGH severity event when resource hits zero")
    void process_resourceDepleted_generatesHighSeverityEvent() {
        Colony colony = colonyWith(resource(ResourceType.OXYGEN, 5, 0, 10, 1000));

        TickResult result = tickProcessor.process(colony, simTime);

        assertThat(result.generatedEvents()).hasSize(1);

        ColonyEvent event = result.generatedEvents().get(0);
        assertThat(event.getEventType()).isEqualTo(EventType.OXYGEN_SHORTAGE);
        assertThat(event.getSeverity()).isEqualTo(EventSeverity.HIGH);
        assertThat(event.getColonyId()).isEqualTo(1L);
        assertThat(event.getSimOccurredAt()).isEqualTo(simTime);
        assertThat(event.isResolved()).isFalse();
    }

    @Test
    @DisplayName("should generate LOW severity event when resource is critically low")
    void process_resourceCriticallyLow_generatesLowSeverityEvent() {
        // after tick: 95 - 5 = 90, which is 9% of 1000 → critically low
        Colony colony = colonyWith(resource(ResourceType.FOOD, 95, 0, 5, 1000));

        TickResult result = tickProcessor.process(colony, simTime);

        assertThat(result.generatedEvents()).hasSize(1);

        ColonyEvent event = result.generatedEvents().get(0);
        assertThat(event.getEventType()).isEqualTo(EventType.FOOD_SHORTAGE);
        assertThat(event.getSeverity()).isEqualTo(EventSeverity.LOW);
    }

    @Test
    @DisplayName("should map each ResourceType to its correct EventType")
    void process_eachResourceType_mapsToCorrectEventType() {
        Colony colony = colonyWith(
                resource(ResourceType.OXYGEN,    5, 0, 10, 1000),
                resource(ResourceType.FOOD,      5, 0, 10, 1000),
                resource(ResourceType.ENERGY,    5, 0, 10, 1000),
                resource(ResourceType.MATERIALS, 5, 0, 10, 1000)
        );

        TickResult result = tickProcessor.process(colony, simTime);

        assertThat(result.generatedEvents())
                .extracting(ColonyEvent::getEventType)
                .containsExactlyInAnyOrder(
                        EventType.OXYGEN_SHORTAGE,
                        EventType.FOOD_SHORTAGE,
                        EventType.ENERGY_FAILURE,
                        EventType.MATERIAL_SHORTAGE
                );
    }

    @Test
    @DisplayName("should stamp event with the correct simulation time")
    void process_eventGenerated_simTimeIsCorrect() {
        LocalDateTime specificTime = LocalDateTime.of(2350, 6, 15, 12, 30);
        Colony colony = colonyWith(resource(ResourceType.OXYGEN, 0, 0, 0, 1000));

        TickResult result = tickProcessor.process(colony, specificTime);

        assertThat(result.generatedEvents().get(0).getSimOccurredAt())
                .isEqualTo(specificTime);
    }

    @Test
    @DisplayName("should record colonyId on generated events")
    void process_eventGenerated_colonyIdIsSet() {
        Colony colony = colonyWith(resource(ResourceType.OXYGEN, 0, 0, 0, 1000));

        TickResult result = tickProcessor.process(colony, simTime);

        assertThat(result.generatedEvents().get(0).getColonyId())
                .isEqualTo(colony.getId());
    }

    // -------------------------------------------------------
    // TickResult
    // -------------------------------------------------------

    @Test
    @DisplayName("should return correct colonyId in TickResult")
    void process_result_containsCorrectColonyId() {
        Colony colony = colonyWith(resource(ResourceType.OXYGEN, 500, 10, 8, 1000));

        TickResult result = tickProcessor.process(colony, simTime);

        assertThat(result.colonyId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("hasEvents() should return false when no events generated")
    void tickResult_hasEvents_falseWhenEmpty() {
        Colony colony = colonyWith(resource(ResourceType.OXYGEN, 500, 10, 8, 1000));

        TickResult result = tickProcessor.process(colony, simTime);

        assertThat(result.hasEvent()).isFalse();
    }

    @Test
    @DisplayName("hasEvents() should return true when events are generated")
    void tickResult_hasEvents_trueWhenEventsExist() {
        Colony colony = colonyWith(resource(ResourceType.OXYGEN, 0, 0, 0, 1000));

        TickResult result = tickProcessor.process(colony, simTime);

        assertThat(result.hasEvent()).isTrue();
    }

    // -------------------------------------------------------
    // Test builders
    // -------------------------------------------------------

    private Colony colonyWith(ColonyResource... resources) {
        return Colony.builder()
                .id(1L)
                .name("Test Colony")
                .ownerId("test-owner")
                .population(10)
                .status(ColonyStatus.ACTIVE)
                .foundedAt(LocalDateTime.now())
                .lastTickAt(simTime)
                .resources(new ArrayList<>(Arrays.asList(resources)))
                .build();
    }

    private ColonyResource resource(
            ResourceType type,
            double current,
            double production,
            double consumption,
            double capacity) {

        return ColonyResource.builder()
                .id(1L)
                .colonyId(1L)
                .resourceType(type)
                .currentAmount(BigDecimal.valueOf(current))
                .productionRate(BigDecimal.valueOf(production))
                .consumptionRate(BigDecimal.valueOf(consumption))
                .storageCapacity(BigDecimal.valueOf(capacity))
                .build();
    }

    private BigDecimal resourceAmount(Colony colony, ResourceType type) {
        return colony.getResource(type)
                .orElseThrow()
                .getCurrentAmount();
    }
}