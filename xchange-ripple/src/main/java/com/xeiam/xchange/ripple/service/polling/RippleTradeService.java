package com.xeiam.xchange.ripple.service.polling;

import java.io.IOException;
import java.util.List;

import com.xeiam.xchange.dto.trade.LimitOrder;
import com.xeiam.xchange.dto.trade.MarketOrder;
import com.xeiam.xchange.dto.trade.OpenOrders;
import com.xeiam.xchange.dto.trade.UserTrades;
import com.xeiam.xchange.exceptions.ExchangeException;
import com.xeiam.xchange.exceptions.NotAvailableFromExchangeException;
import com.xeiam.xchange.exceptions.NotYetImplementedForExchangeException;
import com.xeiam.xchange.ripple.RippleAdapters;
import com.xeiam.xchange.ripple.RippleExchange;
import com.xeiam.xchange.ripple.dto.trade.IRippleTradeTransaction;
import com.xeiam.xchange.ripple.dto.trade.RippleLimitOrder;
import com.xeiam.xchange.ripple.service.polling.params.RippleTradeHistoryAccount;
import com.xeiam.xchange.ripple.service.polling.params.RippleTradeHistoryCount;
import com.xeiam.xchange.ripple.service.polling.params.RippleTradeHistoryHashLimit;
import com.xeiam.xchange.ripple.service.polling.params.RippleTradeHistoryParams;
import com.xeiam.xchange.service.polling.trade.PollingTradeService;
import com.xeiam.xchange.service.polling.trade.params.TradeHistoryParamPaging;
import com.xeiam.xchange.service.polling.trade.params.TradeHistoryParams;
import com.xeiam.xchange.service.polling.trade.params.TradeHistoryParamsTimeSpan;

public class RippleTradeService extends RippleTradeServiceRaw implements PollingTradeService {

  private final RippleExchange ripple;

  /**
   * Empty placeholder trade history parameter object.
   */
  private final RippleTradeHistoryParams defaultTradeHistoryParams = createTradeHistoryParams();

  public RippleTradeService(final RippleExchange exchange) {
    super(exchange);
    ripple = exchange;
  }

  /**
   * The additional data map of an order will be populated with {@link RippleExchange.DATA_BASE_COUNTERPARTY} if the base currency is not XRP,
   * similarly if the counter currency is not XRP then {@link RippleExchange.DATA_COUNTER_COUNTERPARTY} will be populated.
   */
  @Override
  public OpenOrders getOpenOrders() throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {
    return RippleAdapters.adaptOpenOrders(getOpenAccountOrders(), ripple.getRoundingScale());
  }

  @Override
  public String placeMarketOrder(final MarketOrder order)
      throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {
    throw new NotYetImplementedForExchangeException();
  }

  /**
   * @param order this should be a RippleLimitOrder object with the base and counter counterparties populated for any currency other than XRP.
   */
  @Override
  public String placeLimitOrder(final LimitOrder order)
      throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {
    if (order instanceof RippleLimitOrder) {
      return placeOrder((RippleLimitOrder) order, ripple.validateOrderRequests());
    } else {
      throw new IllegalArgumentException("order must be of type: " + RippleLimitOrder.class.getName());
    }
  }

  @Override
  public boolean cancelOrder(final String orderId)
      throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {
    return cancelOrder(orderId, ripple.validateOrderRequests());
  }

  /**
   * Search through the last {@link RippleTradeHistoryParams#DEFAULT_PAGE_LENGTH} notifications looking for a maximum of
   * {@link RippleTradeHistoryCount#DEFAULT_TRADE_COUNT_LIMIT} trades. If an account enters many orders and receives few executions then it is likely
   * that this query will return no trades. See {@link #getTradeHistory(TradeHistoryParams)} for details of how to structure the query to fit your use
   * case.
   *
   * @param arguments these are ignored.
   */

  @Override
  public UserTrades getTradeHistory(final Object... arguments)
      throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {
    return getTradeHistory(defaultTradeHistoryParams);
  }

  /**
   * Ripple trade history is a request intensive process. The REST API does not provide a simple single trade history query. Trades are retrieved by
   * querying account notifications and for those of type order details of the hash are then queried. These order detail queries could be order entry,
   * cancel or execution, it is not possible to tell from the notification. Therefore if an account is entering many orders but executing few of them,
   * this trade history query will result in many API calls without returning any trade history. In order to reduce the time and resources used in
   * these repeated calls In order to reduce the number of API calls a number of different methods can be used:
   * <ul>
   * <li><b>RippleTradeHistoryHashLimit</b> set the {@link RippleTradeHistoryHashLimit#setHashLimit(String)} to the last known trade, this query will
   * then terminate once it has been found.</li>
   * <li><b>RippleTradeHistoryCount</b> set the {@link RippleTradeHistoryCount#setTradeCountLimit(int)} to restrict the number of trades to return,
   * the default is {@link RippleTradeHistoryCount#DEFAULT_TRADE_COUNT_LIMIT}.</li>
   * <li><b>RippleTradeHistoryCount</b> set the {@link RippleTradeHistoryCount#setApiCallCountLimit(int)} to restrict the number of API calls that
   * will be made during a single trade history query, the default is {@link RippleTradeHistoryCount#DEFAULT_API_CALL_COUNT}.</li>
   * <li><b>TradeHistoryParamsTimeSpan</b> set the {@link TradeHistoryParamsTimeSpan#setStartTime(java.util.Date)} to limit the number of trades
   * searched for to those done since the given start time.</li> TradeHistoryParamsTimeSpan
   * </ul>
   *
   * @param params Can optionally implement {@RippleTradeHistoryAccount}, {@RippleTradeHistoryCount}, {@RippleTradeHistoryHashLimit},
   *        {@RippleTradeHistoryPreferredCurrencies}, {@link TradeHistoryParamPaging}, {@TradeHistoryParamCurrencyPair},
   *        {@link TradeHistoryParamsTimeSpan}. All other TradeHistoryParams types will be ignored.
   */
  @Override
  public UserTrades getTradeHistory(final TradeHistoryParams params) throws IOException {
    if (params instanceof RippleTradeHistoryCount) {
      final RippleTradeHistoryCount rippleParams = (RippleTradeHistoryCount) params;
      rippleParams.resetApiCallCount();
      rippleParams.resetTradeCount();
    }

    final String account;
    if (params instanceof RippleTradeHistoryAccount) {
      final RippleTradeHistoryAccount rippleAccount = (RippleTradeHistoryAccount) params;
      if (rippleAccount.getAccount() != null) {
        account = rippleAccount.getAccount();
      } else {
        account = exchange.getExchangeSpecification().getApiKey();
      }
    } else {
      account = defaultTradeHistoryParams.getAccount();
    }

    final List<IRippleTradeTransaction> trades = getTradesForAccount(params, account);
    return RippleAdapters.adaptTrades(trades, params, (RippleAccountService) exchange.getPollingAccountService(), ripple.getRoundingScale());
  }

  @Override
  public RippleTradeHistoryParams createTradeHistoryParams() {
    final RippleTradeHistoryParams params = new RippleTradeHistoryParams();
    params.setAccount(exchange.getExchangeSpecification().getApiKey());
    return params;
  }
}
