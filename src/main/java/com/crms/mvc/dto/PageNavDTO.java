package com.crms.mvc.dto;

import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

/**
 * Everything the pagination bar needs, with the links already built, so templates only loop and print.
 * Shows the first and last page, the pages around the current one, and "…" for the gaps.
 */
public class PageNavDTO {
    private static final int PAGES_AROUND_CURRENT = 2;

    private final int current;
    private final int totalPages;
    private final long totalElements;
    private final long firstItem;
    private final long lastItem;
    private final String previousUrl;
    private final String nextUrl;
    private final List<PageLink> links;

    /** A numbered link, or a gap ("…") when number is 0. */
    public record PageLink(int number, String url, boolean current) {
        public boolean isGap() { return number == 0; }
    }

    private PageNavDTO(int current, int totalPages, long totalElements, long firstItem, long lastItem,
                       String previousUrl, String nextUrl, List<PageLink> links) {
        this.current = current;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.firstItem = firstItem;
        this.lastItem = lastItem;
        this.previousUrl = previousUrl;
        this.nextUrl = nextUrl;
        this.links = links;
    }

    /** @param urlOfPage builds the URL of a 1-based page number, keeping the current search and sort */
    public static PageNavDTO of(Page<?> page, IntFunction<String> urlOfPage) {
        int current = page.getNumber() + 1;
        int total = Math.max(page.getTotalPages(), 1);

        List<PageLink> links = new ArrayList<>();
        int previousShown = 0;
        for (int number = 1; number <= total; number++) {
            boolean shown = number == 1 || number == total || Math.abs(number - current) <= PAGES_AROUND_CURRENT;
            if (!shown) {
                continue;
            }
            if (previousShown != 0 && number - previousShown > 1) {
                links.add(new PageLink(0, null, false));
            }
            links.add(new PageLink(number, urlOfPage.apply(number), number == current));
            previousShown = number;
        }

        long first = page.getTotalElements() == 0 ? 0 : (long) page.getNumber() * page.getSize() + 1;
        long last = (long) page.getNumber() * page.getSize() + page.getNumberOfElements();
        return new PageNavDTO(current, total, page.getTotalElements(), first, last,
                current > 1 ? urlOfPage.apply(current - 1) : null,
                current < total ? urlOfPage.apply(current + 1) : null,
                links);
    }

    public int getCurrent() { return current; }
    public int getTotalPages() { return totalPages; }
    public long getTotalElements() { return totalElements; }
    public long getFirstItem() { return firstItem; }
    public long getLastItem() { return lastItem; }
    public String getPreviousUrl() { return previousUrl; }
    public String getNextUrl() { return nextUrl; }
    public List<PageLink> getLinks() { return links; }
    public boolean isSinglePage() { return totalPages <= 1; }
}
