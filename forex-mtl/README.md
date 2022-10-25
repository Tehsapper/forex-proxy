# Overview

The Forex proxy provides currency exchange rates via OneFrame Rates API, bypassing its strict API call limits.

# Running

There is a Docker Compose manifest provided to easily bring up both Forex proxy and local OneFrame service.

1. Ensure that `docker` client is available.
2. Provide OneFrame API token in `docker-compose.yaml`
3. Start the service:
   ```shell
   docker compose up --build
   ```

# Considerations

Given the following:
1) OneFrame API has a limit of 1000 requests per day;
2) OneFrame Rates API allows an arbitrary number of rates to be returned;
3) Returned currency exchange rates should not be more stale than 5 minutes;
4) There are only 9 supported currencies (AUD, CAD, CHF, EUR, GBP, NZD, JPY, SGD, USD) by Get Rates API.

Then the requirements can be fulfilled by querying and caching all currency combination pairs' rates every 5 minutes.

## Feasibility

In this case, there will be `(24 * 60 / 5) = 288` requests made per day by each Forex proxy instance to OneFrame API.
Any Get Rates API request will be served this cached data.

There are `9*(9-1) = 72` currency pair combinations.
Each requested pair is passed to OneFrame as a query parameter in form `[?&]pair=XXXYYY`, taking 12 characters each,
and `72 * 12 = 864` total, which is far less than maximum URL limit of `8000` octets recommended by HTTP specification in RFC 7230.

## Implementation

`scalacache` library was used for caching, with `Caffeine` implementation, an in-memory cache.
Such cache is easier to set up, manage and reason about, compared to an external one (e.g. Redis).

However, this means that exchange rates cache is not shared between proxy instances.
This will be a problem if scalability is required -- only 3 proxy instances can be run in parallel with current implementation,
consuming `864` OneFrame API requests per day.
Also, if too many restarts are triggered, then API call limit might also be depleted.
