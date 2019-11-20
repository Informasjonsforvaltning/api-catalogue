package no.dcat.controller;

import no.dcat.model.ApiCatalog;
import no.dcat.service.ApiCatalogHarvesterService;
import no.dcat.service.ApiCatalogRepository;
import no.fdk.webutils.exceptions.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.*;

@RestController
@RequestMapping(value = "/catalogs/{catalogId}/apicatalog")
public class ApiCatalogController {
    private static final Logger logger = LoggerFactory.getLogger(ApiCatalogController.class);

    private ApiCatalogRepository apiCatalogRepository;

    private ApiCatalogHarvesterService apiCatalogHarvesterService;

    @Autowired
    public ApiCatalogController(
        ApiCatalogRepository apiCatalogRepository,
        ApiCatalogHarvesterService apiCatalogHarvesterService
    ) {
        this.apiCatalogRepository = apiCatalogRepository;
        this.apiCatalogHarvesterService = apiCatalogHarvesterService;
    }

    @PreAuthorize("hasPermission(#catalogId, 'organization', 'read')")
    @RequestMapping(
        value = "",
        method = GET,
        produces = APPLICATION_JSON_UTF8_VALUE)
    public ApiCatalog getApiCatalog(@PathVariable("catalogId") String catalogId) throws NotFoundException {

        Optional<ApiCatalog> apiCatalogOptional = apiCatalogRepository.findByOrgNo(catalogId);

        if (!apiCatalogOptional.isPresent()) {
            throw new NotFoundException("Did not find any Api Catalog for organization number " + catalogId);
        }

        return apiCatalogOptional.get();
    }

    @PreAuthorize("hasPermission(#catalogId, 'organization', 'write')")
    @RequestMapping(
        value = "",
        method = POST,
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_UTF8_VALUE)
    public ApiCatalog createApiCatalog(
        @PathVariable("catalogId") String catalogId,
        @RequestBody ApiCatalog apiCatalogData) {

        ApiCatalog apiCatalog;

        Optional<ApiCatalog> apiCatalogOptional = apiCatalogRepository.findByOrgNo(catalogId);
        if (apiCatalogOptional.isPresent()) {
            apiCatalog = apiCatalogOptional.get();
        } else {
            apiCatalog = new ApiCatalog();
            apiCatalog.setId(UUID.randomUUID().toString());
            apiCatalog.setOrgNo(catalogId);
        }

        if (!Objects.equals(apiCatalog.getHarvestSourceUri(), apiCatalogData.getHarvestSourceUri())) {
            apiCatalog.setHarvestSourceUri(apiCatalogData.getHarvestSourceUri());
        }

        apiCatalog.setHarvestStatus(null);//Always trigger a harvest
        ApiCatalog apiCatalogSaved = apiCatalogRepository.save(apiCatalog);
        apiCatalogHarvesterService.addHarvestSingleCatalogTaskToQueue(apiCatalogSaved);

        return apiCatalogSaved;
    }

    @PreAuthorize("hasPermission(#catalogId, 'organization', 'write')")
    @RequestMapping(
        value = "",
        method = DELETE,
        produces = APPLICATION_JSON_UTF8_VALUE)
    public void deleteApiCatalog(@PathVariable("catalogId") String catalogId) throws NotFoundException {
        ApiCatalog apiCatalog = apiCatalogRepository.findByOrgNo(catalogId).orElseThrow(NotFoundException::new);
        apiCatalogRepository.delete(apiCatalog);
    }
}
