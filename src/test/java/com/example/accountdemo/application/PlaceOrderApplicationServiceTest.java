package com.example.accountdemo.application;

import org.junit.jupiter.api.Test;

/**
 * Test PlaceOrderApplicationService — dùng fake repository (HashMap), không cần Spring/DB.
 * Sprint 3: chưa khớp lệnh.
 */
class PlaceOrderApplicationServiceTest {

    @Test
    void placeOrder_shouldSaveOrderIntoOrderBook() {
        // TODO: tự viết
        // - Fake OrderBookRepository có sẵn sổ BTC/VND (giống admin đã mở cặp)
        // - Gọi placeOrder BUY LIMIT
        // - Assert order được save + nằm trong buyOrders của OrderBook
    }

    @Test
    void placeOrder_shouldRejectWhenOrderBookNotOpened() {
        // TODO: tự viết
        // - Fake OrderBookRepository trả null khi findByTradingPair
        // - Assert throw IllegalArgumentException
        // - Không gọi save
    }

    @Test
    void placeOrder_shouldRejectLimitOrderWithoutPrice() {
        // TODO: tự viết
        // - Gọi placeOrder với OrderType.LIMIT và price = null
        // - Assert throw (từ Order constructor)
    }
}
