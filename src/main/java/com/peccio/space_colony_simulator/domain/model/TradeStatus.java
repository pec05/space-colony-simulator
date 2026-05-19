package com.peccio.space_colony_simulator.domain.model;

public enum TradeStatus {
    ENDING,    // in transit — resources already deducted from sender
    ARRIVED,    // delivered to receiver
    CANCELLED,  // sender cancelled — resources returned
    FAILED      // receiver destroyed before arrival — resources lost
}
