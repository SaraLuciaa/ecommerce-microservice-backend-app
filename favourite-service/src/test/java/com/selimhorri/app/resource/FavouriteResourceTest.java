package com.selimhorri.app.resource;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.id.FavouriteId;
import com.selimhorri.app.dto.FavouriteDto;
import com.selimhorri.app.service.FavouriteService;

@WebMvcTest(FavouriteResource.class)
class FavouriteResourceTest {

    private static final LocalDateTime LIKE_DATE = LocalDateTime.of(2024, 3, 15, 18, 30, 45, 123_000);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(AppConstant.LOCAL_DATE_TIME_FORMAT);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FavouriteService favouriteService;

    private FavouriteDto favouriteDto;

    @BeforeEach
    void setUp() {
        this.favouriteDto = FavouriteDto.builder()
                .userId(7)
                .productId(8)
                .likeDate(LIKE_DATE)
                .build();
    }

    @Test
    void findAllShouldReturnFavourites() throws Exception {
        when(this.favouriteService.findAll()).thenReturn(List.of(this.favouriteDto));

        this.mockMvc.perform(get("/api/favourites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[0].userId", equalTo(this.favouriteDto.getUserId())))
                .andExpect(jsonPath("$.collection[0].productId", equalTo(this.favouriteDto.getProductId())));

        verify(this.favouriteService).findAll();
    }

    @Test
    void findByCompositePathShouldReturnFavourite() throws Exception {
        FavouriteId favouriteId = new FavouriteId(7, 8, LIKE_DATE);
        when(this.favouriteService.findById(eq(favouriteId))).thenReturn(this.favouriteDto);

        this.mockMvc.perform(get("/api/favourites/{userId}/{productId}/{likeDate}",
                        Integer.toString(this.favouriteDto.getUserId()),
                        Integer.toString(this.favouriteDto.getProductId()),
                        LIKE_DATE.format(FORMATTER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", equalTo(this.favouriteDto.getUserId())));

        verify(this.favouriteService).findById(favouriteId);
    }

    @Test
    void findByBodyShouldReturnFavourite() throws Exception {
        FavouriteId favouriteId = new FavouriteId(3, 4, LIKE_DATE);
        when(this.favouriteService.findById(favouriteId)).thenReturn(this.favouriteDto);

        this.mockMvc.perform(get("/api/favourites/find")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(favouriteId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", equalTo(this.favouriteDto.getUserId())));

        verify(this.favouriteService).findById(favouriteId);
    }

    @Test
    void saveShouldDelegateToService() throws Exception {
        when(this.favouriteService.save(any(FavouriteDto.class))).thenReturn(this.favouriteDto);

        this.mockMvc.perform(post("/api/favourites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(this.favouriteDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", equalTo(this.favouriteDto.getUserId())));

        ArgumentCaptor<FavouriteDto> captor = ArgumentCaptor.forClass(FavouriteDto.class);
        verify(this.favouriteService).save(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getProductId()).isEqualTo(this.favouriteDto.getProductId());
    }

    @Test
    void updateShouldDelegateToService() throws Exception {
        when(this.favouriteService.update(any(FavouriteDto.class))).thenReturn(this.favouriteDto);

        this.mockMvc.perform(put("/api/favourites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(this.favouriteDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId", equalTo(this.favouriteDto.getProductId())));

        ArgumentCaptor<FavouriteDto> captor = ArgumentCaptor.forClass(FavouriteDto.class);
        verify(this.favouriteService).update(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getLikeDate()).isEqualTo(this.favouriteDto.getLikeDate());
    }

    @Test
    void deleteByCompositePathShouldDelegate() throws Exception {
    FavouriteId favouriteId = new FavouriteId(7, 8, LIKE_DATE);

    this.mockMvc.perform(delete("/api/favourites/{userId}/{productId}/{likeDate}",
                        Integer.toString(this.favouriteDto.getUserId()),
                        Integer.toString(this.favouriteDto.getProductId()),
                        LIKE_DATE.format(FORMATTER)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

    verify(this.favouriteService).deleteById(favouriteId);
    }

    @Test
    void deleteByBodyShouldDelegate() throws Exception {
        FavouriteId favouriteId = new FavouriteId(9, 10, LIKE_DATE);

        this.mockMvc.perform(delete("/api/favourites/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(favouriteId)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(this.favouriteService).deleteById(favouriteId);
    }
}
