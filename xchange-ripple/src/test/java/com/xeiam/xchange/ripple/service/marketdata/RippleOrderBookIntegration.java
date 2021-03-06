package com.xeiam.xchange.ripple.service.marketdata;

import static org.fest.assertions.api.Assertions.assertThat;

import org.junit.Test;

import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.ExchangeFactory;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.marketdata.OrderBook;
import com.xeiam.xchange.dto.trade.LimitOrder;
import com.xeiam.xchange.ripple.RippleExchange;
import com.xeiam.xchange.ripple.dto.RippleException;
import com.xeiam.xchange.ripple.dto.trade.RippleLimitOrder;
import com.xeiam.xchange.ripple.service.polling.params.RippleMarketDataParams;
import com.xeiam.xchange.service.polling.marketdata.PollingMarketDataService;

public class RippleOrderBookIntegration {

  @Test
  public void getOrderBookTest() throws Exception {
    final int depthLimit = 15;

    final Exchange exchange = ExchangeFactory.INSTANCE.createExchange(RippleExchange.class.getName());
    final PollingMarketDataService marketDataService = exchange.getPollingMarketDataService();

    final RippleMarketDataParams params = new RippleMarketDataParams();

    // rvYAfWj5gh67oV6fW32ZzP3Aw4Eubs59B is Bitstamp's account
    params.setAddress("rvYAfWj5gh67oV6fW32ZzP3Aw4Eubs59B");

    // Base symbol is XRP, this is the native currency so does not need counterparty
    // params.setBaseCounterparty("");

    // Counter symbol is BTC, this requires a counterparty
    params.setCounterCounterparty("rvYAfWj5gh67oV6fW32ZzP3Aw4Eubs59B");

    // Set number of orders on each bid/ask side to return
    params.setLimit(depthLimit);

    final OrderBook orderBook = marketDataService.getOrderBook(CurrencyPair.XRP_BTC, params);
    assertThat(orderBook).isNotNull();
    assertThat(orderBook.getBids()).hasSize(depthLimit);
    for (final LimitOrder order : orderBook.getBids()) {
      assertThat(order).isInstanceOf(RippleLimitOrder.class);
      assertThat(((RippleLimitOrder) order).getCounterCounterparty()).isEqualTo("rvYAfWj5gh67oV6fW32ZzP3Aw4Eubs59B");
    }
    assertThat(orderBook.getAsks()).hasSize(depthLimit);
    for (final LimitOrder order : orderBook.getAsks()) {
      assertThat(order).isInstanceOf(RippleLimitOrder.class);
      assertThat(((RippleLimitOrder) order).getCounterCounterparty()).isEqualTo("rvYAfWj5gh67oV6fW32ZzP3Aw4Eubs59B");
    }
    System.out.println(orderBook);
  }

  @Test(expected = RippleException.class)
  public void invalidOrderBookTest() throws Exception {
    final int depthLimit = 15;

    final Exchange exchange = ExchangeFactory.INSTANCE.createExchange(RippleExchange.class.getName());
    final PollingMarketDataService marketDataService = exchange.getPollingMarketDataService();

    final RippleMarketDataParams params = new RippleMarketDataParams();

    // rvYAfWj5gh67oV6fW32ZzP3Aw4Eubs59B is Bitstamp's account
    params.setAddress("rvYAfWj5gh67oV6fW32ZzP3Aw4Eubs59B");

    params.setBaseCounterparty("invalid_address"); // This is an invalid address

    // Set number of orders on each bid/ask side to return
    params.setLimit(depthLimit);

    try {
      marketDataService.getOrderBook(CurrencyPair.BTC_XRP, params);
    } catch (final RippleException e) {
      assertThat(e.getError()).containsIgnoringCase("restINVALID_PARAMETER");
      assertThat(e.getErrorType()).containsIgnoringCase("invalid_request");
      assertThat(e.getMessage()).containsIgnoringCase("Invalid parameter: base. Must be a currency string in the form currency+counterparty");
      throw e;
    }

  }
}
