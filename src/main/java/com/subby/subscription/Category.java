package com.subby.subscription;

public enum Category {
    OTT("OTT"),
    MUSIC("음악"),
    AI("AI"),
    CLOUD("클라우드·저장소"),
    SOFTWARE("소프트웨어"),
    GAME("게임"),
    NEWS_BOOK("뉴스·도서"),
    SHOPPING("쇼핑 멤버십"),
    ETC("기타");

    private final String label;

    Category(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
