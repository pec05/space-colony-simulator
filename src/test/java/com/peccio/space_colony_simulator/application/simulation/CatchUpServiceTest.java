package com.peccio.space_colony_simulator.application.simulation;

import com.peccio.space_colony_simulator.domain.model.*;
import com.peccio.space_colony_simulator.infrastructure.config.SimulationProperties;
import com.peccio.space_colony_simulator.infrastructure.persistence.entity.ColonyEntity;
import com.peccio.space_colony_simulator.infrastructure.persistence.entity.ColonyResourceEntity;
import com.peccio.space_colony_simulator.infrastructure.persistence.mapper.ColonyMapper;
import com.peccio.space_colony_simulator.infrastructure.persistence.repository.ColonyEventRepository;
import com.peccio.space_colony_simulator.infrastructure.persistence.repository.ColonyRepository;
import com.peccio.space_colony_simulator.infrastructure.persistence.repository.ColonyResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CatchUpService")
class CatchUpServiceTest {
    @Mock
    private ColonyRepository colonyRepository;
    @Mock private ColonyResourceRepository resourceRepository;
    @Mock private ColonyEventRepository eventRepository;
    @Mock private ColonyMapper colonyMapper;
    @Mock private TickProcessor            tickProcessor;

    private SimulationProperties simulationProperties;
    private CatchUpService       catchUpService;

    @BeforeEach
    void setUp() {
        simulationProperties = new SimulationProperties();
        simulationProperties.setRateMs(10_000);      // 10 seconds per tick
        simulationProperties.setSimHoursPerTick(1);

        catchUpService = new CatchUpService(
                colonyRepository,
                resourceRepository,
                eventRepository,
                colonyMapper,
                tickProcessor,
                simulationProperties
        );
    }

    // -------------------------------------------------------
    // Tick count calculation
    // -------------------------------------------------------

    @Test
    @DisplayName("should process exactly 1 tick when called on schedule")
    void catchUp_onSchedule_processesOneTick() {
        // lastProcessedAt is 10 seconds ago → exactly 1 tick missed
        ColonyEntity entity = colonyEntity(LocalDateTime.now().minusSeconds(10));
        Colony       colony = domainColony();

        stubMocks(entity, colony);

        CatchUpResult result = catchUpService.catchUp(entity.getId());
        when(colonyRepository.findById(anyLong())).thenReturn(Optional.of(entity));
        assertThat(result.ticksProcessed()).isEqualTo(1);
    }

    @Test
    @DisplayName("should process 3 ticks when offline for 3 tick periods")
    void catchUp_offlineFor3Ticks_processes3Ticks() {
        // 30 seconds offline, rate = 10s → 3 ticks
        ColonyEntity entity = colonyEntity(LocalDateTime.now().minusSeconds(30));
        Colony       colony = domainColony();

        stubMocks(entity, colony);

        CatchUpResult result = catchUpService.catchUp(entity.getId());
        when(colonyRepository.findById(anyLong())).thenReturn(Optional.of(entity));
        assertThat(result.ticksProcessed()).isEqualTo(3);
        verify(tickProcessor, times(3)).process(any(), any());
    }

    @Test
    @DisplayName("should process minimum 1 tick even if called too early")
    void catchUp_calledTooEarly_stillProcessesOneTick() {
        // lastProcessedAt is 1 second ago — less than one tick period
        ColonyEntity entity = colonyEntity(LocalDateTime.now().minusSeconds(1));
        Colony       colony = domainColony();

        stubMocks(entity, colony);

        CatchUpResult result = catchUpService.catchUp(entity.getId());
        when(colonyRepository.findById(anyLong())).thenReturn(Optional.of(entity));
        assertThat(result.ticksProcessed()).isEqualTo(1);
    }

    @Test
    @DisplayName("should cap ticks at MAX_CATCH_UP_TICKS when offline very long")
    void catchUp_offlineVeryLong_capsAtMaxTicks() {
        // 10 hours offline with 10s rate = 3600 ticks → capped at 100
        ColonyEntity entity = colonyEntity(LocalDateTime.now().minusHours(10));
        Colony       colony = domainColony();

        stubMocks(entity, colony);

        CatchUpResult result = catchUpService.catchUp(entity.getId());
        when(colonyRepository.findById(anyLong())).thenReturn(Optional.of(entity));

        assertThat(result.ticksProcessed()).isEqualTo(CatchUpService.MAX_CATCH_UP_TICKS);
        verify(tickProcessor, times(CatchUpService.MAX_CATCH_UP_TICKS)).process(any(), any());
    }

    // -------------------------------------------------------
    // CatchUpResult content
    // -------------------------------------------------------

    @Test
    @DisplayName("hadMissedTicks() should be false for a normal single tick")
    void catchUp_singleTick_hadMissedTicksIsFalse() {
        ColonyEntity entity = colonyEntity(LocalDateTime.now().minusSeconds(10));
        Colony       colony = domainColony();

        stubMocks(entity, colony);

        CatchUpResult result = catchUpService.catchUp(entity.getId());
        when(colonyRepository.findById(anyLong())).thenReturn(Optional.of(entity));
        assertThat(result.hadMissedTicks()).isFalse();
    }

