package emu.nebula.server.error;

import lombok.Getter;

// TODO Add rest of the error codes

@Getter
public enum ErrorCode {
    INVALID_MESSAGE_TOKEN               (100101),
    INVALID_MESSAGE_TIME                (100102),
    INVALID_MESSAGE_COMMAND_NUMBER      (100103),
    SERVER_ERROR                        (110105),
    BUILD_NOT_EXIST                     (111103),
    SCORE_BOSS_NOT_AVAILABLE            (112801),
    GEM_NOT_EXIST                       (111609),
    CONFIG_ERROR                        (119902),
    INSUFFICIENT_RESOURCES              (119903);
    
    private int value;
    
    private ErrorCode(int value) {
        this.value = value;
    }
}
