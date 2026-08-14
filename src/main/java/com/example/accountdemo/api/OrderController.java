package com.example.accountdemo.api;

import com.example.accountdemo.api.dto.OrderResponse;
import com.example.accountdemo.api.dto.PlaceOrderRequest;
import com.example.accountdemo.api.dto.PlaceOrderResponse;
import com.example.accountdemo.application.CancelOrderApplicationService;
import com.example.accountdemo.application.GetOrderApplicationService;
import com.example.accountdemo.application.PlaceOrderApplicationService;
import com.example.accountdemo.domain.exchange.order.model.Order;
import com.example.accountdemo.domain.exchange.order.model.OrderSide;
import com.example.accountdemo.domain.exchange.order.model.OrderType;
import com.example.accountdemo.domain.exchange.shared.Price;
import com.example.accountdemo.domain.exchange.shared.Quantity;
import com.example.accountdemo.domain.exchange.shared.TradingPair;
import com.example.accountdemo.infrastructure.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

/**
 * REST API lệnh giao dịch: đặt, xem, hủy.
 *
 * <p><b>Vì sao cần class này:</b> biên giới HTTP cho exchange — map DTO sang value object domain,
 * rồi ủy thác Place/Get/Cancel application service (matching/settlement không nằm ở đây).
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final PlaceOrderApplicationService placeOrderApplicationService;
    private final CancelOrderApplicationService cancelOrderApplicationService;
    private final GetOrderApplicationService getOrderApplicationService;

    /** Đặt lệnh BUY/SELL LIMIT (hoặc SELL MARKET). */
    @PostMapping
    public ResponseEntity<PlaceOrderResponse> placeOrder(@RequestBody PlaceOrderRequest request) {
        String username = SecurityUtils.currentUsername();
        OrderSide side = OrderSide.valueOf(request.side());
        OrderType orderType = OrderType.valueOf(request.orderType());
        TradingPair tradingPair = new TradingPair(request.baseCurrency(), request.quoteCurrency());
        Quantity quantity = new Quantity(request.quantity());
        Price price = request.price() == null ? null : new Price(request.price());

        Order order = placeOrderApplicationService.placeOrder(
                username,
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

    /** Xem chi tiết một lệnh theo orderId. */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        Order order = getOrderApplicationService.get(SecurityUtils.currentUsername(), orderId);
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    /** Hủy lệnh còn mở; phần lock chưa khớp được release. */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable String orderId) {
        Order order = cancelOrderApplicationService.cancel(SecurityUtils.currentUsername(), orderId);
        return ResponseEntity.ok(OrderResponse.from(order));
    }
}
