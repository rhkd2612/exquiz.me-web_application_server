package com.mumomu.exquizme.distribution.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mumomu.exquizme.distribution.domain.Participant;
import com.mumomu.exquizme.distribution.domain.Room;
import com.mumomu.exquizme.distribution.service.RoomService;
import com.mumomu.exquizme.distribution.web.model.ParticipantForm;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.security.core.parameters.P;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;

import javax.management.ServiceNotFoundException;
import javax.persistence.EntityManager;
import javax.servlet.http.Cookie;

import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class RoomRestControllerTest {
    @Autowired
    private EntityManager em;
    @Autowired
    private RoomService roomService;
    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    ObjectMapper objectMapper;

    private MockMvc mvc;
    private String roomPin;
    private String roomPin2;
    private String invalidPin = "9999999";

    @BeforeEach
    public void setUP(){
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();

        roomPin = roomService.newRoom().getPin();
        roomPin2 = roomService.newRoom().getPin();
    }

    @AfterEach
    public void setDown(){
        roomService.closeRoomByPin(roomPin);
        roomService.closeRoomByPin(roomPin2);
    }


    @Test
    @Transactional
    @DisplayName("새로운퀴즈방생성")
    void newRoomTest() throws Exception{
        mvc.perform(post("/api/room/newRoom"))
                .andExpect(status().isCreated())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$.id",notNullValue()))
                .andExpect(jsonPath("$.pin", notNullValue()));
    }

    @Test
    @Transactional
    @DisplayName("퀴즈방폐쇄")
    void closeRoomTest() throws Exception {
        String myRoomPin = roomService.newRoom().getPin();

        mvc.perform(post("/api/room/{roomPin}/close", myRoomPin))
                .andExpect(status().isAccepted())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$.id",notNullValue()))
                .andExpect(jsonPath("$.currentState").value("FINISH"));
    }

    @Test
    @Transactional
    @DisplayName("존재하지않는퀴즈방폐쇄")
    void closeInvalidRoomTest() throws Exception {
        mvc.perform(post("/api/room/{roomPin}/close", invalidPin))
                .andExpect(status().isBadRequest())
                .andDo(MockMvcResultHandlers.print());
    }

    @Test
    @Transactional
    @DisplayName("쿠키없이퀴즈방참가")
    void joinRoomWithNoCookieTest() throws Exception{
        mvc.perform(get("/api/room/{roomPin}", roomPin))
                .andExpect(status().isMovedPermanently())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$.id",notNullValue()))
                .andExpect(jsonPath("$.pin").value(roomPin));
    }

    @Test
    @Transactional
    @DisplayName("쿠키와함께퀴즈방참가")
    void joinRoomWithCookieTest() throws Exception{
        Room room = roomService.findRoomByPin(roomPin);
        String pUuid = UUID.randomUUID().toString();

        Participant participant = Participant.builder()
                .name("test")
                .nickname("test_nickname")
                .uuid(pUuid)
                .room(room)
                .build();
       roomService.joinParticipant(participant);

        mvc.perform(get("/api/room/{roomPin}", roomPin)
                        .cookie(new Cookie("anonymousCode",pUuid)))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$.id",notNullValue()))
                .andExpect(jsonPath("$.uuid").value(pUuid));
    }

    @Test
    @Transactional
    @DisplayName("다른방의쿠키를소지한채퀴즈방참가")
    void joinRoomWithBeforeRoomCookieTest() throws Exception{
        Room room2 = roomService.findRoomByPin(roomPin2);
        String pUuid = UUID.randomUUID().toString();

        Participant participant = Participant.builder()
                .name("test")
                .nickname("test_nickname")
                .uuid(pUuid)
                .room(room2)
                .build();
        roomService.joinParticipant(participant);

        mvc.perform(get("/api/room/{roomPin}", roomPin)
                        .cookie(new Cookie("anonymousCode",pUuid)))
                .andExpect(status().isMovedPermanently())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$.id",notNullValue()))
                .andExpect(jsonPath("$.pin").value(roomPin));
    }

    @Test
    @Transactional
    @DisplayName("존재하지않는방참가")
    void joinInvalidRoomTest() throws Exception{
        mvc.perform(get("/api/room/{roomPin}", invalidPin))
                .andExpect(status().isBadRequest())
                .andDo(MockMvcResultHandlers.print());
    }

    @Test
    @Transactional
    @DisplayName("새로운참여자추가")
    void signUpParticipantTest() throws Exception{
        ParticipantForm participantForm = new ParticipantForm("test","tester");

        mvc.perform(post("/api/room/{roomPin}/signup", roomPin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(participantForm)))
                .andExpect(status().isCreated())
                .andDo(MockMvcResultHandlers.print());
    }

    @Test
    @Transactional
    @DisplayName("퀴즈방의모든참여자출력")
    void printParticipantsTest() throws Exception{
        mvc.perform(get("/api/room/{roomPin}/participants", roomPin))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print());
    }

    @Test
    @Transactional
    @DisplayName("존재하지않는퀴즈방의참여자출력")
    void printInvalidRoomParticipantsTest() throws Exception{
        mvc.perform(get("/api/room/{roomPin}/participants", invalidPin))
                .andExpect(status().isBadRequest())
                .andDo(MockMvcResultHandlers.print());
    }

    private <T> String toJson(T data) throws JsonProcessingException {
        return objectMapper.writeValueAsString(data);
    }
}