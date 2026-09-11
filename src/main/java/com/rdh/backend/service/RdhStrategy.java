package com.rdh.backend.service;

import com.rdh.backend.dto.enumtype.RdhAlgorithmType;
import com.rdh.backend.dto.response.ExtractResponse;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface RdhStrategy {
    RdhAlgorithmType getAlgorithmType();
    byte[] embed(MultipartFile imageFile, String secretData) throws IOException;
    ExtractResponse extractAndRestore(MultipartFile stegoImageFile) throws IOException;
}