    @Test
    @DisplayName("hadMissedTicks() should be true when more than 1 tick was processed")
    void catchUp_multipleTicks_hadMissedTicksIsTrue() {
        ColonyEntity entity = colonyEntity(LocalDateTime.now().minusSeconds(30));
        Colony       colony = domainColony();

        stubMocks(entity, colony);

        CatchUpResult result = catchUpService.catchUp(entity.getId());
        when(colonyRepository.findById(anyLong())).thenReturn(Optional.of(entity));
        assertThat(result.hadMissedTicks()).isTrue();
    }

    @Test
    @DisplayName("should collect events from all ticks into allEvents")
    void catchUp_eventsAcrossMultipleTicks_allCollected() {
        ColonyEntity entity = colonyEntity(LocalDateTime.now().minusSeconds(30));
        Colony       colony = domainColony();

        stubMocks(entity, colony);

        // Each of the 3 ticks returns 1 event
        ColonyEvent event = stubEvent();
        when(tickProcessor.process(any(), any()))
                .thenReturn(new TickResult(1L, List.of(event)))
                .thenReturn(new TickResult(1L, List.of(event)))
                .thenReturn(new TickResult(1L, List.of(event)));

        // Reset the existing unresolved events stub to empty
        when(eventRepository.findAllByColonyIdAndResolved(anyLong(), eq(false)))
                .thenReturn(List.of());

        CatchUpResult result = catchUpService.catchUp(entity.getId());

        assertThat(result.allEvents()).hasSize(3);
    }

    @Test
    @DisplayName("should return correct colonyId and colonyName in result")
    void catchUp_result_containsColonyMetadata() {
        ColonyEntity entity = colonyEntity(LocalDateTime.now().minusSeconds(10));
        Colony       colony = domainColony();

        stubMocks(entity, colony);

        CatchUpResult result = catchUpService.catchUp(entity.getId());
        when(colonyRepository.findById(anyLong())).thenReturn(Optional.of(entity));
        assertThat(result.colonyId()).isEqualTo(1L);
        assertThat(result.colonyName()).isEqualTo("Test Colony");
    }

    // -------------------------------------------------------
    // Persistence interactions
    // -------------------------------------------------------

    @Test
    @DisplayName("should save the colony entity after processing")
    void catchUp_always_savesColonyEntity() {
        ColonyEntity entity = colonyEntity(LocalDateTime.now().minusSeconds(10));
        Colony       colony = domainColony();

        stubMocks(entity, colony);

        catchUpService.catchUp(entity.getId());
        when(colonyRepository.findById(anyLong())).thenReturn(Optional.of(entity));
        verify(colonyRepository, times(1)).save(entity);
    }

    @Test
    @DisplayName("should update lastProcessedAt on the entity after processing")
    void catchUp_always_updatesLastProcessedAt() {
        ColonyEntity entity = colonyEntity(LocalDateTime.now().minusSeconds(10));
        Colony       colony = domainColony();

        stubMocks(entity, colony);

        LocalDateTime before = LocalDateTime.now();
        catchUpService.catchUp(entity.getId());
        LocalDateTime after  = LocalDateTime.now();
        when(colonyRepository.findById(anyLong())).thenReturn(Optional.of(entity));
        assertThat(entity.getLastProcessedAt())
                .isAfterOrEqualTo(before)
                .isBeforeOrEqualTo(after);
    }

    // -------------------------------------------------------
    // Stubs and builders
    // -------------------------------------------------------

    private void stubMocks(ColonyEntity entity, Colony colony) {
        when(colonyMapper.toDomain(entity)).thenReturn(colony);
        when(tickProcessor.process(any(), any()))
                .thenReturn(new TickResult(1L, List.of()));
        when(resourceRepository.findByColonyIdAndResourceType(anyLong(), anyString()))
                .thenReturn(Optional.of(new ColonyResourceEntity()));
        when(eventRepository.findAllByColonyIdAndResolved(anyLong(), anyBoolean()))
                .thenReturn(List.of());
        when(colonyRepository.save(any())).thenReturn(entity);
    }

    private ColonyEntity colonyEntity(LocalDateTime lastProcessedAt) {
        ColonyEntity entity = new ColonyEntity();
        entity.setId(1L);
        entity.setName("Test Colony");
        entity.setStatus("ACTIVE");
        entity.setLastTickAt(LocalDateTime.of(2350, 1, 1, 0, 0));
        entity.setLastProcessedAt(lastProcessedAt);
        entity.setResources(new ArrayList<>());
        return entity;
    }

    private Colony domainColony() {
        return Colony.builder()
                .id(1L)
                .name("Test Colony")
                .ownerId("test-owner")
                .population(10)
                .status(ColonyStatus.ACTIVE)
                .foundedAt(LocalDateTime.now())
                .lastTickAt(LocalDateTime.of(2350, 1, 1, 0, 0))
                .resources(new ArrayList<>())
                .build();
    }

    private ColonyEvent stubEvent() {
        return ColonyEvent.builder()
                .colonyId(1L)
                .eventType(EventType.OXYGEN_SHORTAGE)
                .severity(EventSeverity.HIGH)
                .description("Test event")
                .simOccurredAt(LocalDateTime.now())
                .resolved(false)
                .build();
    }

}