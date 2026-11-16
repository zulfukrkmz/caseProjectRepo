package com.migros.caseProject.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.migros.caseProject.model.Store;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.List;

@Component
public class StoreLoader {

    private final List<Store> stores;

    public StoreLoader(ObjectMapper objectMapper) {
        try (var inputStream = new ClassPathResource("stores.json").getInputStream()) {
            List<Store> loadedStores = objectMapper.readValue(inputStream, new TypeReference<List<Store>>() {});
            if (loadedStores == null || loadedStores.isEmpty()) {
                throw new IllegalStateException("stores.json must contain at least one store");
            }
            stores = List.copyOf(loadedStores);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load stores.json from the application classpath", e);
        }
    }

    public List<Store> getStores() {
        return stores;
    }
}