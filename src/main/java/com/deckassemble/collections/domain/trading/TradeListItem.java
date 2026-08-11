package com.deckassemble.collections.domain.trading;

import com.deckassemble.collections.domain.physical.CardCondition;
import com.deckassemble.collections.domain.physical.PhysicalFinish;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "trade_list_items")
public class TradeListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trade_list_id", nullable = false)
    private Long tradeListId;

    @Column(name = "card_printing_id", nullable = false)
    private Long cardPrintingId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition", length = 30)
    private @Nullable CardCondition condition;

    @Enumerated(EnumType.STRING)
    @Column(name = "finish", length = 20)
    private @Nullable PhysicalFinish finish;

    @Column(name = "language", length = 10)
    private @Nullable String language;

    protected TradeListItem() {}

    // checkstyle:ParameterNumber suppressed: JPA row constructor mirrors the table columns.
    @SuppressWarnings({"checkstyle:ParameterNumber", "PMD.ExcessiveParameterList"})
    public TradeListItem(
            Long tradeListId,
            Long cardPrintingId,
            int quantity,
            @Nullable CardCondition condition,
            @Nullable PhysicalFinish finish,
            @Nullable String language) {
        this.tradeListId = tradeListId;
        this.cardPrintingId = cardPrintingId;
        this.quantity = quantity;
        this.condition = condition;
        this.finish = finish;
        this.language = language;
    }

    public Long getId() {
        return id;
    }

    public Long getTradeListId() {
        return tradeListId;
    }

    public Long getCardPrintingId() {
        return cardPrintingId;
    }

    public int getQuantity() {
        return quantity;
    }

    public @Nullable CardCondition getCondition() {
        return condition;
    }

    public @Nullable PhysicalFinish getFinish() {
        return finish;
    }

    public @Nullable String getLanguage() {
        return language;
    }
}
