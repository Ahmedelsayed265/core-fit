package com.corefit.service;

import com.corefit.dto.request.PlaygroundRequest;
import com.corefit.dto.response.GeneralResponse;
import com.corefit.entity.Playground;
import com.corefit.entity.User;
import com.corefit.enums.UserType;
import com.corefit.exceptions.GeneralException;
import com.corefit.repository.PlaygroundRepo;
import com.corefit.utils.DateParser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlaygroundService {
    @Autowired
    private PlaygroundRepo playgroundRepo;
    @Autowired
    private AuthService authService;
    @Autowired
    private FilesService filesService;

    public GeneralResponse<?> createPlayground(PlaygroundRequest playgroundRequest
            , List<MultipartFile> images
            , HttpServletRequest httpRequest) {

        User user = authService.extractUserFromRequest(httpRequest);
        if (user.getType() != UserType.PROVIDER) {
            throw new GeneralException("User is not a provider");
        }

        List<String> imageUrls = uploadImages(images);

        Playground playground = Playground.builder()
                .name(playgroundRequest.getName())
                .description(playgroundRequest.getDescription())
                .lat(playgroundRequest.getLat())
                .lng(playgroundRequest.getLng())
                .address(playgroundRequest.getAddress())
                .teamMembers(playgroundRequest.getTeamMembers())
                .morningShiftStart(DateParser.parseTime(playgroundRequest.getMorningShiftStart()))
                .morningShiftEnd(DateParser.parseTime(playgroundRequest.getMorningShiftEnd()))
                .nightShiftStart(DateParser.parseTime(playgroundRequest.getNightShiftStart()))
                .nightShiftEnd(DateParser.parseTime(playgroundRequest.getNightShiftEnd()))
                .bookingPrice(playgroundRequest.getBookingPrice())
                .extraNightPrice(playgroundRequest.getExtraNightPrice())
                .hasExtraPrice(playgroundRequest.isHasExtraPrice())
                .images(imageUrls)
                .user(user)
                .build();

        playgroundRepo.save(playground);
        return new GeneralResponse<>("Playground added successfully", playground);
    }


    private List<String> uploadImages(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            return new ArrayList<>();
        }
        return images.stream().map(image -> {
            try {
                return filesService.saveImage(image);
            } catch (IOException e) {
                throw new GeneralException("Failed to upload image: " + e.getMessage());
            }
        }).collect(Collectors.toList());
    }
}
