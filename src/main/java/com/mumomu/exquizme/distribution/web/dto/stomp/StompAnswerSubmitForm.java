package com.mumomu.exquizme.distribution.web.dto.stomp;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StompAnswerSubmitForm extends StompMessage{
    private int problemIdx; // 문제 번호
    private String answerText; // 문제 정답
    private int totalSubmitAmount;

    public void setTotalSubmitAmount(int totalSubmitAmount) {
        this.totalSubmitAmount = totalSubmitAmount;
    }

    public StompAnswerSubmitForm(MessageType flag, String fromSession, int problemIdx, String answerText) {
        super(flag, fromSession);
        this.problemIdx = problemIdx;
        this.answerText = answerText;
    }

    public StompAnswerSubmitForm(int problemIdx, String answerText) {
        super(null,null);
        this.problemIdx = problemIdx;
        this.answerText = answerText;
    }
}
