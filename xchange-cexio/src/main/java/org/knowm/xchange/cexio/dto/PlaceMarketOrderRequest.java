package org.knowm.xchange.cexio.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.knowm.xchange.cexio.dto.trade.CexIOOrder;

import java.math.BigDecimal;

public class PlaceMarketOrderRequest extends CexIORequest {
  public final CexIOOrder.Type type;
  @JsonProperty("order_type")
  public final String orderType = "market";
  public final BigDecimal amount;

  public PlaceMarketOrderRequest(CexIOOrder.Type type, BigDecimal amount) {
    this.type = type;
    this.amount = amount;
  }
}
