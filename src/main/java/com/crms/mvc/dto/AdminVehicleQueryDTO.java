package com.crms.mvc.dto;

import org.springframework.web.util.UriComponentsBuilder;

import java.util.Set;

/** Query of the admin car list: the common search/sort/page plus the status pill. */
public class AdminVehicleQueryDTO extends PageQueryDTO {
    public static final String STATUS_ALL = "all";
    public static final String SORT_PLATE_ASC = "plate_asc";
    private static final Set<String> STATUSES = Set.of(STATUS_ALL, "available", "rented", "maintenance");

    private String status = STATUS_ALL;

    public String getStatus() { return status; }

    public void setStatus(String status) {
        String normalized = status == null ? STATUS_ALL : status.trim().toLowerCase();
        this.status = STATUSES.contains(normalized) ? normalized : STATUS_ALL;
    }

    /** Same search and sort, different status pill; always starts again from the first page. */
    public String urlForStatus(String path, String newStatus) {
        AdminVehicleQueryDTO copy = new AdminVehicleQueryDTO();
        copy.setKeyword(getKeyword());
        copy.setSort(getSort());
        copy.setSize(getSize());
        copy.setStatus(newStatus);
        return copy.urlForPage(path, 1);
    }

    @Override
    protected void addFilterParams(UriComponentsBuilder builder) {
        super.addFilterParams(builder);
        builder.queryParam("status", status);
    }
}
