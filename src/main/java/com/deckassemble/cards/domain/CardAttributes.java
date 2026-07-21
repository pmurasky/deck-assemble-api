package com.deckassemble.cards.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

/**
 * Shared card attribute columns; subclasses keep identical column definitions, schema unchanged.
 */
@MappedSuperclass
public abstract class CardAttributes {

    @Column(name = "mana_cost", length = 255)
    private String manaCost;

    @Column(name = "type_line", length = 255)
    private String typeLine;

    @Column(name = "oracle_text", columnDefinition = "text")
    private String oracleText;

    @Column(name = "power", length = 10)
    private String power;

    @Column(name = "toughness", length = 10)
    private String toughness;

    @Column(name = "loyalty", length = 10)
    private String loyalty;

    // ponytail: comma-separated WUBRG for now; switch to native array if queries need it
    @Column(name = "colors", length = 50)
    private String colors;

    public String getManaCost() {
        return manaCost;
    }

    public void setManaCost(String manaCost) {
        this.manaCost = manaCost;
    }

    public String getTypeLine() {
        return typeLine;
    }

    public void setTypeLine(String typeLine) {
        this.typeLine = typeLine;
    }

    public String getOracleText() {
        return oracleText;
    }

    public void setOracleText(String oracleText) {
        this.oracleText = oracleText;
    }

    public String getPower() {
        return power;
    }

    public void setPower(String power) {
        this.power = power;
    }

    public String getToughness() {
        return toughness;
    }

    public void setToughness(String toughness) {
        this.toughness = toughness;
    }

    public String getLoyalty() {
        return loyalty;
    }

    public void setLoyalty(String loyalty) {
        this.loyalty = loyalty;
    }

    public String getColors() {
        return colors;
    }

    public void setColors(String colors) {
        this.colors = colors;
    }
}
