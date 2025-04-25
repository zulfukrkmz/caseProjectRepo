package com.migros.caseProject.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.migros.caseProject.model.Store;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Component
public class StoreLoader {

    private static final String STORE_FILE_PATH = "src/main/resources/stores.json"; // stores.json dosyasının yolu

    public List<Store> loadStores() {
        try {
            // JSON dosyasını okuma işlemi
            ObjectMapper objectMapper = new ObjectMapper();
            // Store sınıfındaki listeyi döndürüyoruz
            return objectMapper.readValue(new File(STORE_FILE_PATH), objectMapper.getTypeFactory().constructCollectionType(List.class, Store.class));
        } catch (IOException e) {
            e.printStackTrace();
            return null; // Hata durumunda null döndürebiliriz, ancak bu durumda hata yönetimini daha detaylı yapmak iyi olacaktır
        }
    }
}