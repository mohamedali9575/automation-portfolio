package com.automation.hunger;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Project 3a: pure-Java report-logic tests (fast, no browser).
 * Run: mvn test -Dtest=TopRidersCalculatorTest
 */
public class TopRidersCalculatorTest {

    private final TopRidersCalculator calc = new TopRidersCalculator();

    @DataProvider(name = "rankingCases")
    public Object[][] rankingCases() {
        return new Object[][]{
                {"most orders wins",
                        List.of(new TopRidersCalculator.Rider("A", 5, 100.0),
                                new TopRidersCalculator.Rider("B", 9, 10.0)),
                        "B"},
                {"tie broken by cash",
                        List.of(new TopRidersCalculator.Rider("A", 5, 50.0),
                                new TopRidersCalculator.Rider("B", 5, 200.0)),
                        "B"},
        };
    }

    @Test(dataProvider = "rankingCases")
    public void rankingRules(String name, List<TopRidersCalculator.Rider> input, String expectedFirst) {
        List<TopRidersCalculator.Rider> top = calc.topRiders(input, 5);
        Assert.assertEquals(top.get(0).name(), expectedFirst, "Case failed: " + name);
    }

    @Test
    public void limitIsRespected() {
        List<TopRidersCalculator.Rider> input = List.of(
                new TopRidersCalculator.Rider("A", 3, 30.0),
                new TopRidersCalculator.Rider("B", 2, 20.0),
                new TopRidersCalculator.Rider("C", 1, 10.0));
        Assert.assertEquals(calc.topRiders(input, 2).size(), 2);
    }

    @Test
    public void nullInputReturnsEmpty() {
        Assert.assertTrue(calc.topRiders(null, 3).isEmpty());
    }
}
