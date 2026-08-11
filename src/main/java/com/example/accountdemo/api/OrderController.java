package com.example.accountdemo.api;

import com.example.accountdemo.api.dto.PlaceOrderRequest;
import com.example.accountdemo.api.dto.PlaceOrderResponse;
import com.example.accountdemo.application.PlaceOrderApplicationService;
import com.example.accountdemo.domain.exchange.Order;
import com.example.accountdemo.domain.exchange.OrderSide;
import com.example.accountdemo.domain.exchange.OrderType;
import com.example.accountdemo.domain.exchange.Price;
import com.example.accountdemo.domain.exchange.Quantity;
import com.example.accountdemo.domain.exchange.TradingPair;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API đặt lệnh.
 * Parse string → enum/VO ở đây; application chỉ nhận type đã sạch.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final PlaceOrderApplicationService placeOrderApplicationService;

    public OrderController(PlaceOrderApplicationService placeOrderApplicationService) {
        this.placeOrderApplicationService = placeOrderApplicationService;
    }

    @PostMapping
    public ResponseEntity<PlaceOrderResponse> placeOrder(@RequestBody PlaceOrderRequest request) {
        OrderSide side = OrderSide.valueOf(request.side());
        OrderType orderType = OrderType.valueOf(request.orderType());
        TradingPair tradingPair = new TradingPair(request.baseCurrency(), request.quoteCurrency());
        Quantity quantity = new Quantity(request.quantity());
        Price price = request.price() == null ? null : new Price(request.price());

        Order order = placeOrderApplicationService.placeOrder(
                request.accountId(),
                side,
                orderType,
                tradingPair,
                quantity,
                price
        );

        return ResponseEntity.ok(new PlaceOrderResponse(
                order.getOrderId(),
                order.getStatus().name()
        ));
    }
}
