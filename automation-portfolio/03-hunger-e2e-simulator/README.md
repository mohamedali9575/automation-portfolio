# 03 Hunger E2E Simulator — report logic + nested menu UI

Two parts:

1. `TopRidersCalculator` (+ `TopRidersCalculatorTest`) — pure Java ranking logic:
   sort riders by orders desc, tie-break by cash desc. Fast unit tests with DataProvider.
2. `NestedMenuTest` — Playwright test on https://demoqa.com/menu:
   hover/expand "Main Item 2" (like expanding a "Hunger Station" dropdown),
   then assert the nested child appears.

Run all: `mvn test`
Run one: `mvn test -Dtest=TopRidersCalculatorTest`
