package com.peccio.space_colony_simulator.domain.model;

import java.math.BigDecimal;

/**
 * Each type declares:
 *   - which resource it boosts
 *   - how much production it adds per tick
 *   - how many MATERIALS it costs to build
 *
 */
public enum BuildingType {

    LIFE_SUPPORT(ResourceType.OXYGEN,    BigDecimal.valueOf(5),  BigDecimal.valueOf(50)),
    FARM        (ResourceType.FOOD,      BigDecimal.valueOf(8),  BigDecimal.valueOf(40)),
    REACTOR     (ResourceType.ENERGY,    BigDecimal.valueOf(10), BigDecimal.valueOf(60)),
    MINE        (ResourceType.MATERIALS, BigDecimal.valueOf(6),  BigDecimal.valueOf(45));

    private final ResourceType affectedResource;
    private final BigDecimal   productionBonus;    // added to production_rate per tick
    private final BigDecimal   constructionCost;   // deducted from MATERIALS on build

    BuildingType(ResourceType affectedResource,
                 BigDecimal productionBonus,
                 BigDecimal constructionCost) {
        this.affectedResource  = affectedResource;
        this.productionBonus   = productionBonus;
        this.constructionCost  = constructionCost;
    }

    public ResourceType getAffectedResource() { return affectedResource; }
    public BigDecimal   getProductionBonus()  { return productionBonus;  }
    public BigDecimal   getConstructionCost() { return constructionCost; }
}
