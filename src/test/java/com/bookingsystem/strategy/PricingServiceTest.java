package com.bookingsystem.strategy;

import com.bookingsystem.entity.Hotel;
import com.bookingsystem.entity.Inventory;
import com.bookingsystem.entity.Room;
import com.bookingsystem.strategy.impl.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PricingServiceTest {

    private PricingService pricingService;

    @BeforeEach
    void setUp() {
        pricingService = new PricingService();
    }

    private Inventory createInventory(BigDecimal basePrice, BigDecimal surgeFactor,
                                       int bookedCount, int totalCount, LocalDate date) {
        Room room = Room.builder().basePrice(basePrice).build();
        return Inventory.builder()
                .room(room)
                .surgeFactor(surgeFactor)
                .bookedCount(bookedCount)
                .totalCount(totalCount)
                .date(date)
                .build();
    }

    @Nested
    class CalculateDynamicPricing {

        @Test
        void shouldReturnBasePriceForNormalConditions() {
            // No surge, low occupancy, date far in future
            Inventory inventory = createInventory(
                    new BigDecimal("1000"), BigDecimal.ONE, 2, 100, LocalDate.now().plusDays(30));

            BigDecimal price = pricingService.calculateDynamicPricing(inventory);

            // Base price * surge(1.0) = 1000, no occupancy/urgency/holiday modifiers
            assertThat(price).isEqualByComparingTo(new BigDecimal("1000"));
        }

        @Test
        void shouldApplySurgeFactor() {
            Inventory inventory = createInventory(
                    new BigDecimal("1000"), new BigDecimal("1.5"), 2, 100, LocalDate.now().plusDays(30));

            BigDecimal price = pricingService.calculateDynamicPricing(inventory);

            // 1000 * 1.5 = 1500
            assertThat(price).isEqualByComparingTo(new BigDecimal("1500"));
        }

        @Test
        void shouldApplyOccupancyMarkupWhenAbove80Percent() {
            // 85 out of 100 = 85% occupancy
            Inventory inventory = createInventory(
                    new BigDecimal("1000"), BigDecimal.ONE, 85, 100, LocalDate.now().plusDays(30));

            BigDecimal price = pricingService.calculateDynamicPricing(inventory);

            // Base 1000 * surge 1.0 * occupancy 1.2 = 1200
            assertThat(price).isEqualByComparingTo(new BigDecimal("1200"));
        }

        @Test
        void shouldNotApplyOccupancyMarkupWhenAtOrBelow80Percent() {
            // 80 out of 100 = exactly 80%
            Inventory inventory = createInventory(
                    new BigDecimal("1000"), BigDecimal.ONE, 80, 100, LocalDate.now().plusDays(30));

            BigDecimal price = pricingService.calculateDynamicPricing(inventory);

            // No occupancy markup
            assertThat(price).isEqualByComparingTo(new BigDecimal("1000"));
        }

        @Test
        void shouldApplyUrgencyMarkupForBookingWithin7Days() {
            Inventory inventory = createInventory(
                    new BigDecimal("1000"), BigDecimal.ONE, 2, 100, LocalDate.now().plusDays(3));

            BigDecimal price = pricingService.calculateDynamicPricing(inventory);

            // 1000 * urgency 1.15 = 1150
            assertThat(price).isEqualByComparingTo(new BigDecimal("1150"));
        }

        @Test
        void shouldNotApplyUrgencyMarkupForBookingBeyond7Days() {
            Inventory inventory = createInventory(
                    new BigDecimal("1000"), BigDecimal.ONE, 2, 100, LocalDate.now().plusDays(10));

            BigDecimal price = pricingService.calculateDynamicPricing(inventory);

            assertThat(price).isEqualByComparingTo(new BigDecimal("1000"));
        }

        @Test
        void shouldStackSurgeAndOccupancyAndUrgency() {
            // Surge 2.0, 90% occupancy, within 3 days
            Inventory inventory = createInventory(
                    new BigDecimal("1000"), new BigDecimal("2.0"), 90, 100, LocalDate.now().plusDays(3));

            BigDecimal price = pricingService.calculateDynamicPricing(inventory);

            // 1000 * surge 2.0 * occupancy 1.2 * urgency 1.15 = 2760
            assertThat(price).isEqualByComparingTo(new BigDecimal("2760.0"));
        }
    }

    @Nested
    class CalculateTotalPrice {

        @Test
        void shouldSumPricesAcrossMultipleDays() {
            Inventory inv1 = createInventory(
                    new BigDecimal("1000"), BigDecimal.ONE, 2, 100, LocalDate.now().plusDays(30));
            Inventory inv2 = createInventory(
                    new BigDecimal("1000"), new BigDecimal("1.5"), 2, 100, LocalDate.now().plusDays(31));

            BigDecimal total = pricingService.calculateTotalPrice(List.of(inv1, inv2));

            // Day 1: 1000, Day 2: 1500
            assertThat(total).isEqualByComparingTo(new BigDecimal("2500"));
        }

        @Test
        void shouldReturnZeroForEmptyList() {
            BigDecimal total = pricingService.calculateTotalPrice(List.of());
            assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    class IndividualStrategies {

        @Test
        void basePricingStrategyShouldReturnRoomBasePrice() {
            BasePricingStrategy strategy = new BasePricingStrategy();
            Inventory inv = createInventory(new BigDecimal("3000"), BigDecimal.ONE, 0, 10, LocalDate.now());

            assertThat(strategy.calculatePrice(inv)).isEqualByComparingTo(new BigDecimal("3000"));
        }

        @Test
        void surgePricingStrategyShouldMultiplyByFactor() {
            BasePricingStrategy base = new BasePricingStrategy();
            SurgePricingStrategy surge = new SurgePricingStrategy(base);
            Inventory inv = createInventory(new BigDecimal("1000"), new BigDecimal("3.0"), 0, 10, LocalDate.now());

            assertThat(surge.calculatePrice(inv)).isEqualByComparingTo(new BigDecimal("3000"));
        }

        @Test
        void occupancyStrategyShouldMarkupHighOccupancy() {
            BasePricingStrategy base = new BasePricingStrategy();
            OccupancyPricingStrategy occ = new OccupancyPricingStrategy(base);
            Inventory inv = createInventory(new BigDecimal("1000"), BigDecimal.ONE, 9, 10, LocalDate.now());

            // 90% occupancy → 1000 * 1.2 = 1200
            assertThat(occ.calculatePrice(inv)).isEqualByComparingTo(new BigDecimal("1200"));
        }

        @Test
        void occupancyStrategyShouldNotMarkupLowOccupancy() {
            BasePricingStrategy base = new BasePricingStrategy();
            OccupancyPricingStrategy occ = new OccupancyPricingStrategy(base);
            Inventory inv = createInventory(new BigDecimal("1000"), BigDecimal.ONE, 5, 10, LocalDate.now());

            // 50% occupancy → no markup
            assertThat(occ.calculatePrice(inv)).isEqualByComparingTo(new BigDecimal("1000"));
        }

        @Test
        void urgencyStrategyShouldMarkupNearDates() {
            BasePricingStrategy base = new BasePricingStrategy();
            UrgencyPricingStrategy urg = new UrgencyPricingStrategy(base);
            Inventory inv = createInventory(new BigDecimal("1000"), BigDecimal.ONE, 0, 10, LocalDate.now());

            // Today → within 7 days → 1000 * 1.15 = 1150
            assertThat(urg.calculatePrice(inv)).isEqualByComparingTo(new BigDecimal("1150"));
        }

        @Test
        void urgencyStrategyShouldNotMarkupDistantDates() {
            BasePricingStrategy base = new BasePricingStrategy();
            UrgencyPricingStrategy urg = new UrgencyPricingStrategy(base);
            Inventory inv = createInventory(new BigDecimal("1000"), BigDecimal.ONE, 0, 10, LocalDate.now().plusDays(14));

            assertThat(urg.calculatePrice(inv)).isEqualByComparingTo(new BigDecimal("1000"));
        }

        @Test
        void holidayStrategyPassesThroughWhenNotHoliday() {
            BasePricingStrategy base = new BasePricingStrategy();
            HolidayPricingStrategy holiday = new HolidayPricingStrategy(base);
            Inventory inv = createInventory(new BigDecimal("1000"), BigDecimal.ONE, 0, 10, LocalDate.now());

            // Holiday check is TODO (always false), so no markup
            assertThat(holiday.calculatePrice(inv)).isEqualByComparingTo(new BigDecimal("1000"));
        }
    }
}
