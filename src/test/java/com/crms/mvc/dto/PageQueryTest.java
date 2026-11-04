package com.crms.mvc.dto;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class PageQueryTest {

    @Test
    void keywordIsTrimmedAndBlankMeansNoSearch() {
        PageQueryDTO query = new PageQueryDTO();
        query.setKeyword("   ");
        assertNull(query.getKeyword());
        assertEquals("%", query.getLikePattern(), "no keyword matches everything");

        query.setKeyword("  Vios ");
        assertEquals("Vios", query.getKeyword());
        assertEquals("%vios%", query.getLikePattern());
    }

    @Test
    void likeWildcardsTypedByTheUserSearchForThemselves() {
        PageQueryDTO query = new PageQueryDTO();
        query.setKeyword("50%_off\\");
        assertEquals("%50\\%\\_off\\\\%", query.getLikePattern());
    }

    @Test
    void pageAndSizeFallBackToSafeValues() {
        PageQueryDTO query = new PageQueryDTO();
        query.setPage(-4);
        query.setSize(100000);
        assertEquals(1, query.getPage());
        assertEquals(0, query.getPageIndex());
        assertEquals(PageQueryDTO.DEFAULT_SIZE, query.getSize(), "only the sizes offered in the UI are accepted");

        query.setSize(20);
        assertEquals(20, query.getSize());
        query.setSort(null);
        assertEquals(PageQueryDTO.SORT_NAME_ASC, query.getSort());
    }

    @Test
    void urlKeepsSearchSortAndSizeAndEncodesTheKeyword() {
        AdminVehicleQueryDTO query = new AdminVehicleQueryDTO();
        query.setKeyword("Toyota Vios&x");
        query.setSort("plate_asc");
        query.setSize(20);
        query.setStatus("rented");

        String url = query.urlForPage("/admin/vehicles", 3);

        assertTrue(url.startsWith("/admin/vehicles?"));
        assertTrue(url.contains("keyword=Toyota%20Vios%26x"), url);
        assertTrue(url.contains("sort=plate_asc"));
        assertTrue(url.contains("size=20"));
        assertTrue(url.contains("status=rented"));
        assertTrue(url.contains("page=3"));
    }

    @Test
    void changingTheStatusPillKeepsTheSearchButGoesBackToPageOne() {
        AdminVehicleQueryDTO query = new AdminVehicleQueryDTO();
        query.setKeyword("vios");
        query.setPage(4);

        String url = query.urlForStatus("/admin/vehicles", "available");

        assertTrue(url.contains("keyword=vios"));
        assertTrue(url.contains("status=available"));
        assertTrue(url.contains("page=1"));
    }

    @Test
    void unknownStatusIsTreatedAsAll() {
        AdminVehicleQueryDTO query = new AdminVehicleQueryDTO();
        query.setStatus("'; DROP TABLE vehicles;--");
        assertEquals(AdminVehicleQueryDTO.STATUS_ALL, query.getStatus());
        query.setStatus(" RENTED ");
        assertEquals("rented", query.getStatus());
    }

    @Test
    void navigationShowsFirstLastAndPagesAroundTheCurrentOne() {
        Page<Integer> page = pageOf(30, 10, 1); // page 2 of 3, 0-based number 1
        PageNavDTO nav = PageNavDTO.of(page, number -> "/p?page=" + number);

        assertEquals(2, nav.getCurrent());
        assertEquals(3, nav.getTotalPages());
        assertEquals(11, nav.getFirstItem());
        assertEquals(20, nav.getLastItem());
        assertEquals("/p?page=1", nav.getPreviousUrl());
        assertEquals("/p?page=3", nav.getNextUrl());
        assertEquals(List.of(1, 2, 3), nav.getLinks().stream().map(PageNavDTO.PageLink::number).toList());
        assertTrue(nav.getLinks().get(1).current());
    }

    @Test
    void longListsCollapseTheMiddleIntoGaps() {
        Page<Integer> page = pageOf(200, 10, 9); // page 10 of 20
        PageNavDTO nav = PageNavDTO.of(page, number -> "/p?page=" + number);

        List<Integer> numbers = nav.getLinks().stream().map(PageNavDTO.PageLink::number).toList();
        assertEquals(List.of(1, 0, 8, 9, 10, 11, 12, 0, 20), numbers, "0 stands for a gap");
        assertTrue(nav.getLinks().get(1).isGap());
        assertNull(nav.getLinks().get(1).url());
    }

    @Test
    void firstAndLastPageHaveNoPreviousOrNext() {
        PageNavDTO first = PageNavDTO.of(pageOf(25, 10, 0), number -> "/p?page=" + number);
        assertNull(first.getPreviousUrl());
        assertNotNull(first.getNextUrl());

        PageNavDTO last = PageNavDTO.of(pageOf(25, 10, 2), number -> "/p?page=" + number);
        assertNotNull(last.getPreviousUrl());
        assertNull(last.getNextUrl());
        assertEquals(21, last.getFirstItem());
        assertEquals(25, last.getLastItem());
    }

    @Test
    void emptyResultIsASinglePageWithZeroItems() {
        PageNavDTO nav = PageNavDTO.of(new PageImpl<Integer>(List.of(), PageRequest.of(0, 10), 0), number -> "/p?page=" + number);

        assertTrue(nav.isSinglePage());
        assertEquals(0, nav.getTotalElements());
        assertEquals(0, nav.getFirstItem());
        assertEquals(0, nav.getLastItem());
    }

    private Page<Integer> pageOf(int total, int size, int pageNumber) {
        int from = pageNumber * size;
        List<Integer> content = IntStream.range(from, Math.min(from + size, total)).boxed().toList();
        return new PageImpl<>(content, PageRequest.of(pageNumber, size), total);
    }
}
