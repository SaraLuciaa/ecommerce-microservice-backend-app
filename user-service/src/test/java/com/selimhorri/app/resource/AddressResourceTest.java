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
import com.selimhorri.app.dto.AddressDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.service.AddressService;

@WebMvcTest(AddressResource.class)
class AddressResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AddressService addressService;

    private AddressDto addressDto;

    @BeforeEach
    void setUp() {
        this.addressDto = AddressDto.builder()
                .addressId(1)
                .fullAddress("123 Main St")
                .postalCode("7600")
                .city("Cali")
                .userDto(UserDto.builder()
                        .userId(10)
                        .firstName("John")
                        .lastName("Doe")
                        .imageUrl("img.jpg")
                        .email("john@example.com")
                        .phone("1112223333")
                        .build())
                .build();
    }

    @Test
    void findAllShouldReturnCollectionResponse() throws Exception {
        when(this.addressService.findAll()).thenReturn(List.of(this.addressDto));

        this.mockMvc.perform(get("/api/address"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection[0].addressId", equalTo(this.addressDto.getAddressId())));

        verify(this.addressService).findAll();
    }

    @Test
    void findByIdShouldReturnDto() throws Exception {
        when(this.addressService.findById(1)).thenReturn(this.addressDto);

        this.mockMvc.perform(get("/api/address/{addressId}", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city", equalTo(this.addressDto.getCity())));

        verify(this.addressService).findById(1);
    }

    @Test
    void saveShouldDelegateToService() throws Exception {
        when(this.addressService.save(any(AddressDto.class))).thenReturn(this.addressDto);

        this.mockMvc.perform(post("/api/address")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(this.addressDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postalCode", equalTo(this.addressDto.getPostalCode())));

        ArgumentCaptor<AddressDto> captor = ArgumentCaptor.forClass(AddressDto.class);
        verify(this.addressService).save(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getFullAddress()).isEqualTo(this.addressDto.getFullAddress());
    }

    @Test
    void updateShouldDelegateToService() throws Exception {
        when(this.addressService.update(any(AddressDto.class))).thenReturn(this.addressDto);

        this.mockMvc.perform(put("/api/address")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(this.addressDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressId", equalTo(this.addressDto.getAddressId())));

        verify(this.addressService).update(any(AddressDto.class));
    }

    @Test
    void updateWithIdShouldPassParsedIdentifier() throws Exception {
        when(this.addressService.update(eq(1), any(AddressDto.class))).thenReturn(this.addressDto);

        this.mockMvc.perform(put("/api/address/{addressId}", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(this.addressDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullAddress", equalTo(this.addressDto.getFullAddress())));

        ArgumentCaptor<AddressDto> captor = ArgumentCaptor.forClass(AddressDto.class);
        verify(this.addressService).update(eq(1), captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getFullAddress()).isEqualTo(this.addressDto.getFullAddress());
    }

    @Test
    void deleteShouldReturnTrue() throws Exception {
        this.mockMvc.perform(delete("/api/address/{addressId}", "7"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(this.addressService).deleteById(7);
    }
}
