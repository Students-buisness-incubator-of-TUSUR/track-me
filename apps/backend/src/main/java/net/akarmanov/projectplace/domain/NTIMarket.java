package net.akarmanov.projectplace.domain;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum NTIMarket {
    AERO_NET("AeroNet"),
    AUTO_NET("AutoNet"),
    ECO_NET("EcoNet"),
    EDUCATION_NET("EduNet"),
    ENERGY_NET("EnergyNet"),
    FOOD_NET("FoodNet"),
    GAME_NET("GameNet"),
    HEALTH_NET("HealthNet"),
    HOME_NET("HomeNet"),
    MARI_NET("MariNet"),
    NEURO_NET("NeuroNet"),
    SAFE_NET("SafeNet"),
    SPORT_NET("SportNet"),
    TECH_NET("TechNet"),
    WEAR_NET("WearNet");

    private static final Map<String, NTIMarket> MARKET_MAP = new HashMap<>();

    static {
        for (NTIMarket market : NTIMarket.values()) {
            MARKET_MAP.put(market.getValue(), market);
        }
    }

    private final String value;

    NTIMarket(String value) {
        this.value = value;
    }

    public static NTIMarket fromValue(String value) {
        return MARKET_MAP.get(value);
    }
}
