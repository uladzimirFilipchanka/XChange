package org.knowm.xchange.cexio.service;

import org.knowm.xchange.Exchange;
import org.knowm.xchange.cexio.CexIOAdapters;
import org.knowm.xchange.cexio.dto.trade.CexIOArchivedOrder;
import org.knowm.xchange.cexio.dto.trade.CexIOOpenOrder;
import org.knowm.xchange.cexio.dto.trade.CexIOOrder;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.marketdata.Trades;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.knowm.xchange.dto.trade.OpenOrders;
import org.knowm.xchange.dto.trade.UserTrade;
import org.knowm.xchange.dto.trade.UserTrades;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.exceptions.NotAvailableFromExchangeException;
import org.knowm.xchange.exceptions.NotYetImplementedForExchangeException;
import org.knowm.xchange.service.trade.TradeService;
import org.knowm.xchange.service.trade.params.CancelOrderByIdParams;
import org.knowm.xchange.service.trade.params.CancelOrderParams;
import org.knowm.xchange.service.trade.params.TradeHistoryParams;
import org.knowm.xchange.service.trade.params.orders.OpenOrdersParamCurrencyPair;
import org.knowm.xchange.service.trade.params.orders.OpenOrdersParams;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Author: brox Since: 2/6/14
 */

public class CexIOTradeService extends CexIOTradeServiceRaw implements TradeService {

    /**
     * Constructor
     */
    public CexIOTradeService(Exchange exchange) {

        super(exchange);
    }

    @Override
    public OpenOrders getOpenOrders() throws IOException {
        return getOpenOrders(createOpenOrdersParams());
    }

    @Override
    public OpenOrders getOpenOrders(
        OpenOrdersParams params) throws ExchangeException, NotAvailableFromExchangeException,
        NotYetImplementedForExchangeException, IOException {

        List<CexIOOrder> cexIOOrderList;
        if (params instanceof OpenOrdersParamCurrencyPair) {
            cexIOOrderList = getCexIOOpenOrders(((OpenOrdersParamCurrencyPair) params).getCurrencyPair());
        } else {
            cexIOOrderList = getCexIOOpenOrders();
        }

        return CexIOAdapters.adaptOpenOrders(cexIOOrderList);
    }

    @Override
    public String placeMarketOrder(MarketOrder marketOrder) throws IOException {

        CexIOOrder order = placeCexIOMarketOrder(marketOrder);

        return Long.toString(order.getId());
    }

    @Override
    public String placeLimitOrder(LimitOrder limitOrder) throws IOException {

        CexIOOrder order = placeCexIOLimitOrder(limitOrder);

        return Long.toString(order.getId());
    }

    @Override
    public boolean cancelOrder(String orderId) throws IOException {

        return cancelCexIOOrder(orderId);
    }

    @Override
    public boolean cancelOrder(
        CancelOrderParams orderParams) throws ExchangeException, NotAvailableFromExchangeException,
        NotYetImplementedForExchangeException, IOException {
        if (orderParams instanceof CancelOrderByIdParams) {
            cancelOrder(((CancelOrderByIdParams) orderParams).orderId);
        }
        return false;
    }

    @Override
    public UserTrades getTradeHistory(TradeHistoryParams params) throws IOException {
        List<UserTrade> trades = new ArrayList<>();
        for (CexIOArchivedOrder cexIOArchivedOrder : archivedOrders(params)) {
            if (cexIOArchivedOrder.status.equals(
                "c"))//"d" — done (fully executed), "c" — canceled (not executed), "cd" — cancel-done (partially
            // executed)
            {
                continue;
            }
            BigDecimal priceToUse = cexIOArchivedOrder.price;
            if (priceToUse == null) {
                priceToUse = findOutPriceByTransaction(cexIOArchivedOrder.orderId);
            }
            trades.add(CexIOAdapters.adaptArchivedOrder(cexIOArchivedOrder, priceToUse));
        }
        return new UserTrades(trades, Trades.TradeSortType.SortByTimestamp);
    }

    @Override
    public TradeHistoryParams createTradeHistoryParams() {
        throw new NotAvailableFromExchangeException();
    }

    @Override
    public OpenOrdersParams createOpenOrdersParams() {
        return null;
        // TODO: return new DefaultOpenOrdersParamCurrencyPair();
    }

    @Override
    public Collection<Order> getOrder(
        String... orderIds) throws ExchangeException, NotAvailableFromExchangeException,
        NotYetImplementedForExchangeException, IOException {

        List<Order> orders = new ArrayList<>();
        for (String orderId : orderIds) {
            CexIOOpenOrder cexIOOrder = getOrderDetail(orderId);

            BigDecimal priceToUse = cexIOOrder.price == null ? null : new BigDecimal(cexIOOrder.price);
            if (priceToUse == null) {
                priceToUse = findOutPriceByTransaction(cexIOOrder.orderId);
            }
            orders.add(CexIOAdapters.adaptOrder(cexIOOrder, priceToUse));
        }
        return orders;
    }

    private BigDecimal findOutPriceByTransaction(String orderId) throws IOException {
        Map<String, List<Map<String, Object>>> data = (Map<String, List<Map<String, Object>>>) this
            .getOrderTransactions(orderId).get("data");
        if (data == null || data.isEmpty() || data.get("vtx") == null){
            return null;
        }
        return new BigDecimal(data.get("vtx")
            .stream()
            .flatMap(map -> map.entrySet().stream())
            .filter(Objects::nonNull)
            .filter(o -> o.getValue() != null)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (o, o2) -> o))
            .get("price")
            .toString());

    }
}
