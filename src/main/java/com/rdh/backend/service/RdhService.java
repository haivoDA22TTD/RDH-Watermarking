package com.rdh.backend.service;

import com.rdh.backend.dto.response.ExtractResponse;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface RdhService {
    List<Map<String, String>> getAvailableAlgorithms();
    byte[] embed(MultipartFile imageFile, String secretData) throws IOException;
    ExtractResponse extractAndRestore(MultipartFile stegoImageFile) throws IOException;
}