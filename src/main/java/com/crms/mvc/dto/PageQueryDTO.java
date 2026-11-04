package com.crms.mvc.dto;

import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * What an admin list page asks for: a search keyword, an alphabetical sort and one page of results.
 * Bound straight from the query string (?keyword=&sort=&page=&size=) so every list URL can be bookmarked or shared.
 */
public class PageQueryDTO {
    public static final int DEFAULT_SIZE = 10;
    public static final List<Integer> PAGE_SIZES = List.of(10, 20, 50);
    public static final String SORT_NAME_ASC = "name_asc";
    public static final String SORT_NAME_DESC = "name_desc";

    private String keyword;
    private String sort = SORT_NAME_ASC;
    private int page = 1;
    private int size = DEFAULT_SIZE;

    /** Trimmed keyword, or null when the box is empty. */
    public String getKeyword() {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    public void setKeyword(String keyword) { this.keyword = keyword; }

    public String getSort() { return sort; }

    public void setSort(String sort) {
        this.sort = sort == null || sort.isBlank() ? SORT_NAME_ASC : sort.trim();
    }

    /** 1-based page number as shown to the user. */
    public int getPage() { return page; }

    public void setPage(int page) { this.page = Math.max(1, page); }

    public int getSize() { return size; }

    /** Only the page sizes offered in the UI are accepted; anything else falls back to the default. */
    public void setSize(int size) { this.size = PAGE_SIZES.contains(size) ? size : DEFAULT_SIZE; }

    /** 0-based index for Spring Data. */
    public int getPageIndex() { return page - 1; }

    public List<Integer> getPageSizes() { return PAGE_SIZES; }

    /**
     * Keyword as a lower-case "contains" pattern for LIKE ... ESCAPE '\'; "%" (match everything) when empty.
     * %, _ and \ typed by the user are escaped so they search for the character itself.
     */
    public String getLikePattern() {
        String word = getKeyword();
        if (word == null) {
            return "%";
        }
        String escaped = word.toLowerCase()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    /** URL of this same list (same search and sort) at the given page. */
    public String urlForPage(String path, int pageNumber) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(path);
        addFilterParams(builder);
        builder.queryParam("page", pageNumber);
        return builder.build().encode().toUriString();
    }

    /** Extra filters of a concrete list (for example status) add themselves here. */
    protected void addFilterParams(UriComponentsBuilder builder) {
        if (getKeyword() != null) {
            builder.queryParam("keyword", getKeyword());
        }
        builder.queryParam("sort", sort);
        builder.queryParam("size", size);
    }
}
