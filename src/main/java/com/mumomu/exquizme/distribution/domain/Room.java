package com.mumomu.exquizme.distribution.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mumomu.exquizme.production.domain.Problemset;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.*;
import javax.servlet.http.Cookie;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Entity @Getter @Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room {
    @Id @GeneratedValue
    @Column(name = "room_id")
    private Long id;

    private String pin;
    private int maxParticipantCount; // 최대 참여자 수
    private Date startDate;
    private Date endDate;

    @Enumerated(EnumType.STRING)
    private RoomState currentState; // READY, PLAY, FINISH
    private int currentProblemNum; // 최근 진행중인 문제번호

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="problemset_id")
    private Problemset problemset;

    @JsonIgnore
    @OneToMany(mappedBy = "room", fetch = FetchType.LAZY)
    private List<Participant> participants;

    public static Cookie setAnonymousCookie(){
        UUID uuid = UUID.randomUUID();
        Cookie anonymousCookie = new Cookie("anonymousCode", uuid.toString());
        anonymousCookie.setComment("사용자 구분 코드");
        anonymousCookie.setMaxAge(60 * 60 * 3); // 쿠키 지속 시간
        return anonymousCookie;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public void setCurrentState(RoomState currentState) {
        this.currentState = currentState;
    }
}
